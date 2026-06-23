package pe.com.krypton.catalogo.dto.response;

/**
 * DTO de solo lectura para una imagen de producto.
 * url es la URL completa de servido (baseUrl + /api/uploads/images/{filename}).
 */
public record ProductImageResponse(
        Long id,
        String url,
        short displayOrder,
        boolean cover) {
}
