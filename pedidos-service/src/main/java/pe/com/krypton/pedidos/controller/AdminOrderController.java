package pe.com.krypton.pedidos.controller;

import jakarta.validation.Valid;
import java.time.Instant;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import pe.com.krypton.pedidos.dto.request.OrderStatusUpdateRequest;
import pe.com.krypton.pedidos.dto.response.OrderResponse;
import pe.com.krypton.pedidos.dto.response.PageResponse;
import pe.com.krypton.pedidos.model.enums.OrderStatus;
import pe.com.krypton.pedidos.service.OrderService;

/**
 * Endpoints ADMIN de órdenes, consumidos por el monolito vía Feign. NO requiere
 * {@code X-User-Id}: la autenticación/autorización de admin la resuelve el monolito
 * aguas arriba; este micro confía en quien llama.
 *
 * Ruta {@code /api/orders/admin}: cae bajo el predicate del gateway {@code /api/orders/**}
 * → PEDIDOS. El segmento {@code admin} es literal, así que NO colisiona con el
 * {@code GET /api/orders/{id}} de {@code OrderController}: ante una ruta literal y una
 * variable de path, Spring prioriza la literal.
 */
@RestController
@RequestMapping("/api/orders/admin")
public class AdminOrderController {

    private final OrderService orderService;

    public AdminOrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    /**
     * GET /api/orders/admin → 200 PageResponse&lt;OrderResponse&gt;.
     * Filtros opcionales: {@code status}, {@code from}, {@code to} (ISO-8601 Instant).
     * Paginación: {@code page} (default 0), {@code size} (default 20).
     */
    @GetMapping
    public PageResponse<OrderResponse> listAll(
            @RequestParam(required = false) OrderStatus status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return orderService.listAllForAdmin(status, from, to, page, size);
    }

    /** GET /api/orders/admin/{id} → 200 OrderResponse (404 si no existe). */
    @GetMapping("/{id}")
    public OrderResponse getById(@PathVariable Long id) {
        return orderService.getByIdForAdmin(id);
    }

    /**
     * PUT /api/orders/admin/{id}/status → 200 OrderResponse.
     * Body: {@link OrderStatusUpdateRequest}. 404 si no existe; 422 si la transición es ilegal.
     */
    @PutMapping("/{id}/status")
    public OrderResponse updateStatus(@PathVariable Long id,
                                      @Valid @RequestBody OrderStatusUpdateRequest request) {
        return orderService.updateStatusForAdmin(id, request.status());
    }
}
