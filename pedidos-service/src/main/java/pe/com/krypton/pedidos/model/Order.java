package pe.com.krypton.pedidos.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import pe.com.krypton.pedidos.model.enums.DocumentType;
import pe.com.krypton.pedidos.model.enums.OrderStatus;

/**
 * Documento agregado de pedido. Mantiene un {@code Long id} (entero) porque el
 * frontend hace {@code GET /api/orders/{id}} con un número — Mongo no autoincrementa,
 * así que el id lo genera SequenceGenerator (colección db_sequences) antes de guardar.
 *
 * Las líneas (items) viajan EMBEBIDAS dentro del propio documento, no en una colección
 * aparte (aggregate-as-document). El IGV se desglosa hacia adentro del total (el precio
 * de catálogo YA lo incluye): base = total / 1.18, igv = total − base.
 */
@Document("orders")
public class Order {

    @Id
    private Long id;

    private Long userId;
    private Instant orderDate;
    private OrderStatus status;

    // ── Comprobante (receptor) ──
    private DocumentType documentType;
    private String customerName;
    private String customerDoc;

    // ── Montos (snapshot al checkout) ──
    private BigDecimal subtotal;
    private BigDecimal shippingCost;
    private BigDecimal igv;
    private BigDecimal total;

    // ── Líneas embebidas ──
    private List<OrderItem> items = new ArrayList<>();

    public Order() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Instant getOrderDate() {
        return orderDate;
    }

    public void setOrderDate(Instant orderDate) {
        this.orderDate = orderDate;
    }

    public OrderStatus getStatus() {
        return status;
    }

    public void setStatus(OrderStatus status) {
        this.status = status;
    }

    public DocumentType getDocumentType() {
        return documentType;
    }

    public void setDocumentType(DocumentType documentType) {
        this.documentType = documentType;
    }

    public String getCustomerName() {
        return customerName;
    }

    public void setCustomerName(String customerName) {
        this.customerName = customerName;
    }

    public String getCustomerDoc() {
        return customerDoc;
    }

    public void setCustomerDoc(String customerDoc) {
        this.customerDoc = customerDoc;
    }

    public BigDecimal getSubtotal() {
        return subtotal;
    }

    public void setSubtotal(BigDecimal subtotal) {
        this.subtotal = subtotal;
    }

    public BigDecimal getShippingCost() {
        return shippingCost;
    }

    public void setShippingCost(BigDecimal shippingCost) {
        this.shippingCost = shippingCost;
    }

    public BigDecimal getIgv() {
        return igv;
    }

    public void setIgv(BigDecimal igv) {
        this.igv = igv;
    }

    public BigDecimal getTotal() {
        return total;
    }

    public void setTotal(BigDecimal total) {
        this.total = total;
    }

    public List<OrderItem> getItems() {
        return items;
    }

    public void setItems(List<OrderItem> items) {
        this.items = items;
    }
}
