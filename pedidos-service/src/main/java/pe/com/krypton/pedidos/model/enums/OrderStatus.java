package pe.com.krypton.pedidos.model.enums;

/** Estado del pedido. Copiado del monolito para mantener el mismo contrato de wire. */
public enum OrderStatus {
    PENDIENTE,
    CONFIRMADA,
    ENVIADO,
    ENTREGADO,
    CANCELADA
}
