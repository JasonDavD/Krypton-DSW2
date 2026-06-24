package pe.com.krypton.exception;

/**
 * Se intentó emitir el comprobante (boleta/factura) de un pedido cuyo estado no lo permite:
 * un pedido PENDIENTE (sin pagar) o CANCELADA no tiene comprobante. Mapea a 422.
 */
public class ComprobanteNoDisponibleException extends RuntimeException {
    public ComprobanteNoDisponibleException(String message) {
        super(message);
    }
}
