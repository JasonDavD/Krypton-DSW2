package pe.com.krypton.pedidos.dto.response;

import java.math.BigDecimal;

/**
 * Línea del pedido en la respuesta. Mismo shape que el monolito. {@code id} puede ser
 * null: los items van embebidos en el documento Order y no tienen id propio (se puede
 * usar el índice de la línea o null).
 */
public record OrderItemResponse(
        Long id,
        Long productId,
        String productName,
        int quantity,
        BigDecimal unitPrice,
        BigDecimal subtotal) {
}
