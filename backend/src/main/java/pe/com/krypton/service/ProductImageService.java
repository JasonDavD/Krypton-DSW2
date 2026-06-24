package pe.com.krypton.service;

import java.util.List;
import org.springframework.web.multipart.MultipartFile;
import pe.com.krypton.dto.response.ProductImageResponse;

/** Manages the image gallery for a product. All methods are @Transactional on the impl. */
public interface ProductImageService {

    /**
     * Lista las imágenes del producto en orden de galería (displayOrder, luego id).
     * Es la fuente de verdad del ADMIN: las imágenes viven en el monolito (no en
     * catalogo-service), por eso el panel admin debe leerlas desde acá.
     */
    List<ProductImageResponse> list(Long productId);

    /**
     * Uploads an image and attaches it to the product.
     * Validation order: content-type → size → product-exists → max-count → store.
     * The first uploaded image for a product is automatically set as cover.
     */
    void upload(Long productId, MultipartFile file);

    /**
     * Deletes an image by ID. Cover-promotion algorithm:
     * - non-cover deleted → others untouched
     * - cover deleted + others exist → promote next by lowest display_order
     * - last image → products.image_url = null
     */
    void delete(Long productId, Long imageId);

    /**
     * Reorders images. STRICT + COMPLETE: the provided IDs must exactly match
     * the product's current image IDs (no foreign, no missing). Throws
     * IllegalArgumentException (→ 400) otherwise.
     */
    void reorder(Long productId, List<Long> orderedIds);

    /**
     * Sets the cover image. Demotes the current cover, promotes the target.
     * Syncs products.image_url. Idempotent if target is already cover.
     */
    void setCover(Long productId, Long imageId);
}
