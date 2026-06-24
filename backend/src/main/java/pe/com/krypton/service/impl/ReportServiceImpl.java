package pe.com.krypton.service.impl;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.com.krypton.dto.response.OrderItemResponse;
import pe.com.krypton.dto.response.OrderResponse;
import pe.com.krypton.dto.response.PageResponse;
import pe.com.krypton.dto.response.report.KardexMovimientoRow;
import pe.com.krypton.dto.response.report.KardexReport;
import pe.com.krypton.dto.response.report.OrdenesListadoReport;
import pe.com.krypton.dto.response.report.TopProductoRow;
import pe.com.krypton.dto.response.report.TopProductosReport;
import pe.com.krypton.dto.response.report.VentasPeriodoRow;
import pe.com.krypton.dto.response.report.VentasPorPeriodoReport;
import pe.com.krypton.exception.ResourceNotFoundException;
import pe.com.krypton.model.Product;
import pe.com.krypton.model.StockMovement;
import pe.com.krypton.model.enums.OrderStatus;
import pe.com.krypton.repository.ProductRepository;
import pe.com.krypton.repository.StockMovementRepository;
import pe.com.krypton.service.AdminOrderService;
import pe.com.krypton.service.ReportService;

@Service
@Transactional(readOnly = true)
public class ReportServiceImpl implements ReportService {

    private static final ZoneId LIMA = ZoneId.of("America/Lima");
    /** Tamaño de página al traer órdenes de pedidos vía Feign (se pagina hasta agotar). */
    private static final int PAGE_SIZE = 200;

    /**
     * Estados que cuentan como VENTA en los reportes R1/R2: una orden pagada y no cancelada.
     * Una venta sigue siendo venta aunque el admin la avance a ENVIADO o ENTREGADO; por eso
     * NO se filtra solo por CONFIRMADA. PENDIENTE (sin pagar) y CANCELADA (revertida) quedan fuera.
     */
    private static final Set<OrderStatus> VENTA_STATUSES =
            EnumSet.of(OrderStatus.CONFIRMADA, OrderStatus.ENVIADO, OrderStatus.ENTREGADO);

    private final StockMovementRepository stockMovementRepository;
    private final ProductRepository productRepository;
    private final AdminOrderService adminOrderService;

    public ReportServiceImpl(StockMovementRepository stockMovementRepository,
                             ProductRepository productRepository,
                             AdminOrderService adminOrderService) {
        this.stockMovementRepository = stockMovementRepository;
        this.productRepository = productRepository;
        this.adminOrderService = adminOrderService;
    }

    // ─── R1: Ventas por período ──────────────────────────────────────────────────

    @Override
    public VentasPorPeriodoReport ventasPorPeriodo(LocalDate desde, LocalDate hasta, String granularidad) {
        validateDateRange(desde, hasta, true /* both required */);

        String gran = mapGranularidad(granularidad);

        Instant start = toStartOfDay(desde);
        Instant end   = toExclusiveEnd(hasta);

        // pedidos-service es dueño de las órdenes: traemos TODAS las del rango [start, end)
        // paginando hasta agotar, y nos quedamos con las que cuentan como venta
        // (CONFIRMADA/ENVIADO/ENTREGADO). El bucketing por día/mes en zona Lima es en memoria.
        List<OrderResponse> orders = fetchVentaOrders(start, end);

        Map<LocalDate, List<OrderResponse>> porBucket = orders.stream()
                .collect(Collectors.groupingBy(
                        o -> bucket(o.orderDate(), gran), TreeMap::new, Collectors.toList()));

        List<VentasPeriodoRow> filas = porBucket.entrySet().stream()
                .map(e -> new VentasPeriodoRow(
                        e.getKey(),
                        e.getValue().size(),
                        sumTotal(e.getValue())))
                .toList();

        long totalOrdenes = orders.size();
        BigDecimal totalFacturado = sumTotal(orders);
        BigDecimal ticketPromedio = totalOrdenes == 0
                ? BigDecimal.ZERO
                : totalFacturado.divide(BigDecimal.valueOf(totalOrdenes), 2, RoundingMode.HALF_UP);

        return new VentasPorPeriodoReport(
                start,
                end,
                granularidad,
                totalOrdenes,
                totalFacturado,
                ticketPromedio,
                filas);
    }

    /**
     * Trae las órdenes que cuentan como VENTA en el rango: todas las pagadas y no canceladas
     * (CONFIRMADA, ENVIADO o ENTREGADO). Pide TODAS a pedidos y filtra por estado en memoria,
     * porque el endpoint admin de pedidos solo acepta un único estado por llamada.
     */
    private List<OrderResponse> fetchVentaOrders(Instant start, Instant end) {
        return fetchAllOrders(null, start, end).stream()
                .filter(o -> isVenta(o.status()))
                .toList();
    }

