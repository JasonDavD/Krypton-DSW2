package pe.com.krypton.dto.request;

import java.math.BigDecimal;

/** Estado completo de un producto que el monolito empuja a catalogo-service para mantenerlo en sync. */
public record ProductSyncRequest(
        String sku,
        String name,
        String description,
        BigDecimal price,
        int stock,
        String imageUrl,
        boolean active,
        Long categoryId) {
}
