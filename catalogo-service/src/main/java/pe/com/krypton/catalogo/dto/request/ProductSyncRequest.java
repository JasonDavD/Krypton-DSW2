package pe.com.krypton.catalogo.dto.request;

import java.math.BigDecimal;
import java.util.List;

/**
 * Datos completos de un producto que el MONOLITO propaga al catálogo para mantenerlo en sync.
 * A diferencia de {@link ProductRequest}, incluye {@code active} (el campo que más divergía):
 * el monolito es la fuente de verdad mutable y empuja el estado completo tras cada cambio.
 * Incluye la galería completa ({@code images}) — el catálogo reemplaza la suya con esta.
 */
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