    /** ¿El estado (string del snapshot de pedidos) cuenta como venta? Desconocidos → no. */
    private boolean isVenta(String status) {
        try {
            return VENTA_STATUSES.contains(OrderStatus.valueOf(status));
        } catch (IllegalArgumentException | NullPointerException e) {
            return false;
        }
    }

    /** Trae todas las órdenes del rango [start, end) con el status dado (null = todos), paginando hasta agotar. */
    private List<OrderResponse> fetchAllOrders(OrderStatus status, Instant start, Instant end) {
        List<OrderResponse> all = new ArrayList<>();
        int page = 0;
        PageResponse<OrderResponse> resp;
        do {
            resp = adminOrderService.getAllOrders(status, start, end, page, PAGE_SIZE);
            all.addAll(resp.content());
            page++;
        } while (page < resp.totalPages());
        return all;
    }

    /** Bucket de período: el día Lima de la orden, o el primer día del mes Lima si gran = month. */
    private LocalDate bucket(Instant orderDate, String gran) {
        LocalDate limaDate = orderDate.atZone(LIMA).toLocalDate();
        return "month".equals(gran) ? limaDate.withDayOfMonth(1) : limaDate;
    }

    /** Suma los {@code total} de las órdenes (cada uno ya trae IGV incluido). */
    private BigDecimal sumTotal(List<OrderResponse> orders) {
        return orders.stream()
                .map(OrderResponse::total)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    // ─── R2: Productos más vendidos ──────────────────────────────────────────────

    @Override
    public TopProductosReport topProductos(LocalDate desde, LocalDate hasta, int limit) {
        validateLimit(limit);
        validatePartialDateRange(desde, hasta);
        if (desde != null && hasta != null && desde.isAfter(hasta)) {
            throw new IllegalArgumentException("desde debe ser <= hasta");
        }

        Instant startInst = desde == null ? null : toStartOfDay(desde);
        Instant endInst   = hasta == null ? null : toExclusiveEnd(hasta);

        // pedidos es dueño de las órdenes: traemos las ventas del rango (opcional) —pagadas y no
        // canceladas— y agrupamos por producto en memoria. unidades = Σ quantity; ingresos = Σ (quantity * unitPrice).
        List<OrderResponse> orders = fetchVentaOrders(startInst, endInst);

        Map<Long, ProductoAcumulado> porProducto = new LinkedHashMap<>();
        for (OrderResponse o : orders) {
            for (OrderItemResponse it : o.items()) {
                porProducto
                        .computeIfAbsent(it.productId(), id -> new ProductoAcumulado(it.productName()))
                        .add(it.quantity(), it.unitPrice());
            }
        }

        // El sku NO viaja en el snapshot de pedidos: lo resolvemos del catálogo local en 1 batch.
        Map<Long, String> skuPorId = productRepository.findAllById(porProducto.keySet()).stream()
                .collect(Collectors.toMap(Product::getId, Product::getSku));

        List<TopProductoRow> productos = porProducto.entrySet().stream()
                .map(e -> new TopProductoRow(
                        e.getKey(),
                        skuPorId.getOrDefault(e.getKey(), ""),
                        e.getValue().getNombre(),
                        e.getValue().getUnidades(),
                        e.getValue().getIngresos()))
                // unidades DESC; desempate determinista: ingresos DESC, luego productId ASC.
                .sorted(Comparator.comparingLong(TopProductoRow::unidades).reversed()
                        .thenComparing(Comparator.comparing(TopProductoRow::ingresos).reversed())
                        .thenComparing(TopProductoRow::productId))
                .limit(limit)
                .toList();

        return new TopProductosReport(startInst, endInst, limit, productos);
    }

    // ─── R3: Kardex por producto ─────────────────────────────────────────────────

    @Override
    public KardexReport kardexProducto(Long productId, LocalDate desde, LocalDate hasta) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Producto no encontrado: " + productId));

        validatePartialDateRange(desde, hasta);

        List<StockMovement> movements;
        Instant startInst = null;
        Instant endInst   = null;

        if (desde != null) {
            startInst = toStartOfDay(desde);
            endInst   = toExclusiveEnd(hasta);
            movements = stockMovementRepository
                    .findByProduct_IdAndCreatedAtBetweenOrderByCreatedAtAsc(productId, startInst, endInst);
        } else {
            movements = stockMovementRepository
                    .findByProduct_IdOrderByCreatedAtAsc(productId);
        }

        List<KardexMovimientoRow> rows = movements.stream()
                .map(sm -> new KardexMovimientoRow(
                        sm.getCreatedAt(),
                        sm.getType().name(),
                        sm.getQuantity(),
                        sm.getReason(),
                        sm.getReference()))
                .toList();

        return new KardexReport(
                product.getId(),
                product.getSku(),
                product.getName(),
                product.getStock(),
                startInst,
                endInst,
                rows);
    }

