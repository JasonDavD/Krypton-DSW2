package pe.com.krypton.client;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pe.com.krypton.dto.request.ProductSyncRequest;
import pe.com.krypton.model.Category;
import pe.com.krypton.model.Product;

/**
 * Unit test del publisher de sync. Feign mockeado. Verifica el envío y, sobre todo, el contrato
 * BEST-EFFORT: si el catálogo está caído, el publisher NO propaga la excepción.
 */
@ExtendWith(MockitoExtension.class)
class CatalogoSyncPublisherTest {

    @Mock CatalogoSyncClient client;
    @InjectMocks CatalogoSyncPublisher publisher;

    private Product product(Long id, boolean active) {
        Product p = new Product();
        p.setId(id);
        p.setSku("KR-LAP-001");
        p.setName("Laptop");
        p.setPrice(new BigDecimal("4299.00"));
        p.setStock(12);
        p.setActive(active);
        Category c = new Category();
        c.setId(1L);
        p.setCategory(c);
        return p;
    }

    @Test
    void publish_sends_product_to_catalogo() {
        publisher.publish(product(5L, false));

        verify(client).sync(eq(5L), any(ProductSyncRequest.class));
    }

    @Test
    void publish_swallows_failure_when_catalogo_down() {
        doThrow(new RuntimeException("catalogo caido")).when(client).sync(any(), any());

        assertThatCode(() -> publisher.publish(product(5L, false))).doesNotThrowAnyException();
        verify(client).sync(any(), any());
    }
}
