package pe.com.krypton.pedidos.model.enums;

/**
 * Tipo de comprobante de pago. BOLETA para consumidor final (DNI); FACTURA para
 * cliente con RUC. Copiado del monolito para mantener el mismo contrato de wire.
 */
public enum DocumentType {
    BOLETA,
    FACTURA
}
