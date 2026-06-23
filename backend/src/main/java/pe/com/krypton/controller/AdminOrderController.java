package pe.com.krypton.controller;

import jakarta.validation.Valid;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import pe.com.krypton.dto.request.OrderStatusUpdateRequest;
import pe.com.krypton.dto.response.OrderResponse;
import pe.com.krypton.dto.response.PageResponse;
import pe.com.krypton.model.enums.OrderStatus;
import pe.com.krypton.service.AdminOrderService;

/**
 * Admin order management endpoints.
 * Authorization: /api/admin/** → hasRole("ADMIN") enforced by SecurityConfig.
 * No @AuthenticationPrincipal needed — admin context is not ownership-scoped.
 * Los datos vienen de {@code pedidos-service} vía Feign (AdminOrderService), ya no de MySQL.
 * Satisfies REQ-OM-10..REQ-OM-12, REQ-OM-13.
 */
@RestController
@RequestMapping("/api/admin/orders")
@RequiredArgsConstructor
public class AdminOrderController {

    private final AdminOrderService adminOrderService;

    /**
     * GET /api/admin/orders → 200 PageResponse<OrderResponse> (paginado).
     * Filtros opcionales: {@code status}, y rango de fecha [from, to) en ISO-8601.
     * Paginación: {@code page} (default 0) y {@code size} (default 20), igual que el micro pedidos.
     */
    @GetMapping
    public PageResponse<OrderResponse> getAllOrders(
            @RequestParam(required = false) OrderStatus status,
            @RequestParam(required = false) Instant from,
            @RequestParam(required = false) Instant to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return adminOrderService.getAllOrders(status, from, to, page, size);
    }

    /** GET /api/admin/orders/{id} → 200 OrderResponse (any user's order, 404 if not found) */
    @GetMapping("/{id}")
    public OrderResponse getOrder(@PathVariable Long id) {
        return adminOrderService.getOrder(id);
    }

    /** PUT /api/admin/orders/{id}/status → 200 OrderResponse (transición validada por pedidos; 422 si es ilegal) */
    @PutMapping("/{id}/status")
    public OrderResponse updateStatus(@PathVariable Long id,
                                      @Valid @RequestBody OrderStatusUpdateRequest request) {
        return adminOrderService.updateStatus(id, request.status());
    }
}
