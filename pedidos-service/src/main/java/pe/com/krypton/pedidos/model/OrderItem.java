package pe.com.krypton.pedidos.model;

import java.math.BigDecimal;

/**
 * Línea de pedido EMBEBIDA dentro del documento Order (modelo agregado-como-documento).
 * NO es un @Document ni una colección aparte: viaja dentro de {@code orders.items}.
 *
 * {@code unitPrice} es un SNAPSHOT del precio del catálogo al momento del checkout
 * (no se recalcula después). {@code productName} también se congela para que el
 * histórico no dependa de catalogo-service.
 */
public class OrderItem {

    private Long productId;
    private String productName;
    private int quantity;
    private BigDecimal unitPrice;

    public OrderItem() {
    }

    public OrderItem(Long productId, String productName, int quantity, BigDecimal unitPrice) {
        this.productId = productId;
        this.productName = productName;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
    }

    public Long getProductId() {
        return productId;
    }

    public void setProductId(Long productId) {
        this.productId = productId;
    }

    public String getProductName() {
        return productName;
    }

    public void setProductName(String productName) {
        this.productName = productName;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public BigDecimal getUnitPrice() {
        return unitPrice;
    }

    public void setUnitPrice(BigDecimal unitPrice) {
        this.unitPrice = unitPrice;
    }
}
