package pe.com.krypton.dto.request;

import java.math.BigDecimal;
import java.util.List;

/** Estado completo de un producto que el monolito empuja a catalogo-service para mantenerlo en sync. */
public record ProductSyncRequest(
        String sku,
        String name,
        String description,
        BigDecimal price,
        int stock,
        String imageUrl,
        boolean active,
        Long categoryId,
        List<ProductImageSyncRequest> images) {
}
