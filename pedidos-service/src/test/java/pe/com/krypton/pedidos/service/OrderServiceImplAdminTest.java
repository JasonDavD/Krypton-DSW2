package pe.com.krypton.pedidos.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Sort;
import pe.com.krypton.pedidos.client.CatalogoClient;
import pe.com.krypton.pedidos.client.MonolitoStockClient;
import pe.com.krypton.pedidos.dto.request.StockSaleRequest;
import pe.com.krypton.pedidos.dto.response.OrderResponse;
import pe.com.krypton.pedidos.dto.response.PageResponse;
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
 * Unit test de los casos de uso ADMIN de {@link OrderServiceImpl}. OrderRepository y
 * OrderStatusPolicy van mockeados. Cubre filtros (status / rango de fecha), paginación en
 * memoria, 404, y la transición de estado válida e ilegal.
 */
@ExtendWith(MockitoExtension.class)
class OrderServiceImplAdminTest {

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

    // ─── helpers ─────────────────────────────────────────────────────────────────

    private Order order(Long id, OrderStatus status, Instant orderDate) {
        Order o = new Order();
        o.setId(id);
        o.setUserId(1L);
        o.setStatus(status);
        o.setOrderDate(orderDate);
        o.setDocumentType(DocumentType.BOLETA);
        o.setCustomerName("Cliente " + id);
        o.setCustomerDoc("12345678");
        o.setSubtotal(new BigDecimal("100.00"));
        o.setShippingCost(new BigDecimal("20.00"));
        o.setIgv(new BigDecimal("18.31"));
        o.setTotal(new BigDecimal("120.00"));
        o.setItems(List.of(new OrderItem(10L, "Mouse", 1, new BigDecimal("100.00"))));
        return o;
    }

    private final Instant t0 = Instant.parse("2026-01-01T00:00:00Z");

    private Instant at(int daysOffset) {
        return t0.plus(daysOffset, ChronoUnit.DAYS);
    }

    // ─── listAllForAdmin: sin filtros ────────────────────────────────────────────

    @Test
    void should_return_all_orders_when_no_filters_applied() {
        List<Order> all = List.of(
                order(3L, OrderStatus.PENDIENTE, at(3)),
                order(2L, OrderStatus.CONFIRMADA, at(2)),
                order(1L, OrderStatus.ENVIADO, at(1)));
        when(orderRepository.findAll(any(Sort.class))).thenReturn(all);

        PageResponse<OrderResponse> result = service.listAllForAdmin(null, null, null, 0, 20);

        assertThat(result.content()).hasSize(3);
        assertThat(result.totalElements()).isEqualTo(3L);
        assertThat(result.totalPages()).isEqualTo(1);
        assertThat(result.page()).isEqualTo(0);
        assertThat(result.size()).isEqualTo(20);
        // El orden viene del repo (DESC por orderDate); el service no reordena.
        assertThat(result.content().get(0).id()).isEqualTo(3L);
    }

    @Test
    void should_sort_by_orderDate_desc_when_querying_repository() {
        when(orderRepository.findAll(any(Sort.class))).thenReturn(List.of());

        service.listAllForAdmin(null, null, null, 0, 20);

        ArgumentCaptor<Sort> captor = ArgumentCaptor.forClass(Sort.class);
        verify(orderRepository).findAll(captor.capture());
        Sort.Order sortOrder = captor.getValue().getOrderFor("orderDate");
        assertThat(sortOrder).isNotNull();
        assertThat(sortOrder.getDirection()).isEqualTo(Sort.Direction.DESC);
    }

    // ─── listAllForAdmin: filtro por status ──────────────────────────────────────

    @Test
    void should_filter_by_status_when_status_provided() {
        List<Order> all = List.of(
                order(3L, OrderStatus.PENDIENTE, at(3)),
                order(2L, OrderStatus.CONFIRMADA, at(2)),
                order(1L, OrderStatus.PENDIENTE, at(1)));
        when(orderRepository.findAll(any(Sort.class))).thenReturn(all);

        PageResponse<OrderResponse> result =
                service.listAllForAdmin(OrderStatus.PENDIENTE, null, null, 0, 20);

        assertThat(result.content()).hasSize(2);
        assertThat(result.totalElements()).isEqualTo(2L);
        assertThat(result.content()).allMatch(o -> o.status().equals("PENDIENTE"));
    }

    // ─── listAllForAdmin: filtro por rango de fecha [from, to) ───────────────────

    @Test
    void should_filter_by_date_range_half_open_when_from_and_to_provided() {
        List<Order> all = List.of(
                order(1L, OrderStatus.PENDIENTE, at(1)),   // dentro
                order(2L, OrderStatus.PENDIENTE, at(2)),   // dentro
                order(3L, OrderStatus.PENDIENTE, at(3)),   // == to → EXCLUIDO (half-open)
                order(0L, OrderStatus.PENDIENTE, at(0)));  // < from → excluido
        when(orderRepository.findAll(any(Sort.class))).thenReturn(all);

        // Rango [at(1), at(3)): incluye at(1) y at(2), excluye at(3) y at(0).
        PageResponse<OrderResponse> result =
                service.listAllForAdmin(null, at(1), at(3), 0, 20);

        assertThat(result.totalElements()).isEqualTo(2L);
        assertThat(result.content()).extracting(OrderResponse::id)
                .containsExactlyInAnyOrder(1L, 2L);
    }

