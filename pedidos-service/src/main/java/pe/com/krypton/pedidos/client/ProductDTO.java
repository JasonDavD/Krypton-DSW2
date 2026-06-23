package pe.com.krypton.pedidos.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.math.BigDecimal;

/**
 * Vista mínima del producto que necesita pedidos: id, name, price, stock.
 * catalogo-service devuelve un ProductResponse más ancho; ignoramos el resto
 * de campos (sku, description, imageUrl, active, categoryId, categoryName, images)
 * con @JsonIgnoreProperties para no acoplarnos a su shape completo.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record ProductDTO(
        Long id,
        String name,
        BigDecimal price,
        int stock) {
}
