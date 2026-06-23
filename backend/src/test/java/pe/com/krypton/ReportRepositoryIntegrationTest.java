package pe.com.krypton;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import pe.com.krypton.model.Category;
import pe.com.krypton.model.Product;
import pe.com.krypton.model.StockMovement;
import pe.com.krypton.model.enums.MovementType;
import pe.com.krypton.repository.CategoryRepository;
import pe.com.krypton.repository.ProductRepository;
import pe.com.krypton.repository.StockMovementRepository;

/**
 * Integration tests for the kardex (R3) repository queries against real Postgres.
 * Extends AbstractIntegrationTest (singleton Testcontainers Postgres 16).
 *
 * <p>Prueba las queries de {@code stock_movement}: ventana half-open de instantes y el
 * historial completo ordenado. R1/R2/R4 ya NO se prueban acá: esos reportes se migraron a
 * pedidos-service (Feign) y se agregan en memoria — su cobertura vive en
 * {@code ReportServiceImplTest}. Las queries nativas/JPQL de ventas y top-productos y la
 * {@code OrderSpecification} fueron eliminadas (código muerto tras la migración Fase 2).
 *
 * <p>Lima es UTC-5 (sin DST). Prefijos de datos de test: "IT-RPT-" (SKU), "IT-Rpt-" (categoría).
 */
class ReportRepositoryIntegrationTest extends AbstractIntegrationTest {

    private static final ZoneId LIMA = ZoneId.of("America/Lima");

    private static final LocalDate D1 = LocalDate.of(2024, 3, 1);
    private static final LocalDate D2 = LocalDate.of(2024, 3, 2);
    // D1 en Lima = 2024-03-01T05:00:00Z, D2 = 2024-03-02T05:00:00Z
    private static final Instant D1_LIMA_START = D1.atStartOfDay(LIMA).toInstant();
    private static final Instant D2_LIMA_START = D2.atStartOfDay(LIMA).toInstant();

    @Autowired CategoryRepository categoryRepository;
    @Autowired ProductRepository productRepository;
    @Autowired StockMovementRepository stockMovementRepository;

    private Category category;
    private Product product1;

    @BeforeEach
    void seed() {
        category = new Category();
        category.setName("IT-Rpt-Cat-" + System.nanoTime());
        category = categoryRepository.save(category);

        product1 = new Product();
        product1.setSku("IT-RPT-P1-" + System.nanoTime());
        product1.setName("Prod Rpt 1");
        product1.setPrice(new BigDecimal("100.00"));
        product1.setStock(50);
        product1.setActive(true);
        product1.setCategory(category);
        product1 = productRepository.save(product1);

        // Movimiento DENTRO de la ventana [D1_LIMA_START, D2_LIMA_START)
        StockMovement mvInside = new StockMovement();
        mvInside.setProduct(product1);
        mvInside.setType(MovementType.ENTRADA);
        mvInside.setQuantity(10);
        mvInside.setReason("compra");
        mvInside.setCreatedAt(Instant.parse("2024-03-01T08:00:00Z"));
        stockMovementRepository.save(mvInside);

        // Movimiento FUERA de la ventana (antes de D1)
        StockMovement mvOutside = new StockMovement();
        mvOutside.setProduct(product1);
        mvOutside.setType(MovementType.SALIDA);
        mvOutside.setQuantity(5);
        mvOutside.setReason("venta");
        mvOutside.setCreatedAt(Instant.parse("2024-02-28T10:00:00Z"));
        stockMovementRepository.save(mvOutside);
    }

    @AfterEach
    void cleanup() {
        stockMovementRepository.deleteAll(
                stockMovementRepository.findByProduct_IdOrderByCreatedAtAsc(product1.getId()));
        productRepository.delete(product1);
        categoryRepository.delete(category);
    }

    // ---- R3: kardex / stock movements ---------------------------------------

    @Test
    void r3_findByProduct_IdAndCreatedAtBetween_returns_only_movements_in_window() {
        List<StockMovement> inside = stockMovementRepository
                .findByProduct_IdAndCreatedAtBetweenOrderByCreatedAtAsc(
                        product1.getId(), D1_LIMA_START, D2_LIMA_START);

        assertThat(inside).hasSize(1);
        assertThat(inside.get(0).getType()).isEqualTo(MovementType.ENTRADA);
        assertThat(inside.get(0).getQuantity()).isEqualTo(10);
    }

    @Test
    void r3_findByProduct_IdOrderByCreatedAtAsc_returns_all_movements() {
        List<StockMovement> all = stockMovementRepository
                .findByProduct_IdOrderByCreatedAtAsc(product1.getId());

        assertThat(all).hasSize(2);
        // Ordenados por created_at ASC: fuera (Feb 28) primero, dentro (Mar 1) después
        assertThat(all.get(0).getType()).isEqualTo(MovementType.SALIDA);
        assertThat(all.get(1).getType()).isEqualTo(MovementType.ENTRADA);
    }
}
