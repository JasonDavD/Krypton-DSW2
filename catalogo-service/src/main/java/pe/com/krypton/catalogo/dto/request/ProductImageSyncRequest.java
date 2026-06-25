package pe.com.krypton.catalogo.dto.request;

/**
 * Una imagen de la galería que el monolito propaga. {@code path} es el nombre de archivo;
 * el catálogo arma la URL de servido con su propio base-url al exponer el detalle.
 */
public record ProductImageSyncRequest(
        String path,
        short displayOrder,
        boolean cover) {
}
