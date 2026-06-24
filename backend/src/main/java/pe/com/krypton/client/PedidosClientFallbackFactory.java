package pe.com.krypton.client;

import feign.FeignException;
import java.time.Instant;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;
import pe.com.krypton.dto.request.OrderStatusUpdateRequest;
import pe.com.krypton.dto.response.OrderResponse;
import pe.com.krypton.dto.response.PageResponse;
import pe.com.krypton.exception.PedidosUnavailableException;
import pe.com.krypton.model.enums.OrderStatus;

/**
 * Fallback del circuit-breaker de {@link PedidosClient}. Recibe la causa para decidir:
 * <ul>
 *   <li>Si pedidos respondió con un status HTTP real (404/422), se RE-LANZA esa FeignException
 *       para preservar el código (el GlobalExceptionHandler lo re-emite tal cual).</li>
 *   <li>Si pedidos está caído (timeout, connection refused, circuito abierto → sin status),
 *       degrada a {@link PedidosUnavailableException} → 503 elegante, sin colgar la request.</li>
 * </ul>
 */
@Component
public class PedidosClientFallbackFactory implements FallbackFactory<PedidosClient> {

    @Override
    public PedidosClient create(Throwable cause) {
        return new PedidosClient() {
            @Override
            public PageResponse<OrderResponse> listAll(OrderStatus status, Instant from, Instant to,
                                                       Integer page, Integer size) {
                return degrade();
            }

            @Override
            public OrderResponse getById(Long id) {
                return degrade();
            }

            @Override
            public OrderResponse updateStatus(Long id, OrderStatusUpdateRequest request) {
                return degrade();
            }

            private <T> T degrade() {
                if (cause instanceof FeignException fe && fe.status() > 0) {
                    throw fe; // 404/422 real de pedidos → preservar status
                }
                throw new PedidosUnavailableException(
                        "El servicio de pedidos no está disponible. Intentá de nuevo en unos minutos.");
            }
        };
    }
}
