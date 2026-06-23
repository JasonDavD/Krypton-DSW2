package pe.com.krypton.service.impl;

import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import pe.com.krypton.client.PedidosClient;
import pe.com.krypton.dto.request.OrderStatusUpdateRequest;
import pe.com.krypton.dto.response.OrderResponse;
import pe.com.krypton.dto.response.PageResponse;
import pe.com.krypton.model.enums.OrderStatus;
import pe.com.krypton.service.AdminOrderService;

/**
 * Implementación del admin de órdenes respaldada por Feign.
 * Delegador delgado: cada método reenvía a {@link PedidosClient} y propaga el response.
 * Los errores HTTP de pedidos (404/422) viajan como {@code FeignException} y los re-emite
 * el {@code GlobalExceptionHandler}, preservando el status.
 */
@Service
@RequiredArgsConstructor
public class AdminOrderServiceImpl implements AdminOrderService {

    private final PedidosClient pedidosClient;

    @Override
    public PageResponse<OrderResponse> getAllOrders(OrderStatus status, Instant from, Instant to, int page, int size) {
        return pedidosClient.listAll(status, from, to, page, size);
    }

    @Override
    public OrderResponse getOrder(Long id) {
        return pedidosClient.getById(id);
    }

    @Override
    public OrderResponse updateStatus(Long id, OrderStatus newStatus) {
        return pedidosClient.updateStatus(id, new OrderStatusUpdateRequest(newStatus));
    }
}
