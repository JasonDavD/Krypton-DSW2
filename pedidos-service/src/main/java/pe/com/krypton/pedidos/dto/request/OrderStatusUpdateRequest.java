package pe.com.krypton.pedidos.dto.request;

import jakarta.validation.constraints.NotNull;
import pe.com.krypton.pedidos.model.enums.OrderStatus;

/**
 * Cuerpo del cambio de estado admin (PUT /api/orders/admin/{id}/status). El estado destino
 * es obligatorio; la legalidad de la transición la valida {@code OrderStatusPolicy} (no aquí).
 */
public record OrderStatusUpdateRequest(@NotNull OrderStatus status) {
}
