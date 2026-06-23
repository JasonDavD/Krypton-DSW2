package pe.com.krypton.service;

import java.util.List;
import pe.com.krypton.dto.request.CheckoutRequest;
import pe.com.krypton.dto.request.PaymentRequest;
import pe.com.krypton.dto.response.OrderResponse;

public interface OrderService {

    /**
     * Atomic checkout: cart → Order (PENDIENTE), stock decrement, StockMovement, clear cart.
     * Calcula el envío (gratis ≥ S/300, si no S/20) y desglosa el IGV (el precio ya lo
     * incluye). El comprobante (boleta/factura + receptor) viene en {@code request}.
     */
    OrderResponse checkout(String email, CheckoutRequest request);

    /** Returns the authenticated client's orders ordered by date DESC. */
    List<OrderResponse> getMyOrders(String email);

    /** Returns the client's own order detail. Throws ResourceNotFoundException (404) if IDOR. */
    OrderResponse getMyOrder(String email, Long orderId);

    /**
     * Simulated payment: PENDIENTE → CONFIRMADA.
     * Throws ResourceNotFoundException (404) if IDOR.
     * Throws OrderStatusTransitionException (422) if not PENDIENTE.
     */
    OrderResponse pay(String email, Long orderId, PaymentRequest request);
}
