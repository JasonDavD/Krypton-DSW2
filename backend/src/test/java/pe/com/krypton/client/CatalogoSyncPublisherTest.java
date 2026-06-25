package pe.com.krypton.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pe.com.krypton.dto.request.ProductSyncRequest;
import pe.com.krypton.model.Product;
import pe.com.krypton.model.ProductImage;
import pe.com.krypton.repository.ProductImageRepository;

/**
 * Unit test del publisher de sync. Feign + repo de imágenes mockeados. Verifica el envío del
 * producto + su galería y, sobre todo, el contrato BEST-EFFORT: si el catálogo está caído, el
 * publisher NO propaga la excepción.
 */
@ExtendWith(MockitoExtension.class)
class CatalogoSyncPublisherTest {

    @Mock CatalogoSyncClient client;
    @Mock ProductImageRepository productImageRepository;
    @InjectMocks CatalogoSyncPublisher publisher;

    private Product product(Long id, boolean active) {
        Product p = new Product();
        p.setId(id);
        p.setSku("KR-LAP-001");
        p.setName("Laptop");
        p.setPrice(new BigDecimal("4299.00"));
        p.setStock(12);
        p.setActive(active);
        p.setCategoryId(1L);
        return p;
    }

    private ProductImage image(Long id, String path, short order, boolean cover) {
        ProductImage img = new ProductImage();
        img.setId(id);
        img.setPath(path);
        img.setDisplayOrder(order);
        img.setCover(cover);
        return img;
    }

    @Test
    void publish_sends_product_to_catalogo() {
        publisher.publish(product(5L, false));

        verify(client).sync(eq(5L), any(ProductSyncRequest.class));
    }

    @Test
    void publish_sends_gallery_ordered_by_display_order() {
        // Devueltas desordenadas a propósito: el publisher las ordena por displayOrder, id.
        when(productImageRepository.findByProductId(5L)).thenReturn(List.of(
                image(20L, "b.png", (short) 1, false),
                image(10L, "a.jpg", (short) 0, true)));

        ArgumentCaptor<ProductSyncRequest> captor = ArgumentCaptor.forClass(ProductSyncRequest.class);
        publisher.publish(product(5L, true));

        verify(client).sync(eq(5L), captor.capture());
        assertThat(captor.getValue().images()).hasSize(2);
        assertThat(captor.getValue().images().get(0).path()).isEqualTo("a.jpg");   // displayOrder 0 primero
        assertThat(captor.getValue().images().get(0).cover()).isTrue();
        assertThat(captor.getValue().images().get(1).path()).isEqualTo("b.png");
    }

    @Test
    void publish_swallows_failure_when_catalogo_down() {
        doThrow(new RuntimeException("catalogo caido")).when(client).sync(any(), any());

        assertThatCode(() -> publisher.publish(product(5L, false))).doesNotThrowAnyException();
        verify(client).sync(any(), any());
    }
}
