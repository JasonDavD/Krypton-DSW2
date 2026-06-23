package pe.com.krypton.catalogo.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.math.BigDecimal;
import java.util.List;

/**
 * DTO de salida del producto. Contrato IDÉNTICO al del monolito (mismos campos,
 * mismos tipos) para no romper al frontend ni al futuro cliente Feign de pedidos.
 *
 * Nota microservicios: `categoryName` se mantiene en el contrato pero SIEMPRE
 * viaja null desde este servicio, porque la entidad Category vive en otro
 * servicio. Quien necesite el nombre lo resuelve aparte.
 */
public record ProductResponse(
        Long id,
        String sku,
        String name,
        String description,
        BigDecimal price,
        int stock,
        String imageUrl,
        boolean active,
        Long categoryId,
        String categoryName,
        @JsonInclude(JsonInclude.Include.NON_NULL) List<ProductImageResponse> images) {
}
