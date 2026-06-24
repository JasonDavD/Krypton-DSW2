package pe.com.krypton.service;

/** Genera el comprobante (boleta/factura) en PDF de un pedido propio del cliente. */
public interface ComprobanteService {

    /**
     * PDF del comprobante de un pedido del usuario autenticado.
     *
     * @param email   email del cliente (del JWT)
     * @param orderId id del pedido (la orden vive en pedidos-service)
     * @return bytes del PDF (boleta o factura según el comprobante del pedido)
     * @throws pe.com.krypton.exception.ResourceNotFoundException si el usuario no existe, o si el
     *         pedido no existe o no pertenece al usuario (IDOR → 404, sin filtrar existencia ajena)
     */
    byte[] generarComprobante(String email, Long orderId);
}
