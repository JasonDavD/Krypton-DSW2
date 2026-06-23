package pe.com.krypton.pedidos.controller;

import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import pe.com.krypton.pedidos.dto.request.CheckoutRequest;
import pe.com.krypton.pedidos.dto.request.PaymentRequest;
import pe.com.krypton.pedidos.dto.response.OrderResponse;
import pe.com.krypton.pedidos.service.OrderService;

/**
 * Endpoints de pedidos del cliente — mismo path/contrato que el monolito: /api/orders.
 *
 * Autenticación simplificada: este micro NO valida JWT. El id del usuario llega en el
 * header {@code X-User-Id} que inyectará el API Gateway tras validar el token.
 * TODO: validate JWT / gateway injects X-User-Id.
 */
@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    /** POST /api/orders/checkout → 201 OrderResponse */
    @PostMapping("/checkout")
    @ResponseStatus(HttpStatus.CREATED)
    public OrderResponse checkout(@RequestHeader("X-User-Id") Long userId,
                                  @Valid @RequestBody CheckoutRequest request) {
        return orderService.checkout(userId, request);
    }

    /** GET /api/orders → 200 List<OrderResponse> (sólo las del caller, más recientes primero) */
    @GetMapping
    public List<OrderResponse> getMyOrders(@RequestHeader("X-User-Id") Long userId) {
        return orderService.getMyOrders(userId);
    }

    /** GET /api/orders/{id} → 200 OrderResponse (404 si no es del usuario o no existe) */
    @GetMapping("/{id}")
    public OrderResponse getMyOrder(@RequestHeader("X-User-Id") Long userId,
                                    @PathVariable Long id) {
        return orderService.getMyOrder(userId, id);
    }

    /** POST /api/orders/{id}/pay → 200 OrderResponse (pago simulado: PENDIENTE → CONFIRMADA) */
    @PostMapping("/{id}/pay")
    public OrderResponse pay(@RequestHeader("X-User-Id") Long userId,
                             @PathVariable Long id,
                             @Valid @RequestBody PaymentRequest request) {
        return orderService.pay(userId, id, request);
    }
}
