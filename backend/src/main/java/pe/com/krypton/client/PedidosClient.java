package pe.com.krypton.client;

import java.time.Instant;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import pe.com.krypton.dto.request.OrderStatusUpdateRequest;
import pe.com.krypton.dto.response.OrderResponse;
import pe.com.krypton.dto.response.PageResponse;
import pe.com.krypton.model.enums.OrderStatus;

/**
 * Cliente Feign contra el microservicio {@code pedidos-service} (Eureka app name {@code PEDIDOS}).
 * El monolito dejó de leer la tabla MySQL {@code orders} (ahora vacía): el ADMIN de órdenes
 * delega aquí. Feign deserializa el JSON de pedidos directamente en los tipos del monolito
 * ({@link PageResponse} de {@link OrderResponse}), porque el shape coincide 1:1.
 *
 * <p>Cuando pedidos responde 404/422, Feign lanza {@code FeignException} con ese {@code status()};
 * el {@code GlobalExceptionHandler} del monolito lo re-emite preservando el código.
 */
@FeignClient(name = "PEDIDOS")
public interface PedidosClient {

    /**
     * GET /api/orders/admin — lista paginada con filtros opcionales.
     * status/from/to son opcionales; page/size tienen default en pedidos.
     */
    @GetMapping("/api/orders/admin")
    PageResponse<OrderResponse> listAll(
            @RequestParam(value = "status", required = false) OrderStatus status,
            @RequestParam(value = "from", required = false) Instant from,
            @RequestParam(value = "to", required = false) Instant to,
            @RequestParam(value = "page", required = false) Integer page,
            @RequestParam(value = "size", required = false) Integer size);

    /** GET /api/orders/admin/{id} → 200 OrderResponse · 404 si no existe. */
    @GetMapping("/api/orders/admin/{id}")
    OrderResponse getById(@PathVariable("id") Long id);

    /** PUT /api/orders/admin/{id}/status → 200 OrderResponse · 422 transición ilegal · 404 si no existe. */
    @PutMapping("/api/orders/admin/{id}/status")
    OrderResponse updateStatus(@PathVariable("id") Long id, @RequestBody OrderStatusUpdateRequest request);
}
