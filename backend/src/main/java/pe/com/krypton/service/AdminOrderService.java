package pe.com.krypton.service;

import java.time.Instant;
import pe.com.krypton.dto.response.OrderResponse;
import pe.com.krypton.dto.response.PageResponse;
import pe.com.krypton.model.enums.OrderStatus;

/**
 * Operaciones de administración de órdenes. Tras la migración a microservicios, los datos
 * NO viven en el monolito: esta capa delega en {@code pedidos-service} vía Feign.
 * El monolito solo reenvía; la validación de transiciones y el stock son del micro pedidos.
 */
public interface AdminOrderService {

    /**
     * Lista paginada de órdenes con filtros opcionales (estado, rango de fecha [from, to)).
     * Delega en {@code pedidos-service}; orden por orderDate DESC lo aplica el micro.
     */
    PageResponse<OrderResponse> getAllOrders(OrderStatus status, Instant from, Instant to, int page, int size);

    /** Detalle de una orden por id. Propaga 404 si pedidos no la encuentra. */
    OrderResponse getOrder(Long id);

    /**
     * Cambia el estado de una orden. La validación de la máquina de estados la hace
     * pedidos (dueño de la orden); el monolito solo delega. Propaga 422 (transición
     * ilegal) y 404 (no existe) tal como los devuelve el micro.
     */
    OrderResponse updateStatus(Long id, OrderStatus newStatus);
}
