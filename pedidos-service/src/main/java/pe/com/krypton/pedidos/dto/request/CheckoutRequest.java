package pe.com.krypton.pedidos.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.List;
import pe.com.krypton.pedidos.model.enums.DocumentType;

/**
 * Cuerpo del checkout. A diferencia del monolito (que lee el carrito server-side),
 * este micro recibe las líneas en el request y las REPRECIA vía Feign (el cliente no
 * puede falsear el precio). El total NO viene del cliente: lo calcula el backend
 * (subtotal + envío, IGV desglosado).
 *
 * La regla condicional (BOLETA → DNI 8 díg, FACTURA → RUC 11 díg) NO se valida aquí;
 * bean validation sólo cubre la forma genérica del documento (8 u 11 dígitos).
 */
public record CheckoutRequest(
        @NotEmpty(message = "El pedido debe tener al menos un ítem")
        @Valid
        List<CheckoutItem> items,

        @NotNull DocumentType documentType,

        @NotBlank @Size(max = 150) String customerName,

        @NotBlank
        @Pattern(regexp = "\\d{8}|\\d{11}",
                message = "El documento debe tener 8 dígitos (DNI) u 11 (RUC)")
        String customerDoc) {

    /** Línea solicitada por el cliente: sólo producto + cantidad. El precio lo pone el catálogo. */
    public record CheckoutItem(
            @NotNull Long productId,
            @Min(value = 1, message = "La cantidad mínima es 1") int quantity) {
    }
}
