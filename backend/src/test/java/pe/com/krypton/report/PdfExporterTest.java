package pe.com.krypton.report;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import pe.com.krypton.dto.response.OrderItemResponse;
import pe.com.krypton.dto.response.OrderResponse;
import pe.com.krypton.dto.response.report.KardexMovimientoRow;
import pe.com.krypton.dto.response.report.KardexReport;
import pe.com.krypton.dto.response.report.OrdenesListadoReport;
import pe.com.krypton.dto.response.report.TopProductoRow;
import pe.com.krypton.dto.response.report.TopProductosReport;
import pe.com.krypton.dto.response.report.VentasPeriodoRow;
import pe.com.krypton.dto.response.report.VentasPorPeriodoReport;

/**
 * Pure-Java unit tests for PdfExporter. No Spring context.
 * Asserts %PDF magic bytes (0x25 0x50 0x44 0x46) + non-empty output.
 * TDD: RED before PdfExporter implementation exists.
 */
class PdfExporterTest {

    private static final byte[] PDF_MAGIC = new byte[]{ 0x25, 0x50, 0x44, 0x46 }; // %PDF

    PdfExporter exporter;

    @BeforeEach
    void setUp() {
        exporter = new PdfExporter();
    }

    // ─── exportVentas ────────────────────────────────────────────────────────────

    @Test
    void exportVentas_nonempty_produces_pdf_magic() {
        VentasPorPeriodoReport report = ventasReport(List.of(
                new VentasPeriodoRow(LocalDate.of(2024, 1, 1), 3L, new BigDecimal("150.00"))
        ));

        byte[] bytes = exporter.exportVentas(report);

        assertPdf(bytes);
    }

    @Test
    void exportVentas_empty_still_valid_pdf() {
        byte[] bytes = exporter.exportVentas(ventasReport(List.of()));
        assertPdf(bytes);
    }

    // ─── exportTopProductos ───────────────────────────────────────────────────────

    @Test
    void exportTopProductos_nonempty_produces_pdf_magic() {
        TopProductosReport report = topReport(List.of(
                new TopProductoRow(1L, "SKU-A", "Prod A", 100L, new BigDecimal("500.00"))
        ));

        byte[] bytes = exporter.exportTopProductos(report);

        assertPdf(bytes);
    }

    @Test
    void exportTopProductos_empty_still_valid_pdf() {
        byte[] bytes = exporter.exportTopProductos(topReport(List.of()));
        assertPdf(bytes);
    }

    // ─── exportKardex ─────────────────────────────────────────────────────────────

    @Test
    void exportKardex_nonempty_produces_pdf_magic() {
        KardexReport report = kardexReport(List.of(
                new KardexMovimientoRow(Instant.now(), "ENTRADA", 20, "Compra", "PO-001")
        ));

        byte[] bytes = exporter.exportKardex(report);

        assertPdf(bytes);
    }

    @Test
    void exportKardex_empty_still_valid_pdf() {
        byte[] bytes = exporter.exportKardex(kardexReport(List.of()));
        assertPdf(bytes);
    }

    // ─── exportOrdenes ────────────────────────────────────────────────────────────

    @Test
    void exportOrdenes_nonempty_produces_pdf_magic() {
        OrderResponse order = new OrderResponse(
                1L, 10L, Instant.now(), "CONFIRMADA",
                "BOLETA", "Juan Cliente", "12345678",
                new BigDecimal("99.00"), BigDecimal.ZERO, new BigDecimal("15.10"),
                new BigDecimal("99.00"), List.of());
        OrdenesListadoReport report = new OrdenesListadoReport(
                "CONFIRMADA", null, null, null, 1L, List.of(order));

        byte[] bytes = exporter.exportOrdenes(report);

        assertPdf(bytes);
    }

    @Test
    void exportOrdenes_empty_still_valid_pdf() {
        OrdenesListadoReport report = new OrdenesListadoReport(
                null, null, null, null, 0L, List.of());

        byte[] bytes = exporter.exportOrdenes(report);

        assertPdf(bytes);
    }

    // ─── exportComprobante (boleta/factura individual del cliente) ─────────────────

    @Test
    void exportComprobante_boleta_produces_pdf_magic() {
        OrderResponse order = new OrderResponse(
                5L, 10L, Instant.now(), "CONFIRMADA",
                "BOLETA", "Juan Cliente", "12345678",
                new BigDecimal("299.90"), BigDecimal.ZERO, new BigDecimal("45.75"),
                new BigDecimal("299.90"),
                List.of(new OrderItemResponse(1L, 7L, "Laptop Krypton", 1,
                        new BigDecimal("299.90"), new BigDecimal("299.90"))));

        byte[] bytes = exporter.exportComprobante(order);

        assertPdf(bytes);
    }

    @Test
    void exportComprobante_factura_multiple_items_produces_pdf_magic() {
        OrderResponse order = new OrderResponse(
                6L, 11L, Instant.now(), "CONFIRMADA",
                "FACTURA", "Empresa SAC", "20123456789",
                new BigDecimal("500.00"), new BigDecimal("20.00"), new BigDecimal("79.32"),
                new BigDecimal("520.00"),
                List.of(
                        new OrderItemResponse(1L, 7L, "Monitor 27", 2,
                                new BigDecimal("200.00"), new BigDecimal("400.00")),
                        new OrderItemResponse(2L, 8L, "Teclado mecánico", 1,
                                new BigDecimal("100.00"), new BigDecimal("100.00"))));

        byte[] bytes = exporter.exportComprobante(order);

        assertPdf(bytes);
    }

    // ─── helper ───────────────────────────────────────────────────────────────────

    private void assertPdf(byte[] bytes) {
        assertThat(bytes).isNotNull();
        assertThat(bytes.length).isGreaterThan(4);
        // %PDF magic
        assertThat(bytes[0]).isEqualTo(PDF_MAGIC[0]);
        assertThat(bytes[1]).isEqualTo(PDF_MAGIC[1]);
        assertThat(bytes[2]).isEqualTo(PDF_MAGIC[2]);
        assertThat(bytes[3]).isEqualTo(PDF_MAGIC[3]);
    }

    private VentasPorPeriodoReport ventasReport(List<VentasPeriodoRow> filas) {
        return new VentasPorPeriodoReport(
                Instant.parse("2024-01-01T05:00:00Z"),
                Instant.parse("2024-02-01T05:00:00Z"),
                "dia",
                filas.stream().mapToLong(VentasPeriodoRow::ordenes).sum(),
                filas.stream().map(VentasPeriodoRow::monto).reduce(BigDecimal.ZERO, BigDecimal::add),
                BigDecimal.ZERO,
                filas);
    }

    private TopProductosReport topReport(List<TopProductoRow> productos) {
        return new TopProductosReport(null, null, 10, productos);
    }

    private KardexReport kardexReport(List<KardexMovimientoRow> movs) {
        return new KardexReport(1L, "SKU-001", "Producto A", 50, null, null, movs);
    }
}
