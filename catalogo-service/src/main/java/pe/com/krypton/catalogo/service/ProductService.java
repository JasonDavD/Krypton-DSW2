package pe.com.krypton.catalogo.service;

import java.math.BigDecimal;
import org.springframework.data.domain.Pageable;
import pe.com.krypton.catalogo.dto.response.PageResponse;
import pe.com.krypton.catalogo.dto.response.ProductResponse;

/**
 * Operaciones de catálogo para productos — SOLO LECTURA en este servicio.
 * Las operaciones de escritura (create/update/delete) y la gestión de stock
 * NO se portan: pertenecen al servicio de administración / monolito.
 */
public interface ProductService {

    /** Búsqueda pública con filtros opcionales. Siempre filtra active=true. */
    PageResponse<ProductResponse> search(String name, Long categoryId,
                                         BigDecimal priceMin, BigDecimal priceMax,
                                         Pageable pageable);

    /** Retorna el producto activo o lanza ResourceNotFoundException (404). */
    ProductResponse getById(Long id);
}