    // ─── R4: Listado de órdenes ──────────────────────────────────────────────────

    @Override
    public OrdenesListadoReport listadoOrdenes(String status, LocalDate desde, LocalDate hasta, Long userId) {
        validatePartialDateRange(desde, hasta);

        OrderStatus orderStatus = parseStatus(status);

        Instant startInst = desde != null ? toStartOfDay(desde) : null;
        Instant endInst   = hasta != null ? toExclusiveEnd(hasta) : null;

        // pedidos filtra status+rango y ordena por orderDate DESC. El endpoint admin de pedidos
        // NO soporta filtro por userId → lo aplicamos en memoria (preserva el orden DESC).
        List<OrderResponse> ordenes = fetchAllOrders(orderStatus, startInst, endInst).stream()
                .filter(o -> userId == null || userId.equals(o.userId()))
                .toList();

        return new OrdenesListadoReport(status, startInst, endInst, userId, ordenes.size(), ordenes);
    }

    // ─── private helpers ─────────────────────────────────────────────────────────

    /** Lima-timezone start-of-day → UTC Instant (inclusive lower bound). */
    private Instant toStartOfDay(LocalDate date) {
        return date.atStartOfDay(LIMA).toInstant();
    }

    /**
     * Lima-timezone start of NEXT day → UTC Instant (exclusive upper bound).
     * The range contract is half-open [start, end).
     */
    private Instant toExclusiveEnd(LocalDate date) {
        return date.plusDays(1).atStartOfDay(LIMA).toInstant();
    }

    /**
     * Maps the user-facing granularidad to the bucket token used by ventasPorPeriodo.
     * "dia" → "day", "mes" → "month". Null / unknown → IllegalArgumentException.
     */
    private String mapGranularidad(String granularidad) {
        if (granularidad == null) {
            throw new IllegalArgumentException("granularidad es obligatorio (dia|mes)");
        }
        return switch (granularidad.toLowerCase()) {
            case "dia"  -> "day";
            case "mes"  -> "month";
            default     -> throw new IllegalArgumentException(
                    "granularidad inválida: '" + granularidad + "'. Valores válidos: dia, mes");
        };
    }

    /**
     * Validates that limit is in range [1, 100].
     */
    private void validateLimit(int limit) {
        if (limit <= 0) {
            throw new IllegalArgumentException("limit debe ser >= 1, recibido: " + limit);
        }
        if (limit > 100) {
            throw new IllegalArgumentException("limit máximo es 100, recibido: " + limit);
        }
    }

    /**
     * Validates that BOTH dates are present or BOTH are absent.
     * Throws IllegalArgumentException if only one is provided (partial range).
     */
    private void validatePartialDateRange(LocalDate desde, LocalDate hasta) {
        if ((desde == null) != (hasta == null)) {
            throw new IllegalArgumentException(
                    "desde y hasta deben proporcionarse juntos (ambos o ninguno)");
        }
    }

    /**
     * Validates a required date range (both must be present) and desde <= hasta.
     */
    private void validateDateRange(LocalDate desde, LocalDate hasta, boolean required) {
        if (required && (desde == null || hasta == null)) {
            throw new IllegalArgumentException("desde y hasta son obligatorios");
        }
        if (desde != null && hasta != null && desde.isAfter(hasta)) {
            throw new IllegalArgumentException("desde debe ser <= hasta");
        }
    }

    /**
     * Parses a status string (case-insensitive) to OrderStatus enum.
     * Returns null when input is null (no filter). Throws IllegalArgumentException for unknown values.
     */
    private OrderStatus parseStatus(String status) {
        if (status == null) {
            return null;
        }
        try {
            return OrderStatus.valueOf(status.toUpperCase());
        } catch (IllegalArgumentException e) {
            String valid = Arrays.stream(OrderStatus.values())
                    .map(Enum::name)
                    .collect(Collectors.joining(", "));
            throw new IllegalArgumentException(
                    "status inválido: '" + status + "'. Valores válidos: " + valid);
        }
    }

    /** Acumulador mutable de R2: suma unidades e ingresos por producto, reteniendo el nombre snapshot. */
    private static final class ProductoAcumulado {
        private final String nombre;
        private long unidades;
        private BigDecimal ingresos = BigDecimal.ZERO;

        private ProductoAcumulado(String nombre) {
            this.nombre = nombre;
        }

        private void add(int quantity, BigDecimal unitPrice) {
            this.unidades += quantity;
            this.ingresos = this.ingresos.add(unitPrice.multiply(BigDecimal.valueOf(quantity)));
        }

        private String getNombre()       { return nombre; }
        private long getUnidades()       { return unidades; }
        private BigDecimal getIngresos() { return ingresos; }
    }
}
