package pe.com.krypton.catalogo.service;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pe.com.krypton.catalogo.dto.request.ProductSyncRequest;
import pe.com.krypton.catalogo.repository.ProductRepository;
import pe.com.krypton.catalogo.service.impl.CatalogoSyncServiceImpl;

/** Unit test del sync: repositorio mockeado. Verifica que se delega al upsert con todos los campos. */
@ExtendWith(MockitoExtension.class)
class CatalogoSyncServiceImplTest {

    @Mock ProductRepository productRepository;
    @InjectMocks CatalogoSyncServiceImpl service;

    @Test
    void sync_delegates_to_upsert_with_all_fields() {
        ProductSyncRequest r = new ProductSyncRequest(
                "KR-LAP-001", "Laptop", "desc", new BigDecimal("4299.00"), 12, null, false, 1L);

        service.sync(5L, r);

        verify(productRepository).upsert(eq(5L), eq("KR-LAP-001"), eq("Laptop"), eq("desc"),
                eq(new BigDecimal("4299.00")), eq(12), isNull(), eq(false), eq(1L));
    }
}
