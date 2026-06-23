package pe.com.krypton.pedidos.policy;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import pe.com.krypton.pedidos.exception.OrderStatusTransitionException;
import pe.com.krypton.pedidos.model.enums.OrderStatus;

/**
 * Unit test de la máquina de estados. Cubre TODAS las transiciones válidas (deben pasar)
 * y una muestra representativa de ilegales (deben lanzar OrderStatusTransitionException).
 */
class OrderStatusPolicyTest {

    private final OrderStatusPolicy policy = new OrderStatusPolicy();

    // ─── transiciones VÁLIDAS (las 5 aristas del grafo) ──────────────────────────

    @ParameterizedTest
    @CsvSource({
            "PENDIENTE,  CONFIRMADA",
            "PENDIENTE,  CANCELADA",
            "CONFIRMADA, ENVIADO",
            "CONFIRMADA, CANCELADA",
            "ENVIADO,    ENTREGADO"
    })
    void should_not_throw_when_transition_is_legal(OrderStatus from, OrderStatus to) {
        assertThatCode(() -> policy.assertCanTransition(from, to))
                .doesNotThrowAnyException();
    }

    // ─── transiciones ILEGALES (muestra) ─────────────────────────────────────────

    @ParameterizedTest
    @CsvSource({
            "PENDIENTE,  ENVIADO",     // salto de paso
            "PENDIENTE,  ENTREGADO",   // salto de paso
            "CONFIRMADA, PENDIENTE",   // retroceso
            "CONFIRMADA, ENTREGADO",   // salto de paso
            "ENVIADO,    CANCELADA",   // no se puede cancelar un enviado
            "ENVIADO,    PENDIENTE",   // retroceso
            "ENTREGADO,  ENVIADO",     // terminal: no sale a ningún lado
            "ENTREGADO,  CANCELADA",   // terminal
            "CANCELADA,  CONFIRMADA",  // terminal
            "CANCELADA,  PENDIENTE"    // terminal
    })
    void should_throw_OrderStatusTransition_when_transition_is_illegal(OrderStatus from, OrderStatus to) {
        assertThatThrownBy(() -> policy.assertCanTransition(from, to))
                .isInstanceOf(OrderStatusTransitionException.class);
    }

    @Test
    void should_throw_when_transition_to_same_state() {
        // No hay self-loops en el grafo: PENDIENTE → PENDIENTE es ilegal.
        assertThatThrownBy(() -> policy.assertCanTransition(OrderStatus.PENDIENTE, OrderStatus.PENDIENTE))
                .isInstanceOf(OrderStatusTransitionException.class);
    }
}
