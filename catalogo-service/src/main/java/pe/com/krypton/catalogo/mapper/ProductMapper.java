package pe.com.krypton.catalogo.mapper;

import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import pe.com.krypton.catalogo.dto.response.ProductImageResponse;
import pe.com.krypton.catalogo.dto.response.ProductResponse;
import pe.com.krypton.catalogo.model.Product;
import pe.com.krypton.catalogo.model.ProductImage;

/**
 * Traduce la entidad Product a su DTO de salida. Nunca expone la entidad fuera del servicio.
 *
 * Dos modos de mapeo:
 * - toResponse()           lean (images = null, omitido por @JsonInclude NON_NULL) — para lista/búsqueda.
 * - toResponseWithImages() full (images poblado, ordenado por displayOrder ASC, id ASC) — para getById.
 *
 * ADAPTACIÓN MICROSERVICIOS: aquí NO existe la entidad Category. Se mapea
 * `categoryId` directo desde el producto y `categoryName` viaja SIEMPRE null
 * (la entidad Category vive en otro servicio).
 *
 * El constructor recibe la base-url para que las URLs de imagen queden absolutas.
 */
@Component
public class ProductMapper {

    private final String baseUrl;

    public ProductMapper(
            @Value("${app.uploads.base-url:http://localhost:8080}") String baseUrl) {
        this.baseUrl = baseUrl;
    }

    /** Mapeo lean: el campo images es null (omitido del JSON vía @JsonInclude NON_NULL). */
    public ProductResponse toResponse(Product product) {
        return new ProductResponse(
                product.getId(),
                product.getSku(),
                product.getName(),
                product.getDescription(),
                product.getPrice(),
                product.getStock(),
                product.getImageUrl(),
                product.isActive(),
                product.getCategoryId(),
                null,                 // categoryName: Category vive en otro servicio
                null);
    }

    /**
     * Mapeo full: la colección images se carga (el caller debe estar dentro de
     * @Transactional(readOnly=true) porque la colección es LAZY). La lista ya
     * viene ordenada por el @OrderBy de Product.images.
     */
    public ProductResponse toResponseWithImages(Product product) {
        List<ProductImageResponse> imageResponses = product.getImages().stream()
                .map(this::toImageResponse)
                .toList();

        return new ProductResponse(
                product.getId(),
                product.getSku(),
                product.getName(),
                product.getDescription(),
                product.getPrice(),
                product.getStock(),
                product.getImageUrl(),
                product.isActive(),
                product.getCategoryId(),
                null,                 // categoryName: Category vive en otro servicio
                imageResponses);
    }

    // ─── private helpers ─────────────────────────────────────────────────────────

    private ProductImageResponse toImageResponse(ProductImage image) {
        String url = baseUrl + "/api/uploads/images/" + image.getPath();
        return new ProductImageResponse(
                image.getId(),
                url,
                image.getDisplayOrder(),
                image.isCover());
    }
}
