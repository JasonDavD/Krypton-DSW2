package pe.com.krypton.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pe.com.krypton.dto.response.OrderItemResponse;
import pe.com.krypton.dto.response.OrderResponse;
import pe.com.krypton.dto.response.PageResponse;
import pe.com.krypton.dto.response.report.KardexReport;
import pe.com.krypton.dto.response.report.OrdenesListadoReport;
import pe.com.krypton.dto.response.report.TopProductoRow;
import pe.com.krypton.dto.response.report.TopProductosReport;
import pe.com.krypton.dto.response.report.VentasPorPeriodoReport;
import pe.com.krypton.exception.ResourceNotFoundException;
import pe.com.krypton.model.Product;
import pe.com.krypton.model.StockMovement;
import pe.com.krypton.model.enums.MovementType;
import pe.com.krypton.model.enums.OrderStatus;
import pe.com.krypton.repository.ProductRepository;
import pe.com.krypton.repository.StockMovementRepository;
import pe.com.krypton.service.impl.ReportServiceImpl;

/**
 * Unit test de ReportServiceImpl. Colaboradores MOCKEADOS. Sin Spring context, sin DB.
 *
 * <p>Tras la migración a microservicios, R1 (ventas por período) ya NO lee la tabla MySQL
 * local (vacía): trae las órdenes CONFIRMADA desde {@code pedidos-service} vía
 * {@link AdminOrderService} (Feign) paginando hasta agotar, y AGREGA EN MEMORIA el bucketing
 * por día/mes en zona America/Lima. El kardex y los demás reportes siguen siendo locales.
 */
@ExtendWith(MockitoExtension.class)
class ReportServiceImplTest {

    @Mock StockMovementRepository stockMovementRepository;
    @Mock ProductRepository productRepository;
    @Mock AdminOrderService adminOrderService;

    ReportServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new ReportServiceImpl(
                stockMovementRepository,
                productRepository,
                adminOrderService);
    }

    // ─── R1: solo pide CONFIRMADA a pedidos, con los instants Lima ──────────────────

    /**
     * Lima is UTC-5. 2024-03-01 Lima midnight = 2024-03-01T05:00:00Z.
     * Next-day exclusive boundary: 2024-03-02T05:00:00Z.
     * El servicio delega el filtro de estado + rango a pedidos: solo agrega en memoria.
     */
    @Test
    void ventasPorPeriodo_requests_only_confirmadas_with_lima_boundary_instants() {
        LocalDate desde = LocalDate.of(2024, 3, 1);
        LocalDate hasta = LocalDate.of(2024, 3, 1);

        Instant expectedStart = Instant.parse("2024-03-01T05:00:00Z");
        Instant expectedEnd   = Instant.parse("2024-03-02T05:00:00Z");

        when(adminOrderService.getAllOrders(eq(OrderStatus.CONFIRMADA), eq(expectedStart), eq(expectedEnd),
                anyInt(), anyInt()))
                .thenReturn(emptyPage());

        service.ventasPorPeriodo(desde, hasta, "dia");

        verify(adminOrderService).getAllOrders(
                eq(OrderStatus.CONFIRMADA), eq(expectedStart), eq(expectedEnd), anyInt(), anyInt());
    }

    // ─── R1: bucketing en memoria por día ───────────────────────────────────────────

    @Test
    void ventasPorPeriodo_gran_dia_groups_per_lima_day_and_sums_total() {
        // 2 órdenes el 2024-01-10 Lima, 1 el 2024-01-11 Lima.
        OrderResponse o1 = confirmada(Instant.parse("2024-01-10T17:00:00Z"), "100.00"); // 12:00 Lima
        OrderResponse o2 = confirmada(Instant.parse("2024-01-10T20:00:00Z"), "50.00");  // 15:00 Lima
        OrderResponse o3 = confirmada(Instant.parse("2024-01-11T14:00:00Z"), "30.00");  // 09:00 Lima

        when(adminOrderService.getAllOrders(any(), any(), any(), anyInt(), anyInt()))
                .thenReturn(singlePage(List.of(o1, o2, o3)));

        VentasPorPeriodoReport report = service.ventasPorPeriodo(
                LocalDate.of(2024, 1, 1), LocalDate.of(2024, 1, 31), "dia");

        assertThat(report.filas()).hasSize(2);
        assertThat(report.filas().get(0).periodo()).isEqualTo(LocalDate.of(2024, 1, 10));
        assertThat(report.filas().get(0).ordenes()).isEqualTo(2L);
        assertThat(report.filas().get(0).monto()).isEqualByComparingTo("150.00");
        assertThat(report.filas().get(1).periodo()).isEqualTo(LocalDate.of(2024, 1, 11));
        assertThat(report.filas().get(1).ordenes()).isEqualTo(1L);
        assertThat(report.filas().get(1).monto()).isEqualByComparingTo("30.00");
    }

    @Test
    void ventasPorPeriodo_gran_mes_groups_per_lima_month_first_day() {
        OrderResponse ene = confirmada(Instant.parse("2024-01-15T15:00:00Z"), "200.00");
        OrderResponse feb = confirmada(Instant.parse("2024-02-20T15:00:00Z"), "300.00");

        when(adminOrderService.getAllOrders(any(), any(), any(), anyInt(), anyInt()))
                .thenReturn(singlePage(List.of(ene, feb)));

        VentasPorPeriodoReport report = service.ventasPorPeriodo(
                LocalDate.of(2024, 1, 1), LocalDate.of(2024, 2, 29), "mes");

        assertThat(report.filas()).hasSize(2);
        assertThat(report.filas().get(0).periodo()).isEqualTo(LocalDate.of(2024, 1, 1));
        assertThat(report.filas().get(1).periodo()).isEqualTo(LocalDate.of(2024, 2, 1));
    }

    /**
     * Borde de zona horaria: una orden cuyo instante UTC cae en el día siguiente, pero en
     * Lima (UTC-5) todavía es el día anterior, debe agruparse en el día Lima, no el UTC.
     * 2024-03-02T03:00:00Z = 2024-03-01 22:00 en Lima → bucket 2024-03-01.
     */
    @Test
    void ventasPorPeriodo_assigns_order_to_lima_day_not_utc_day() {
        OrderResponse cruza = confirmada(Instant.parse("2024-03-02T03:00:00Z"), "80.00");

        when(adminOrderService.getAllOrders(any(), any(), any(), anyInt(), anyInt()))
                .thenReturn(singlePage(List.of(cruza)));

        VentasPorPeriodoReport report = service.ventasPorPeriodo(
                LocalDate.of(2024, 3, 1), LocalDate.of(2024, 3, 2), "dia");

        assertThat(report.filas()).hasSize(1);
        assertThat(report.filas().get(0).periodo()).isEqualTo(LocalDate.of(2024, 3, 1));
    }

    // ─── R1: paginación hasta agotar ────────────────────────────────────────────────

    @Test
    void ventasPorPeriodo_paginates_until_exhausted_accumulating_all_pages() {
        OrderResponse p0 = confirmada(Instant.parse("2024-01-10T15:00:00Z"), "100.00");
        OrderResponse p1 = confirmada(Instant.parse("2024-01-10T16:00:00Z"), "100.00");

        // 2 páginas (totalPages = 2): cada llamada devuelve 1 elemento.
        when(adminOrderService.getAllOrders(any(), any(), any(), eq(0), anyInt()))
                .thenReturn(new PageResponse<>(List.of(p0), 0, 1, 2L, 2));
        when(adminOrderService.getAllOrders(any(), any(), any(), eq(1), anyInt()))
                .thenReturn(new PageResponse<>(List.of(p1), 1, 1, 2L, 2));

        VentasPorPeriodoReport report = service.ventasPorPeriodo(
                LocalDate.of(2024, 1, 1), LocalDate.of(2024, 1, 31), "dia");

        assertThat(report.totalOrdenes()).isEqualTo(2L);
        assertThat(report.totalFacturado()).isEqualByComparingTo("200.00");
        verify(adminOrderService).getAllOrders(any(), any(), any(), eq(0), anyInt());
        verify(adminOrderService).getAllOrders(any(), any(), any(), eq(1), anyInt());
    }

    // ─── R1: totales y ticket promedio ──────────────────────────────────────────────

    @Test
    void ventasPorPeriodo_computes_totals_and_ticket_promedio() {
        OrderResponse o1 = confirmada(Instant.parse("2024-01-10T15:00:00Z"), "100.00");
        OrderResponse o2 = confirmada(Instant.parse("2024-01-11T15:00:00Z"), "50.00");

        when(adminOrderService.getAllOrders(any(), any(), any(), anyInt(), anyInt()))
                .thenReturn(singlePage(List.of(o1, o2)));

        VentasPorPeriodoReport report = service.ventasPorPeriodo(
                LocalDate.of(2024, 1, 1), LocalDate.of(2024, 1, 31), "dia");

        assertThat(report.totalOrdenes()).isEqualTo(2L);
        assertThat(report.totalFacturado()).isEqualByComparingTo("150.00");
        assertThat(report.ticketPromedio()).isEqualByComparingTo("75.00");
        assertThat(report.granularidad()).isEqualTo("dia");
    }

    @Test
    void ventasPorPeriodo_no_orders_returns_zeros_and_empty_rows() {
        when(adminOrderService.getAllOrders(any(), any(), any(), anyInt(), anyInt()))
                .thenReturn(emptyPage());

        VentasPorPeriodoReport report = service.ventasPorPeriodo(
                LocalDate.of(2024, 1, 1), LocalDate.of(2024, 1, 31), "dia");

        assertThat(report.totalOrdenes()).isZero();
        assertThat(report.totalFacturado()).isEqualByComparingTo("0");
        assertThat(report.ticketPromedio()).isEqualByComparingTo("0");
        assertThat(report.filas()).isEmpty();
    }

    // ─── R1: validaciones (no llegan a pedidos) ─────────────────────────────────────

    @Test
    void ventasPorPeriodo_invalid_gran_throws_illegal_argument() {
        assertThatThrownBy(() -> service.ventasPorPeriodo(
                LocalDate.of(2024, 1, 1), LocalDate.of(2024, 1, 31), "semana"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("granularidad");
    }

    @Test
    void ventasPorPeriodo_desde_after_hasta_throws_illegal_argument() {
        assertThatThrownBy(() -> service.ventasPorPeriodo(
                LocalDate.of(2024, 3, 1), LocalDate.of(2024, 2, 28), "dia"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    /**
     * REQ-RPT-07: un rango de un solo día (desde == hasta) es válido y significa un día calendario.
     */
    @Test
    void ventasPorPeriodo_sameDayRange_desdeEqualsHasta_isValid() {
        LocalDate sameDay = LocalDate.of(2025, 6, 15);
        OrderResponse o = confirmada(Instant.parse("2025-06-15T17:00:00Z"), "40.00");

        when(adminOrderService.getAllOrders(any(), any(), any(), anyInt(), anyInt()))
                .thenReturn(singlePage(List.of(o)));

        VentasPorPeriodoReport report = service.ventasPorPeriodo(sameDay, sameDay, "dia");

        assertThat(report).isNotNull();
        assertThat(report.totalOrdenes()).isEqualTo(1L);
        assertThat(report.filas()).hasSize(1);
        assertThat(report.filas().get(0).periodo()).isEqualTo(sameDay);
    }

    // ─── R2: topProductos ───────────────────────────────────────────────────────────

    @Test
    void topProductos_desde_after_hasta_throws_illegal_argument() {
        assertThatThrownBy(() -> service.topProductos(
                LocalDate.of(2024, 3, 1), LocalDate.of(2024, 2, 1), 10))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void topProductos_partial_date_range_desde_only_throws_illegal_argument() {
        assertThatThrownBy(() -> service.topProductos(
                LocalDate.of(2024, 1, 1), null, 10))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void topProductos_partial_date_range_hasta_only_throws_illegal_argument() {
        assertThatThrownBy(() -> service.topProductos(
                null, LocalDate.of(2024, 1, 31), 10))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void kardex_partial_date_range_desde_only_throws_illegal_argument() {
        Product p = stubProduct(1L, "SKU-001", "Prod", 10);
        when(productRepository.findById(1L)).thenReturn(Optional.of(p));

        assertThatThrownBy(() -> service.kardexProducto(1L, LocalDate.of(2024, 1, 1), null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void kardex_partial_date_range_hasta_only_throws_illegal_argument() {
        Product p = stubProduct(1L, "SKU-001", "Prod", 10);
        when(productRepository.findById(1L)).thenReturn(Optional.of(p));

        assertThatThrownBy(() -> service.kardexProducto(1L, null, LocalDate.of(2024, 1, 31)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void listadoOrdenes_partial_date_range_desde_only_throws_illegal_argument() {
        assertThatThrownBy(() -> service.listadoOrdenes(
                null, LocalDate.of(2024, 1, 1), null, null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void listadoOrdenes_partial_date_range_hasta_only_throws_illegal_argument() {
        assertThatThrownBy(() -> service.listadoOrdenes(
                null, null, LocalDate.of(2024, 1, 31), null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    // ─── limit validation ───────────────────────────────────────────────────────────

    @Test
    void topProductos_limit_greater_than_100_throws_illegal_argument() {
        assertThatThrownBy(() -> service.topProductos(null, null, 101))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void topProductos_limit_zero_throws_illegal_argument() {
        assertThatThrownBy(() -> service.topProductos(null, null, 0))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void topProductos_limit_negative_throws_illegal_argument() {
        assertThatThrownBy(() -> service.topProductos(null, null, -5))
                .isInstanceOf(IllegalArgumentException.class);
    }

    // ─── R2: agregación por producto (desde pedidos vía Feign) ─────────────────────────────────────────────

    @Test
    void topProductos_aggregates_units_and_revenue_per_product_resolving_sku() {
        // Producto 1: 2 + 3 = 5 unidades; Producto 2: 1 unidad.
        OrderResponse o1 = orderWithItems(Instant.parse("2024-01-10T15:00:00Z"),
                item(1L, "Laptop", 2, "100.00"), item(2L, "Mouse", 1, "50.00"));
        OrderResponse o2 = orderWithItems(Instant.parse("2024-01-11T15:00:00Z"),
                item(1L, "Laptop", 3, "100.00"));

        when(adminOrderService.getAllOrders(any(), any(), any(), anyInt(), anyInt()))
                .thenReturn(singlePage(List.of(o1, o2)));
        when(productRepository.findAllById(any()))
                .thenReturn(List.of(
                        stubProduct(1L, "KR-LAP-001", "Laptop", 10),
                        stubProduct(2L, "KR-PER-002", "Mouse", 10)));

        TopProductosReport report = service.topProductos(null, null, 10);

        assertThat(report.productos()).hasSize(2);
        TopProductoRow top = report.productos().get(0);
        assertThat(top.productId()).isEqualTo(1L);
        assertThat(top.sku()).isEqualTo("KR-LAP-001");
        assertThat(top.nombre()).isEqualTo("Laptop");
        assertThat(top.unidades()).isEqualTo(5L);
        assertThat(top.ingresos()).isEqualByComparingTo("500.00");
        assertThat(report.productos().get(1).productId()).isEqualTo(2L);
        assertThat(report.productos().get(1).unidades()).isEqualTo(1L);
        assertThat(report.productos().get(1).ingresos()).isEqualByComparingTo("50.00");
    }

    @Test
    void topProductos_orders_by_units_desc_and_caps_to_limit() {
        OrderResponse o = orderWithItems(Instant.parse("2024-01-10T15:00:00Z"),
                item(1L, "A", 1, "10.00"),
                item(2L, "B", 5, "10.00"),
                item(3L, "C", 3, "10.00"));

        when(adminOrderService.getAllOrders(any(), any(), any(), anyInt(), anyInt()))
                .thenReturn(singlePage(List.of(o)));
        when(productRepository.findAllById(any()))
                .thenReturn(List.of(
                        stubProduct(1L, "S1", "A", 1),
                        stubProduct(2L, "S2", "B", 1),
                        stubProduct(3L, "S3", "C", 1)));

        TopProductosReport report = service.topProductos(null, null, 2);

        assertThat(report.productos()).hasSize(2);
        assertThat(report.productos().get(0).productId()).isEqualTo(2L); // 5 unidades
        assertThat(report.productos().get(1).productId()).isEqualTo(3L); // 3 unidades
        // producto 1 (1 unidad) queda fuera por el cap = 2
    }

    @Test
    void topProductos_unknown_product_falls_back_to_empty_sku_keeps_snapshot_name() {
        OrderResponse o = orderWithItems(Instant.parse("2024-01-10T15:00:00Z"),
                item(99L, "Fantasma", 1, "10.00"));

        when(adminOrderService.getAllOrders(any(), any(), any(), anyInt(), anyInt()))
                .thenReturn(singlePage(List.of(o)));
        when(productRepository.findAllById(any())).thenReturn(List.of()); // no existe en catálogo local

        TopProductosReport report = service.topProductos(null, null, 10);

        assertThat(report.productos()).hasSize(1);
        assertThat(report.productos().get(0).sku()).isEmpty();
        assertThat(report.productos().get(0).nombre()).isEqualTo("Fantasma"); // del snapshot de pedidos
    }

    @Test
    void topProductos_no_dates_requests_confirmadas_without_date_filter() {
        when(adminOrderService.getAllOrders(eq(OrderStatus.CONFIRMADA), isNull(), isNull(), anyInt(), anyInt()))
                .thenReturn(emptyPage());
        when(productRepository.findAllById(any())).thenReturn(List.of());

        service.topProductos(null, null, 10);

        verify(adminOrderService).getAllOrders(
                eq(OrderStatus.CONFIRMADA), isNull(), isNull(), anyInt(), anyInt());
    }

    @Test
    void topProductos_with_dates_requests_confirmadas_in_lima_range() {
        Instant start = Instant.parse("2024-01-01T05:00:00Z");
        Instant end   = Instant.parse("2024-02-01T05:00:00Z");

        when(adminOrderService.getAllOrders(eq(OrderStatus.CONFIRMADA), eq(start), eq(end), anyInt(), anyInt()))
                .thenReturn(emptyPage());
        when(productRepository.findAllById(any())).thenReturn(List.of());

        service.topProductos(LocalDate.of(2024, 1, 1), LocalDate.of(2024, 1, 31), 10);

        verify(adminOrderService).getAllOrders(
                eq(OrderStatus.CONFIRMADA), eq(start), eq(end), anyInt(), anyInt());
    }

    // ─── kardex: product not found → 404 ───────────────────────────────────────────

    @Test
    void kardex_product_not_found_throws_resource_not_found() {
        when(productRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.kardexProducto(999L, null, null))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void kardex_product_found_no_dates_returns_all_movements() {
        Product p = stubProduct(1L, "SKU-001", "Prod A", 50);
        when(productRepository.findById(1L)).thenReturn(Optional.of(p));

        StockMovement sm = stubMovement(1L, p, MovementType.ENTRADA, 10);
        when(stockMovementRepository.findByProduct_IdOrderByCreatedAtAsc(1L))
                .thenReturn(List.of(sm));

        KardexReport report = service.kardexProducto(1L, null, null);

        assertThat(report.productId()).isEqualTo(1L);
        assertThat(report.sku()).isEqualTo("SKU-001");
        assertThat(report.stockActual()).isEqualTo(50);
        assertThat(report.movimientos()).hasSize(1);
        assertThat(report.movimientos().get(0).tipo()).isEqualTo("ENTRADA");
        assertThat(report.movimientos().get(0).cantidad()).isEqualTo(10);
    }

    @Test
    void kardex_with_dates_calls_between_method() {
        LocalDate desde = LocalDate.of(2024, 1, 1);
        LocalDate hasta = LocalDate.of(2024, 1, 31);

        Product p = stubProduct(1L, "SKU-001", "Prod", 20);
        when(productRepository.findById(1L)).thenReturn(Optional.of(p));
        when(stockMovementRepository.findByProduct_IdAndCreatedAtBetweenOrderByCreatedAtAsc(
                eq(1L), any(Instant.class), any(Instant.class)))
                .thenReturn(List.of());

        KardexReport report = service.kardexProducto(1L, desde, hasta);

        assertThat(report.movimientos()).isEmpty();
        verify(stockMovementRepository)
                .findByProduct_IdAndCreatedAtBetweenOrderByCreatedAtAsc(eq(1L), any(), any());
    }

    // ─── listadoOrdenes ─────────────────────────────────────────────────────────────

    @Test
    void listadoOrdenes_invalid_status_throws_illegal_argument() {
        assertThatThrownBy(() -> service.listadoOrdenes("UNKNOWN_STATUS", null, null, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("status");
    }

    @Test
    void listadoOrdenes_no_filters_returns_all_orders_from_pedidos() {
        OrderResponse o1 = order(1L, 10L, "PENDIENTE");
        OrderResponse o2 = order(2L, 20L, "CONFIRMADA");
        when(adminOrderService.getAllOrders(isNull(), isNull(), isNull(), anyInt(), anyInt()))
                .thenReturn(singlePage(List.of(o1, o2)));

        OrdenesListadoReport report = service.listadoOrdenes(null, null, null, null);

        assertThat(report.ordenes()).hasSize(2);
        assertThat(report.total()).isEqualTo(2L);
        assertThat(report.statusFiltro()).isNull();
    }

    @Test
    void listadoOrdenes_valid_status_string_passes_enum_to_pedidos() {
        when(adminOrderService.getAllOrders(eq(OrderStatus.CONFIRMADA), any(), any(), anyInt(), anyInt()))
                .thenReturn(emptyPage());

        OrdenesListadoReport report = service.listadoOrdenes("CONFIRMADA", null, null, null);

        assertThat(report.statusFiltro()).isEqualTo("CONFIRMADA");
        verify(adminOrderService).getAllOrders(
                eq(OrderStatus.CONFIRMADA), any(), any(), anyInt(), anyInt());
    }

    @Test
    void listadoOrdenes_filters_by_userId_in_memory() {
        OrderResponse u10a = order(1L, 10L, "PENDIENTE");
        OrderResponse u20  = order(2L, 20L, "CONFIRMADA");
        OrderResponse u10b = order(3L, 10L, "ENVIADO");
        when(adminOrderService.getAllOrders(any(), any(), any(), anyInt(), anyInt()))
                .thenReturn(singlePage(List.of(u10a, u20, u10b)));

        OrdenesListadoReport report = service.listadoOrdenes(null, null, null, 10L);

        assertThat(report.ordenes()).hasSize(2);
        assertThat(report.ordenes()).allMatch(o -> o.userId().equals(10L));
        assertThat(report.total()).isEqualTo(2L);
        assertThat(report.userId()).isEqualTo(10L);
    }

    @Test
    void listadoOrdenes_passes_lima_range_to_pedidos() {
        Instant start = Instant.parse("2024-01-01T05:00:00Z");
        Instant end   = Instant.parse("2024-02-01T05:00:00Z");
        when(adminOrderService.getAllOrders(isNull(), eq(start), eq(end), anyInt(), anyInt()))
                .thenReturn(emptyPage());

        service.listadoOrdenes(null, LocalDate.of(2024, 1, 1), LocalDate.of(2024, 1, 31), null);

        verify(adminOrderService).getAllOrders(isNull(), eq(start), eq(end), anyInt(), anyInt());
    }

    // ─── helpers ────────────────────────────────────────────────────────────────────

    /** Orden CONFIRMADA mínima: solo importan orderDate y total para la agregación. */
    private OrderResponse confirmada(Instant orderDate, String total) {
        return new OrderResponse(
                1L, 1L, orderDate, "CONFIRMADA", "BOLETA", "Cliente", "12345678",
                new BigDecimal(total), BigDecimal.ZERO, BigDecimal.ZERO, new BigDecimal(total), List.of());
    }

    private PageResponse<OrderResponse> singlePage(List<OrderResponse> content) {
        return new PageResponse<>(content, 0, 200, content.size(), 1);
    }

    /** Orden CONFIRMADA con líneas; para R2 solo importan los items. */
    private OrderResponse orderWithItems(Instant orderDate, OrderItemResponse... items) {
        return new OrderResponse(
                1L, 1L, orderDate, "CONFIRMADA", "BOLETA", "Cliente", "12345678",
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, List.of(items));
    }

    /** Línea de pedido: subtotal = quantity * unitPrice (como lo arma pedidos). */
    private OrderItemResponse item(Long productId, String name, int qty, String unitPrice) {
        BigDecimal up = new BigDecimal(unitPrice);
        return new OrderItemResponse(null, productId, name, qty, up, up.multiply(BigDecimal.valueOf(qty)));
    }

    /** Orden para R4: importan id, userId y status. */
    private OrderResponse order(Long id, Long userId, String status) {
        return new OrderResponse(
                id, userId, Instant.parse("2024-01-10T15:00:00Z"), status, "BOLETA", "Cliente", "12345678",
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, List.of());
    }

    private PageResponse<OrderResponse> emptyPage() {
        return new PageResponse<>(List.of(), 0, 200, 0L, 0);
    }

    private Product stubProduct(Long id, String sku, String name, int stock) {
        Product p = new Product();
        p.setId(id);
        p.setSku(sku);
        p.setName(name);
        p.setStock(stock);
        return p;
    }

    private StockMovement stubMovement(Long id, Product product, MovementType type, int qty) {
        StockMovement sm = new StockMovement();
        sm.setId(id);
        sm.setProduct(product);
        sm.setType(type);
        sm.setQuantity(qty);
        sm.setReason("test reason");
        sm.setReference("REF-001");
        sm.setCreatedAt(Instant.now());
        return sm;
    }
}
