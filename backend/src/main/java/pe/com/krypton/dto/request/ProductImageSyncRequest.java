package pe.com.krypton.dto.request;

/**
 * Una imagen de la galería que el monolito propaga al catálogo.
 * {@code path} es el nombre de archivo (el catálogo arma la URL con su propio base-url),
 * igual que el contrato de servido del monolito.
 */
public record ProductImageSyncRequest(
        String path,
        short displayOrder,
        boolean cover) {
}
