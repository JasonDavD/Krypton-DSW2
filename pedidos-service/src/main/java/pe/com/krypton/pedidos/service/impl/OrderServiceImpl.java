package pe.com.krypton.pedidos.service.impl;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import pe.com.krypton.pedidos.client.CatalogoClient;
import pe.com.krypton.pedidos.client.MonolitoStockClient;
import pe.com.krypton.pedidos.client.ProductDTO;
import pe.com.krypton.pedidos.dto.request.CheckoutRequest;
import pe.com.krypton.pedidos.dto.request.CheckoutRequest.CheckoutItem;
import pe.com.krypton.pedidos.dto.request.PaymentRequest;
import pe.com.krypton.pedidos.dto.request.StockSaleRequest;
import pe.com.krypton.pedidos.dto.response.OrderItemResponse;
import pe.com.krypton.pedidos.dto.response.OrderResponse;
import pe.com.krypton.pedidos.dto.response.PageResponse;
import pe.com.krypton.pedidos.exception.CatalogoUnavailableException;
import pe.com.krypton.pedidos.exception.InsufficientStockException;
import pe.com.krypton.pedidos.exception.OrderStatusTransitionException;
import pe.com.krypton.pedidos.exception.ResourceNotFoundException;
import pe.com.krypton.pedidos.model.Order;
import pe.com.krypton.pedidos.model.OrderItem;
import pe.com.krypton.pedidos.model.enums.OrderStatus;
import pe.com.krypton.pedidos.policy.OrderStatusPolicy;
import pe.com.krypton.pedidos.repository.OrderRepository;
import pe.com.krypton.pedidos.service.OrderService;
import pe.com.krypton.pedidos.service.SequenceGenerator;

@Service
public class OrderServiceImpl implements OrderService {

    // ── Reglas de negocio de facturación (paridad con el monolito) ──
    /** Envío gratis si el subtotal alcanza este umbral. */
    private static final BigDecimal FREE_SHIPPING_THRESHOLD = new BigDecimal("300.00");
    /** Costo de envío fijo cuando no aplica el envío gratis. */
    private static final BigDecimal SHIPPING_COST = new BigDecimal("20.00");
    /** 1 + tasa IGV (18%). El precio ya incluye IGV → se desglosa dividiendo por esto. */
    private static final BigDecimal IGV_DIVISOR = new BigDecimal("1.18");

    /** Nombre de la secuencia Mongo para el id entero de las órdenes. */
    private static final String ORDERS_SEQ = "orders_seq";

    private final OrderRepository orderRepository;
    private final CatalogoClient catalogoClient;
    private final MonolitoStockClient monolitoStockClient;
    private final SequenceGenerator sequenceGenerator;
    private final OrderStatusPolicy orderStatusPolicy;

    public OrderServiceImpl(OrderRepository orderRepository,
                            CatalogoClient catalogoClient,
                            MonolitoStockClient monolitoStockClient,
                            SequenceGenerator sequenceGenerator,
                            OrderStatusPolicy orderStatusPolicy) {
        this.orderRepository = orderRepository;
        this.catalogoClient = catalogoClient;
        this.monolitoStockClient = monolitoStockClient;
        this.sequenceGenerator = sequenceGenerator;
        this.orderStatusPolicy = orderStatusPolicy;
    }

    // ─── checkout ────────────────────────────────────────────────────────────────

    @Override
    public OrderResponse checkout(Long userId, CheckoutRequest request) {
        // Reprecia + valida stock contra el catálogo (el cliente no puede falsear el precio).
        List<OrderItem> items = new ArrayList<>();
        BigDecimal subtotal = BigDecimal.ZERO;

        for (CheckoutItem reqItem : request.items()) {
            ProductDTO product = catalogoClient.getById(reqItem.productId());
            // El fallback del circuit-breaker devuelve null → catálogo caído.
            if (product == null) {
                throw new CatalogoUnavailableException("Catálogo no disponible, intentá más tarde.");
            }
            int requested = reqItem.quantity();
            if (requested > product.stock()) {
                throw new InsufficientStockException(
                        "Stock insuficiente para el producto " + product.id()
                        + ": solicitado=" + requested + ", disponible=" + product.stock());
            }
            // Línea embebida con precio y nombre congelados (snapshot al checkout).
            items.add(new OrderItem(product.id(), product.name(), requested, product.price()));
            subtotal = subtotal.add(product.price().multiply(BigDecimal.valueOf(requested)));
        }

        // ── Montos: envío + total + IGV desglosado hacia adentro ──
        BigDecimal shippingCost = subtotal.compareTo(FREE_SHIPPING_THRESHOLD) >= 0
                ? BigDecimal.ZERO
                : SHIPPING_COST;
        BigDecimal total = subtotal.add(shippingCost);
        // El precio ya incluye IGV: base = total / 1.18 (redondeada), igv = total − base.
        // Restar garantiza base + igv == total exacto (sin descuadres de centavos).
        BigDecimal base = total.divide(IGV_DIVISOR, 2, RoundingMode.HALF_UP);
        BigDecimal igv = total.subtract(base);

        Order order = new Order();
        order.setId(sequenceGenerator.nextId(ORDERS_SEQ)); // id entero (Mongo no autoincrementa)
        order.setUserId(userId);
        order.setOrderDate(Instant.now());
        order.setStatus(OrderStatus.PENDIENTE);
        order.setDocumentType(request.documentType());
        order.setCustomerName(request.customerName());
        order.setCustomerDoc(request.customerDoc());
        order.setSubtotal(subtotal);
        order.setShippingCost(shippingCost);
        order.setIgv(igv);
        order.setTotal(total);
        order.setItems(items);

        Order saved = orderRepository.save(order);

        // Notifica la venta al monolito (master del stock) para que descuente stock + kardex y
        // re-sincronice el catálogo. Best-effort: si el monolito está caído, el fallback no rompe
        // la compra (la orden ya quedó persistida acá). Degradación elegante.
        List<StockSaleRequest.Line> soldLines = items.stream()
                .map(it -> new StockSaleRequest.Line(it.getProductId(), it.getQuantity()))
                .toList();
        monolitoStockClient.registerSale(
                new StockSaleRequest(String.valueOf(saved.getId()), soldLines));

        return toResponse(saved);
    }

