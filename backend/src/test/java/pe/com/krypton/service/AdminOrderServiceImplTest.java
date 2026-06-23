package pe.com.krypton.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pe.com.krypton.client.PedidosClient;
import pe.com.krypton.dto.request.OrderStatusUpdateRequest;
import pe.com.krypton.dto.response.OrderResponse;
import pe.com.krypton.dto.response.PageResponse;
import pe.com.krypton.model.enums.OrderStatus;
import pe.com.krypton.service.impl.AdminOrderServiceImpl;

/**
 * Unit test for AdminOrderServiceImpl — backed by Feign (PedidosClient mocked).
 * El service es un delegador delgado: NO valida transiciones (eso lo hace pedidos,
 * dueño de la orden) ni toca stock. Solo reenvía y propaga el response.
 * Strict TDD: RED → GREEN → REFACTOR.
 */
@ExtendWith(MockitoExtension.class)
class AdminOrderServiceImplTest {

    @Mock PedidosClient pedidosClient;

    AdminOrderServiceImpl service;

    private OrderResponse sampleOrder(Long id, String status) {
        return new OrderResponse(id, 3L, Instant.now(), status,
                "BOLETA", "Juan Cliente", "12345678",
                new BigDecimal("299.90"), BigDecimal.ZERO, new BigDecimal("45.75"),
                new BigDecimal("299.90"), List.of());
    }

    private PageResponse<OrderResponse> singlePage(OrderResponse order) {
        return new PageResponse<>(List.of(order), 0, 20, 1L, 1);
    }

    @org.junit.jupiter.api.BeforeEach
    void setUp() {
        service = new AdminOrderServiceImpl(pedidosClient);
    }

    // ─── getAllOrders ────────────────────────────────────────────────────────────

    @Test
    void should_delegate_to_client_and_return_page_when_getAllOrders() {
        OrderStatus status = OrderStatus.PENDIENTE;
        Instant from = Instant.parse("2026-01-01T00:00:00Z");
        Instant to = Instant.parse("2026-02-01T00:00:00Z");
        PageResponse<OrderResponse> expected = singlePage(sampleOrder(1L, "PENDIENTE"));

        when(pedidosClient.listAll(status, from, to, 0, 20)).thenReturn(expected);

        PageResponse<OrderResponse> result = service.getAllOrders(status, from, to, 0, 20);

        assertThat(result).isSameAs(expected);
        verify(pedidosClient).listAll(status, from, to, 0, 20);
    }

    @Test
    void should_pass_null_filters_when_getAllOrders_without_filters() {
        PageResponse<OrderResponse> expected = singlePage(sampleOrder(2L, "CONFIRMADA"));
        when(pedidosClient.listAll(null, null, null, 1, 50)).thenReturn(expected);

        PageResponse<OrderResponse> result = service.getAllOrders(null, null, null, 1, 50);

        assertThat(result.content()).hasSize(1);
        verify(pedidosClient).listAll(null, null, null, 1, 50);
    }

    // ─── getOrder ────────────────────────────────────────────────────────────────

    @Test
    void should_delegate_to_client_and_return_order_when_getOrder() {
        OrderResponse expected = sampleOrder(10L, "PENDIENTE");
        when(pedidosClient.getById(10L)).thenReturn(expected);

        OrderResponse result = service.getOrder(10L);

        assertThat(result).isSameAs(expected);
        verify(pedidosClient).getById(10L);
    }

    // ─── updateStatus ────────────────────────────────────────────────────────────

    @Test
    void should_delegate_status_to_client_and_return_updated_when_updateStatus() {
        OrderResponse expected = sampleOrder(2L, "CANCELADA");
        when(pedidosClient.updateStatus(eq(2L), org.mockito.ArgumentMatchers.any()))
                .thenReturn(expected);

        OrderResponse result = service.updateStatus(2L, OrderStatus.CANCELADA);

        assertThat(result).isSameAs(expected);

        ArgumentCaptor<OrderStatusUpdateRequest> captor =
                ArgumentCaptor.forClass(OrderStatusUpdateRequest.class);
        verify(pedidosClient).updateStatus(eq(2L), captor.capture());
        assertThat(captor.getValue().status()).isEqualTo(OrderStatus.CANCELADA);
    }

    @Test
    void should_propagate_exception_when_client_fails() {
        when(pedidosClient.getById(999L)).thenThrow(new RuntimeException("boom"));

        assertThatThrownBy(() -> service.getOrder(999L))
                .isInstanceOf(RuntimeException.class);
    }
}
