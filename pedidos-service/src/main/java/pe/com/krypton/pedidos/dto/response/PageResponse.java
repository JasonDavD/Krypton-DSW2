package pe.com.krypton.pedidos.dto.response;

import java.util.List;

/**
 * Página genérica para listados. El shape JSON (content/page/size/totalElements/totalPages)
 * coincide con el {@code PageResponse} que el monolito espera al consumir este micro por
 * Feign — por eso es un DTO propio y NO el {@code org.springframework.data.domain.Page}
 * (que serializa con un envoltorio inestable y advertido por Spring).
 *
 * @param content       elementos de la página actual
 * @param page          índice de página (0-based)
 * @param size          tamaño de página solicitado
 * @param totalElements total de elementos en TODO el listado (no sólo la página)
 * @param totalPages    cantidad total de páginas dado {@code size}
 */
public record PageResponse<T>(
        List<T> content,
        int page,
        int size,
        long totalElements,
        int totalPages) {

    /**
     * Construye la página calculando {@code totalPages} a partir de {@code totalElements} y
     * {@code size}. Con {@code size <= 0} totalPages es 0 (evita división por cero).
     */
    public static <T> PageResponse<T> of(List<T> content, int page, int size, long totalElements) {
        int totalPages = size <= 0 ? 0 : (int) Math.ceil((double) totalElements / size);
        return new PageResponse<>(content, page, size, totalElements, totalPages);
    }
}