    // ─── read ────────────────────────────────────────────────────────────────────

    @Override
    public List<OrderResponse> getMyOrders(Long userId) {
        return orderRepository.findByUserIdOrderByOrderDateDesc(userId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    public OrderResponse getMyOrder(Long userId, Long orderId) {
        Order order = orderRepository.findByIdAndUserId(orderId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Orden no encontrada: " + orderId));
        return toResponse(order);
    }

    // ─── pay ───────────────────────────────────────────────────────────────────────

    @Override
    public OrderResponse pay(Long userId, Long orderId, PaymentRequest request) {
        Order order = orderRepository.findByIdAndUserId(orderId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Orden no encontrada: " + orderId));
        // Pago = transición PENDIENTE → CONFIRMADA. Cualquier otro estado es ilegal.
        if (order.getStatus() != OrderStatus.PENDIENTE) {
            throw new OrderStatusTransitionException(
                    "No se puede pagar una orden en estado " + order.getStatus());
        }
        order.setStatus(OrderStatus.CONFIRMADA);
        Order saved = orderRepository.save(order);
        return toResponse(saved);
    }

    // ─── admin ───────────────────────────────────────────────────────────────────

    @Override
    public PageResponse<OrderResponse> listAllForAdmin(
            OrderStatus status, Instant from, Instant to, int page, int size) {
        // Escala demo: traemos TODO ordenado por fecha DESC y filtramos/paginamos en memoria.
        // A escala real esto se empujaría a Mongo con Query + Criteria (status eq, orderDate
        // gte from / lt to) y paginación nativa; en demo, con pocos documentos, en memoria es
        // suficiente y trivial de testear (no requiere @DataMongoTest ni índices).
        List<Order> all = orderRepository.findAll(Sort.by(Sort.Direction.DESC, "orderDate"));

        List<OrderResponse> filtered = all.stream()
                .filter(o -> status == null || o.getStatus() == status)
                // Rango half-open [from, to): cada borde es opcional e independiente.
                .filter(o -> from == null || !o.getOrderDate().isBefore(from)) // orderDate >= from
                .filter(o -> to == null || o.getOrderDate().isBefore(to))      // orderDate <  to
                .map(this::toResponse)
                .toList();

        long totalElements = filtered.size();
        int fromIndex = Math.min(page * size, filtered.size());
        int toIndex = Math.min(fromIndex + size, filtered.size());
        List<OrderResponse> pageContent = filtered.subList(fromIndex, toIndex);

        return PageResponse.of(pageContent, page, size, totalElements);
    }

    @Override
    public OrderResponse getByIdForAdmin(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Orden no encontrada: " + orderId));
        return toResponse(order);
    }

    @Override
    public OrderResponse updateStatusForAdmin(Long orderId, OrderStatus newStatus) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Orden no encontrada: " + orderId));
        // Valida la transición contra la máquina de estados (lanza 422 si es ilegal).
        orderStatusPolicy.assertCanTransition(order.getStatus(), newStatus);
        order.setStatus(newStatus);
        // NOTA: no se ajusta stock acá. La saga de stock cross-service es deuda conocida (fuera de alcance).
        Order saved = orderRepository.save(order);
        return toResponse(saved);
    }

    // ─── mapping ─────────────────────────────────────────────────────────────────

    /**
     * Mapea el documento Order a su DTO. subtotal por línea = unitPrice (snapshot) × qty.
     * Los items embebidos no tienen id propio → id de línea null (shape monolito conservado).
     */
    private OrderResponse toResponse(Order order) {
        List<OrderItemResponse> itemResponses = new ArrayList<>();
        for (OrderItem item : order.getItems()) {
            BigDecimal lineSubtotal = item.getUnitPrice()
                    .multiply(BigDecimal.valueOf(item.getQuantity()));
            itemResponses.add(new OrderItemResponse(
                    null, // embebido: sin id propio
                    item.getProductId(),
                    item.getProductName(),
                    item.getQuantity(),
                    item.getUnitPrice(),
                    lineSubtotal));
        }
        return new OrderResponse(
                order.getId(),
                order.getUserId(),
                order.getOrderDate(),
                order.getStatus().name(),
                order.getDocumentType().name(),
                order.getCustomerName(),
                order.getCustomerDoc(),
                order.getSubtotal(),
                order.getShippingCost(),
                order.getIgv(),
                order.getTotal(),
                itemResponses);
    }
}
