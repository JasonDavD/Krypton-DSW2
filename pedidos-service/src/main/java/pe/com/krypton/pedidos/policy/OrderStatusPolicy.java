package pe.com.krypton.pedidos.policy;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Component;
import pe.com.krypton.pedidos.exception.OrderStatusTransitionException;
import pe.com.krypton.pedidos.model.enums.OrderStatus;

/**
 * Máquina de estados del ciclo de vida de una orden. Centraliza qué transiciones son
 * legales para que tanto el flujo de cliente como el de admin compartan la MISMA regla.
 *
 * Grafo de transiciones válidas:
 * <pre>
 *   PENDIENTE  → {CONFIRMADA, CANCELADA}
 *   CONFIRMADA → {ENVIADO, CANCELADA}
 *   ENVIADO    → {ENTREGADO}
 *   ENTREGADO  → {}   (terminal)
 *   CANCELADA  → {}   (terminal)
 * </pre>
 */
@Component
public class OrderStatusPolicy {

    /** Transiciones permitidas por estado de origen. Estados terminales mapean a conjunto vacío. */
    private static final Map<OrderStatus, Set<OrderStatus>> ALLOWED = new EnumMap<>(OrderStatus.class);

    static {
        ALLOWED.put(OrderStatus.PENDIENTE, EnumSet.of(OrderStatus.CONFIRMADA, OrderStatus.CANCELADA));
        ALLOWED.put(OrderStatus.CONFIRMADA, EnumSet.of(OrderStatus.ENVIADO, OrderStatus.CANCELADA));
        ALLOWED.put(OrderStatus.ENVIADO, EnumSet.of(OrderStatus.ENTREGADO));
        ALLOWED.put(OrderStatus.ENTREGADO, EnumSet.noneOf(OrderStatus.class));
        ALLOWED.put(OrderStatus.CANCELADA, EnumSet.noneOf(OrderStatus.class));
    }

    /**
     * Valida que {@code from → to} sea una transición legal. No-op si lo es.
     *
     * @throws OrderStatusTransitionException si la transición no está permitida (→ 422).
     */
    public void assertCanTransition(OrderStatus from, OrderStatus to) {
        Set<OrderStatus> allowedTargets = ALLOWED.getOrDefault(from, EnumSet.noneOf(OrderStatus.class));
        if (!allowedTargets.contains(to)) {
            throw new OrderStatusTransitionException(
                    "Transición de estado inválida: " + from + " → " + to);
        }
    }
}
