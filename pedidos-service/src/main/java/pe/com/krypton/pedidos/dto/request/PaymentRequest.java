package pe.com.krypton.pedidos.dto.request;

import jakarta.validation.constraints.NotBlank;

/**
 * Pago simulado del pedido. El monolito usa un enum PaymentMethod; aquí se acepta el
 * método como String libre (no requerido por la lógica del micro, sólo se valida que
 * venga presente) para no acoplar este servicio al catálogo de métodos del monolito.
 */
public record PaymentRequest(@NotBlank String method) {
}
