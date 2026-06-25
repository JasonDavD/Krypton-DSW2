package pe.com.krypton.catalogo.service;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;

import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pe.com.krypton.catalogo.dto.request.ProductImageSyncRequest;
import pe.com.krypton.catalogo.dto.request.ProductSyncRequest;
import pe.com.krypton.catalogo.repository.ProductImageRepository;
import pe.com.krypton.catalogo.repository.ProductRepository;
import pe.com.krypton.catalogo.service.impl.CatalogoSyncServiceImpl;

/** Unit test del sync: repositorios mockeados. Verifica el upsert + el reemplazo de la galería. */
@ExtendWith(MockitoExtension.class)
class CatalogoSyncServiceImplTest {

    @Mock ProductRepository productRepository;
    @Mock ProductImageRepository productImageRepository;
    @InjectMocks CatalogoSyncServiceImpl service;

    @Test
    void sync_delegates_to_upsert_with_all_fields() {
        ProductSyncRequest r = new ProductSyncRequest(
                "KR-LAP-001", "Laptop", "desc", new BigDecimal("4299.00"), 12, null, false, 1L, null);

        service.sync(5L, r);

        verify(productRepository).upsert(eq(5L), eq("KR-LAP-001"), eq("Laptop"), eq("desc"),
                eq(new BigDecimal("4299.00")), eq(12), isNull(), eq(false), eq(1L));
        // images null → solo se limpia la galería, sin inserts.
        verify(productImageRepository).deleteByProductId(5L);
    }

    @Test
    void sync_replaces_gallery_deleting_then_inserting_each_image() {
        ProductSyncRequest r = new ProductSyncRequest(
                "KR-LAP-001", "Laptop", "desc", new BigDecimal("4299.00"), 12,
                "http://gw/api/uploads/images/a.jpg", true, 1L,
                List.of(new ProductImageSyncRequest("a.jpg", (short) 0, true),
                        new ProductImageSyncRequest("b.png", (short) 1, false)));

        service.sync(5L, r);

        verify(productImageRepository).deleteByProductId(5L);
        verify(productImageRepository).insertImage(5L, "a.jpg", (short) 0, true);
        verify(productImageRepository).insertImage(5L, "b.png", (short) 1, false);
    }
}