    @Test
    void should_apply_only_lower_bound_when_to_is_null() {
        List<Order> all = List.of(
                order(0L, OrderStatus.PENDIENTE, at(0)),   // < from → excluido
                order(1L, OrderStatus.PENDIENTE, at(1)),   // >= from → incluido
                order(5L, OrderStatus.PENDIENTE, at(5)));  // >= from → incluido
        when(orderRepository.findAll(any(Sort.class))).thenReturn(all);

        PageResponse<OrderResponse> result =
                service.listAllForAdmin(null, at(1), null, 0, 20);

        assertThat(result.totalElements()).isEqualTo(2L);
        assertThat(result.content()).extracting(OrderResponse::id)
                .containsExactlyInAnyOrder(1L, 5L);
    }

    // ─── listAllForAdmin: paginación ─────────────────────────────────────────────

    @Test
    void should_paginate_in_memory_when_more_results_than_page_size() {
        List<Order> all = List.of(
                order(5L, OrderStatus.PENDIENTE, at(5)),
                order(4L, OrderStatus.PENDIENTE, at(4)),
                order(3L, OrderStatus.PENDIENTE, at(3)),
                order(2L, OrderStatus.PENDIENTE, at(2)),
                order(1L, OrderStatus.PENDIENTE, at(1)));
        when(orderRepository.findAll(any(Sort.class))).thenReturn(all);

        // size 2 → 3 páginas (5 elementos). Página 1 (segunda) = elementos índice 2,3.
        PageResponse<OrderResponse> page1 = service.listAllForAdmin(null, null, null, 1, 2);

        assertThat(page1.content()).hasSize(2);
        assertThat(page1.content()).extracting(OrderResponse::id).containsExactly(3L, 2L);
        assertThat(page1.totalElements()).isEqualTo(5L);
        assertThat(page1.totalPages()).isEqualTo(3);
        assertThat(page1.page()).isEqualTo(1);
        assertThat(page1.size()).isEqualTo(2);
    }

    @Test
    void should_return_empty_content_when_page_out_of_range() {
        List<Order> all = List.of(order(1L, OrderStatus.PENDIENTE, at(1)));
        when(orderRepository.findAll(any(Sort.class))).thenReturn(all);

        PageResponse<OrderResponse> result = service.listAllForAdmin(null, null, null, 5, 20);

        assertThat(result.content()).isEmpty();
        assertThat(result.totalElements()).isEqualTo(1L);
    }

    // ─── getByIdForAdmin ─────────────────────────────────────────────────────────

    @Test
    void should_return_order_when_found_by_id_for_admin() {
        Order o = order(7L, OrderStatus.CONFIRMADA, at(2));
        when(orderRepository.findById(7L)).thenReturn(Optional.of(o));

        OrderResponse result = service.getByIdForAdmin(7L);

        assertThat(result.id()).isEqualTo(7L);
        assertThat(result.status()).isEqualTo("CONFIRMADA");
    }

    @Test
    void should_throw_ResourceNotFound_when_order_missing_for_admin() {
        when(orderRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getByIdForAdmin(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ─── updateStatusForAdmin ────────────────────────────────────────────────────

    @Test
    void should_update_status_when_transition_is_valid() {
        Order o = order(4L, OrderStatus.CONFIRMADA, at(2));
        when(orderRepository.findById(4L)).thenReturn(Optional.of(o));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

        OrderResponse result = service.updateStatusForAdmin(4L, OrderStatus.ENVIADO);

        verify(orderStatusPolicy).assertCanTransition(OrderStatus.CONFIRMADA, OrderStatus.ENVIADO);
        assertThat(o.getStatus()).isEqualTo(OrderStatus.ENVIADO);
        assertThat(result.status()).isEqualTo("ENVIADO");
        verify(orderRepository).save(o);
        verify(monolitoStockClient, never()).revertSale(any()); // solo se repone al CANCELAR
    }

    @Test
    void should_revert_stock_in_monolith_when_cancelling_order() {
        Order o = order(8L, OrderStatus.PENDIENTE, at(2)); // item: productId 10, qty 1
        when(orderRepository.findById(8L)).thenReturn(Optional.of(o));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

        service.updateStatusForAdmin(8L, OrderStatus.CANCELADA);

        ArgumentCaptor<StockSaleRequest> captor = ArgumentCaptor.forClass(StockSaleRequest.class);
        verify(monolitoStockClient).revertSale(captor.capture());
        StockSaleRequest req = captor.getValue();
        assertThat(req.reference()).isEqualTo("8"); // la orden como referencia
        assertThat(req.items()).hasSize(1);
        assertThat(req.items().get(0).productId()).isEqualTo(10L);
        assertThat(req.items().get(0).quantity()).isEqualTo(1);
    }

    @Test
    void should_throw_OrderStatusTransition_and_not_save_when_transition_is_illegal() {
        Order o = order(6L, OrderStatus.ENTREGADO, at(2));
        when(orderRepository.findById(6L)).thenReturn(Optional.of(o));
        doThrow(new OrderStatusTransitionException("ilegal"))
                .when(orderStatusPolicy).assertCanTransition(OrderStatus.ENTREGADO, OrderStatus.PENDIENTE);

        assertThatThrownBy(() -> service.updateStatusForAdmin(6L, OrderStatus.PENDIENTE))
                .isInstanceOf(OrderStatusTransitionException.class);

        assertThat(o.getStatus()).isEqualTo(OrderStatus.ENTREGADO); // no mutó
        verify(orderRepository, never()).save(any());
    }

    @Test
    void should_throw_ResourceNotFound_when_updating_status_of_missing_order() {
        when(orderRepository.findById(50L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.updateStatusForAdmin(50L, OrderStatus.CONFIRMADA))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(orderRepository, never()).save(any());
    }
}
