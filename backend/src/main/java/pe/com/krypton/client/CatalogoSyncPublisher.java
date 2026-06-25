package pe.com.krypton.client;

import java.util.Comparator;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import pe.com.krypton.dto.request.ProductImageSyncRequest;
import pe.com.krypton.dto.request.ProductSyncRequest;
import pe.com.krypton.model.Product;
import pe.com.krypton.model.ProductImage;
import pe.com.krypton.repository.ProductImageRepository;

/**
 * Propaga el estado de un producto a catalogo-service (Feign) tras un cambio del admin.
 * Empuja el estado completo: campos del producto + la galería de imágenes (para que el
 * storefront pueda mostrar el carrusel, no solo la portada).
 * BEST-EFFORT: si el catálogo no responde, loguea y NO rompe la operación del monolito —
 * la fuente de verdad ya guardó; el catálogo se re-alinea en el próximo cambio del producto.
 */
@Component
public class CatalogoSyncPublisher {

    private static final Logger log = LoggerFactory.getLogger(CatalogoSyncPublisher.class);

    private final CatalogoSyncClient client;
    private final ProductImageRepository productImageRepository;

    public CatalogoSyncPublisher(CatalogoSyncClient client,
                                 ProductImageRepository productImageRepository) {
        this.client = client;
        this.productImageRepository = productImageRepository;
    }

    /** Empuja el producto (con su galería) al catálogo. Tolera fallos del micro (no propaga la excepción). */
    public void publish(Product p) {
        try {
            client.sync(p.getId(), new ProductSyncRequest(
                    p.getSku(), p.getName(), p.getDescription(), p.getPrice(),
                    p.getStock(), p.getImageUrl(), p.isActive(), p.getCategoryId(),
                    galleryOf(p.getId())));
        } catch (Exception e) {
            log.warn("No se pudo sincronizar el producto {} al catalogo (best-effort): {}",
                    p.getId(), e.getMessage());
        }
    }

    /** Galería del producto ordenada por displayOrder, id — el catálogo reemplaza la suya con esta. */
    private List<ProductImageSyncRequest> galleryOf(Long productId) {
        return productImageRepository.findByProductId(productId).stream()
                .sorted(Comparator.comparingInt((ProductImage i) -> i.getDisplayOrder())
                        .thenComparing(ProductImage::getId))
                .map(i -> new ProductImageSyncRequest(i.getPath(), i.getDisplayOrder(), i.isCover()))
                .toList();
    }
}
