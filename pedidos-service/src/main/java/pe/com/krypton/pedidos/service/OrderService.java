package pe.com.krypton.pedidos.service;

import java.time.Instant;
import java.util.List;
import pe.com.krypton.pedidos.dto.request.CheckoutRequest;
import pe.com.krypton.pedidos.dto.request.PaymentRequest;
import pe.com.krypton.pedidos.dto.response.OrderResponse;
import pe.com.krypton.pedidos.dto.response.PageResponse;
import pe.com.krypton.pedidos.model.enums.OrderStatus;

/**
 * Casos de uso de pedidos del cliente. El {@code userId} llega ya resuelto desde el
 * header X-User-Id (el gateway lo inyecta; este micro NO valida JWT por ahora).
 */
public interface OrderService {

    /**
     * Checkout: reprecia cada ítem contra el catálogo (Feign), valida stock, arma el
     * documento con líneas embebidas (precio snapshot), calcula envío (gratis ≥ S/300,
     * si no S/20) y desglosa el IGV (el precio ya lo incluye). Estado inicial PENDIENTE.
     */
    OrderResponse checkout(Long userId, CheckoutRequest request);

    /** Pedidos del usuario, más recientes primero. */
    List<OrderResponse> getMyOrders(Long userId);

    /** Detalle de un pedido propio. 404 si no existe o no es del usuario (IDOR). */
    OrderResponse getMyOrder(Long userId, Long orderId);

    /**
     * Pago simulado: PENDIENTE → CONFIRMADA. 404 si IDOR;
     * OrderStatusTransitionException (422) si no estaba PENDIENTE.
     */
    OrderResponse pay(Long userId, Long orderId, PaymentRequest request);

    // ─── ADMIN (sin filtro de dueño; la auth de admin la hace el monolito aguas arriba) ───

    /**
     * Listado ADMIN de TODAS las órdenes (no filtra por usuario), más recientes primero.
     * Filtros opcionales (cualquiera puede ser null): por {@code status} y por rango de
     * fecha {@code [from, to)} (half-open, cada borde independiente). Paginado.
     */
    PageResponse<OrderResponse> listAllForAdmin(
            OrderStatus status, Instant from, Instant to, int page, int size);

    /** Detalle ADMIN por id, SIN filtro de dueño. 404 si no existe. */
    OrderResponse getByIdForAdmin(Long orderId);

    /**
     * Cambio de estado ADMIN. 404 si no existe; OrderStatusTransitionException (422) si la
     * transición actual→nuevo es ilegal (validada por OrderStatusPolicy). No toca stock.
     */
    OrderResponse updateStatusForAdmin(Long orderId, OrderStatus newStatus);
}
