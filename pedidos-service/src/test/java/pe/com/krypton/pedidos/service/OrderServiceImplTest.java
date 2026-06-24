package pe.com.krypton.pedidos.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pe.com.krypton.pedidos.client.CatalogoClient;
import pe.com.krypton.pedidos.client.MonolitoStockClient;
import pe.com.krypton.pedidos.client.ProductDTO;
import pe.com.krypton.pedidos.dto.request.CheckoutRequest;
import pe.com.krypton.pedidos.dto.request.CheckoutRequest.CheckoutItem;
import pe.com.krypton.pedidos.dto.request.PaymentRequest;
import pe.com.krypton.pedidos.dto.request.StockSaleRequest;
import pe.com.krypton.pedidos.dto.response.OrderResponse;
import pe.com.krypton.pedidos.exception.CatalogoUnavailableException;
import pe.com.krypton.pedidos.exception.InsufficientStockException;
import pe.com.krypton.pedidos.exception.OrderStatusTransitionException;
import pe.com.krypton.pedidos.exception.ResourceNotFoundException;
import pe.com.krypton.pedidos.model.Order;
import pe.com.krypton.pedidos.model.OrderItem;
import pe.com.krypton.pedidos.model.enums.DocumentType;
import pe.com.krypton.pedidos.model.enums.OrderStatus;
import pe.com.krypton.pedidos.policy.OrderStatusPolicy;
import pe.com.krypton.pedidos.repository.OrderRepository;
import pe.com.krypton.pedidos.service.impl.OrderServiceImpl;

/**
 * Unit test for OrderServiceImpl. CatalogoClient (Feign), OrderRepository y
 * SequenceGenerator van mockeados. Verifica la lógica de checkout (repricing +
 * envío + IGV + snapshot), la degradación del catálogo y la transición de pago.
 */
@ExtendWith(MockitoExtension.class)
class OrderServiceImplTest {

    @Mock OrderRepository orderRepository;
    @Mock CatalogoClient catalogoClient;
    @Mock MonolitoStockClient monolitoStockClient;
    @Mock SequenceGenerator sequenceGenerator;
    @Mock OrderStatusPolicy orderStatusPolicy;

    OrderServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new OrderServiceImpl(
                orderRepository, catalogoClient, monolitoStockClient, sequenceGenerator, orderStatusPolicy);
    }

    // ─── helpers ────────────────────────────────────────────────────────────────

    private ProductDTO product(Long id, String name, BigDecimal price, int stock) {
        return new ProductDTO(id, name, price, stock);
    }

    private CheckoutRequest checkout(List<CheckoutItem> items) {
        return new CheckoutRequest(items, DocumentType.BOLETA, "Juan Cliente", "12345678");
    }

    private Order order(Long id, Long userId, OrderStatus status) {
        Order o = new Order();
        o.setId(id);
        o.setUserId(userId);
        o.setStatus(status);
        o.setOrderDate(Instant.now());
        o.setDocumentType(DocumentType.BOLETA);
        o.setCustomerName("Juan Cliente");
        o.setCustomerDoc("12345678");
        o.setSubtotal(new BigDecimal("100.00"));
        o.setShippingCost(new BigDecimal("20.00"));
        o.setIgv(new BigDecimal("18.31"));
        o.setTotal(new BigDecimal("120.00"));
        o.setItems(List.of(new OrderItem(10L, "Mouse", 1, new BigDecimal("100.00"))));
        return o;
    }

    // ─── CHECKOUT GROUP ─────────────────────────────────────────────────────────

    @Test
    void checkout_happy_path_snapshots_catalog_price_and_computes_free_shipping_and_igv() {
        Long userId = 3L;
        // El cliente NO manda precio; el catálogo dice 299.90. subtotal = 2×299.90 = 599.80 ≥ 300 → envío gratis.
        CheckoutRequest req = checkout(List.of(new CheckoutItem(10L, 2)));
        when(catalogoClient.getById(10L))
                .thenReturn(product(10L, "Notebook", new BigDecimal("299.90"), 5));
        when(sequenceGenerator.nextId(anyString())).thenReturn(1L);
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

        OrderResponse result = service.checkout(userId, req);

        ArgumentCaptor<Order> captor = ArgumentCaptor.forClass(Order.class);
        verify(orderRepository).save(captor.capture());
        Order saved = captor.getValue();

        assertThat(saved.getId()).isEqualTo(1L);
        assertThat(saved.getUserId()).isEqualTo(3L);
        assertThat(saved.getStatus()).isEqualTo(OrderStatus.PENDIENTE);
        assertThat(saved.getSubtotal()).isEqualByComparingTo(new BigDecimal("599.80"));
        assertThat(saved.getShippingCost()).isEqualByComparingTo(BigDecimal.ZERO); // ≥ 300 → gratis
        assertThat(saved.getTotal()).isEqualByComparingTo(new BigDecimal("599.80"));
        // IGV desglosado: 599.80/1.18 = 508.31 base → igv = 91.49
        assertThat(saved.getIgv()).isEqualByComparingTo(new BigDecimal("91.49"));

        // Snapshot: el precio congelado viene del catálogo (299.90), no del cliente.
        assertThat(saved.getItems()).hasSize(1);
        OrderItem line = saved.getItems().get(0);
        assertThat(line.getUnitPrice()).isEqualByComparingTo(new BigDecimal("299.90"));
        assertThat(line.getProductName()).isEqualTo("Notebook");
        assertThat(line.getQuantity()).isEqualTo(2);

        // El response refleja el documento guardado.
        assertThat(result.total()).isEqualByComparingTo(new BigDecimal("599.80"));
        assertThat(result.items().get(0).id()).isNull(); // embebido: sin id propio
        assertThat(result.items().get(0).subtotal()).isEqualByComparingTo(new BigDecimal("599.80"));

        // Notifica la venta al monolito (master del stock) usando la orden como referencia.
        ArgumentCaptor<StockSaleRequest> saleCaptor = ArgumentCaptor.forClass(StockSaleRequest.class);
        verify(monolitoStockClient).registerSale(saleCaptor.capture());
        StockSaleRequest sale = saleCaptor.getValue();
        assertThat(sale.reference()).isEqualTo("1"); // saved.getId() == 1
        assertThat(sale.items()).hasSize(1);
        assertThat(sale.items().get(0).productId()).isEqualTo(10L);
        assertThat(sale.items().get(0).quantity()).isEqualTo(2);
    }

    @Test
    void checkout_charges_20_shipping_when_subtotal_below_300() {
        CheckoutRequest req = checkout(List.of(new CheckoutItem(10L, 1)));
        when(catalogoClient.getById(10L))
                .thenReturn(product(10L, "Mouse", new BigDecimal("100.00"), 5)); // subtotal 100 < 300
        when(sequenceGenerator.nextId(anyString())).thenReturn(2L);
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

        service.checkout(3L, req);

        ArgumentCaptor<Order> captor = ArgumentCaptor.forClass(Order.class);
        verify(orderRepository).save(captor.capture());
        Order o = captor.getValue();
        assertThat(o.getSubtotal()).isEqualByComparingTo(new BigDecimal("100.00"));
        assertThat(o.getShippingCost()).isEqualByComparingTo(new BigDecimal("20.00"));
        assertThat(o.getTotal()).isEqualByComparingTo(new BigDecimal("120.00")); // 100 + 20
        // IGV: 120/1.18 = 101.69 base → igv = 18.31
        assertThat(o.getIgv()).isEqualByComparingTo(new BigDecimal("18.31"));
    }

    @Test
    void checkout_free_shipping_when_subtotal_reaches_exactly_300() {
        CheckoutRequest req = checkout(List.of(new CheckoutItem(10L, 2)));
        when(catalogoClient.getById(10L))
                .thenReturn(product(10L, "Teclado", new BigDecimal("150.00"), 5)); // 2×150 = 300 exacto
        when(sequenceGenerator.nextId(anyString())).thenReturn(3L);
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

        service.checkout(3L, req);

        ArgumentCaptor<Order> captor = ArgumentCaptor.forClass(Order.class);
        verify(orderRepository).save(captor.capture());
        Order o = captor.getValue();
        assertThat(o.getShippingCost()).isEqualByComparingTo(BigDecimal.ZERO); // umbral inclusivo
        assertThat(o.getTotal()).isEqualByComparingTo(new BigDecimal("300.00"));
        // IGV: 300/1.18 = 254.24 base → igv = 45.76
        assertThat(o.getIgv()).isEqualByComparingTo(new BigDecimal("45.76"));
    }

    @Test
    void checkout_when_catalog_returns_null_throws_CatalogoUnavailable_and_nothing_saved() {
        CheckoutRequest req = checkout(List.of(new CheckoutItem(10L, 1)));
        when(catalogoClient.getById(10L)).thenReturn(null); // fallback del circuit-breaker

        assertThatThrownBy(() -> service.checkout(3L, req))
                .isInstanceOf(CatalogoUnavailableException.class);

        verify(orderRepository, never()).save(any());
        verify(sequenceGenerator, never()).nextId(anyString());
        verify(monolitoStockClient, never()).registerSale(any()); // no hubo venta → no se descuenta
    }

    @Test
    void checkout_when_requested_quantity_exceeds_stock_throws_InsufficientStock_and_nothing_saved() {
        CheckoutRequest req = checkout(List.of(new CheckoutItem(10L, 5)));
        when(catalogoClient.getById(10L))
                .thenReturn(product(10L, "Notebook", new BigDecimal("299.90"), 1)); // sólo 1 en stock

        assertThatThrownBy(() -> service.checkout(3L, req))
                .isInstanceOf(InsufficientStockException.class);

        verify(orderRepository, never()).save(any());
        verify(monolitoStockClient, never()).registerSale(any()); // no hubo venta → no se descuenta
    }

    // ─── READ GROUP ──────────────────────────────────────────────────────────────

    @Test
    void getMyOrders_returns_user_orders_mapped() {
        Order o1 = order(1L, 3L, OrderStatus.PENDIENTE);
        Order o2 = order(2L, 3L, OrderStatus.CONFIRMADA);
        when(orderRepository.findByUserIdOrderByOrderDateDesc(3L)).thenReturn(List.of(o2, o1));

        List<OrderResponse> result = service.getMyOrders(3L);

        assertThat(result).hasSize(2);
        assertThat(result.get(0).id()).isEqualTo(2L); // tal como vino del repo (más reciente primero)
        assertThat(result.get(1).id()).isEqualTo(1L);
    }

    @Test
    void getMyOrder_returns_order_when_owner_matches() {
        Order o = order(5L, 3L, OrderStatus.PENDIENTE);
        when(orderRepository.findByIdAndUserId(5L, 3L)).thenReturn(Optional.of(o));

        OrderResponse result = service.getMyOrder(3L, 5L);

        assertThat(result.id()).isEqualTo(5L);
        assertThat(result.userId()).isEqualTo(3L);
    }

    @Test
    void getMyOrder_IDOR_or_missing_throws_ResourceNotFound() {
        when(orderRepository.findByIdAndUserId(9L, 3L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getMyOrder(3L, 9L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ─── PAY GROUP ───────────────────────────────────────────────────────────────

    @Test
    void pay_happy_path_transitions_pendiente_to_confirmada() {
        Order o = order(3L, 3L, OrderStatus.PENDIENTE);
        when(orderRepository.findByIdAndUserId(3L, 3L)).thenReturn(Optional.of(o));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

        OrderResponse result = service.pay(3L, 3L, new PaymentRequest("YAPE"));

        assertThat(o.getStatus()).isEqualTo(OrderStatus.CONFIRMADA);
        assertThat(result.status()).isEqualTo("CONFIRMADA");
        verify(orderRepository).save(o);
    }

    @Test
    void pay_already_confirmada_throws_OrderStatusTransition_and_nothing_saved() {
        Order o = order(4L, 3L, OrderStatus.CONFIRMADA);
        when(orderRepository.findByIdAndUserId(4L, 3L)).thenReturn(Optional.of(o));

        assertThatThrownBy(() -> service.pay(3L, 4L, new PaymentRequest("YAPE")))
                .isInstanceOf(OrderStatusTransitionException.class);

        verify(orderRepository, never()).save(any());
    }

    @Test
    void pay_cancelada_throws_OrderStatusTransition() {
        Order o = order(7L, 3L, OrderStatus.CANCELADA);
        when(orderRepository.findByIdAndUserId(7L, 3L)).thenReturn(Optional.of(o));

        assertThatThrownBy(() -> service.pay(3L, 7L, new PaymentRequest("EFECTIVO")))
                .isInstanceOf(OrderStatusTransitionException.class);
    }

    @Test
    void pay_IDOR_or_missing_throws_ResourceNotFound() {
        when(orderRepository.findByIdAndUserId(8L, 3L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.pay(3L, 8L, new PaymentRequest("CREDIT_CARD")))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
