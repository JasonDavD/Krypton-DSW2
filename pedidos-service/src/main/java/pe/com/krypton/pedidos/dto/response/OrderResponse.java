package pe.com.krypton.pedidos.dto.response;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * Respuesta de pedido. Contrato IDÉNTICO al del monolito (mismos campos y tipos) para
 * no romper al frontend. El precio del catálogo YA trae IGV incluido, por eso `igv` se
 * desglosa hacia adentro del total (base = total − igv). `total = subtotal + shippingCost`.
 */
public record OrderResponse(
        Long id,
        Long userId,
        Instant orderDate,
        String status,
        String documentType,
        String customerName,
        String customerDoc,
        BigDecimal subtotal,
        BigDecimal shippingCost,
        BigDecimal igv,
        BigDecimal total,
        List<OrderItemResponse> items) {
}
