package pe.com.krypton.pedidos.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import pe.com.krypton.pedidos.dto.response.OrderResponse;
import pe.com.krypton.pedidos.dto.response.PageResponse;
import pe.com.krypton.pedidos.exception.OrderStatusTransitionException;
import pe.com.krypton.pedidos.exception.ResourceNotFoundException;
import pe.com.krypton.pedidos.model.enums.OrderStatus;
import pe.com.krypton.pedidos.service.OrderService;

/**
 * Web slice del controller ADMIN. El service va mockeado (@MockitoBean). Verifica el
 * binding de query params, los códigos de estado (200/404/422) y la forma del JSON que
 * el monolito consumirá por Feign.
 */
@WebMvcTest(AdminOrderController.class)
class AdminOrderControllerTest {

    @Autowired MockMvc mockMvc;

    // NOTA: el estándar pide @MockitoBean, pero esa anotación nace en Spring Framework 6.2
    // (Boot 3.4). Este micro corre en Boot 3.3.5 / Spring 6.1.14, donde NO existe; el
    // equivalente disponible es @MockBean. Migrar a @MockitoBean al subir a Boot 3.4+.
    @MockBean OrderService orderService;

    private OrderResponse sampleOrder(Long id, String status) {
        return new OrderResponse(
                id, 1L, Instant.parse("2026-01-01T00:00:00Z"), status, "BOLETA",
                "Juan Cliente", "12345678",
                new BigDecimal("100.00"), new BigDecimal("20.00"),
                new BigDecimal("18.31"), new BigDecimal("120.00"),
                List.of());
    }

    // ─── GET /api/orders/admin ───────────────────────────────────────────────────

    @Test
    void should_return_200_with_page_body_when_listing_without_filters() throws Exception {
        PageResponse<OrderResponse> page = PageResponse.of(
                List.of(sampleOrder(1L, "PENDIENTE")), 0, 20, 1L);
        when(orderService.listAllForAdmin(isNull(), isNull(), isNull(), eq(0), eq(20)))
                .thenReturn(page);

        mockMvc.perform(get("/api/orders/admin"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content[0].id").value(1))
                .andExpect(jsonPath("$.content[0].status").value("PENDIENTE"))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(20))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.totalPages").value(1));
    }

    @Test
    void should_bind_status_and_date_and_paging_query_params_when_provided() throws Exception {
        PageResponse<OrderResponse> page = PageResponse.of(
                List.of(sampleOrder(2L, "CONFIRMADA")), 1, 5, 6L);
        when(orderService.listAllForAdmin(
                eq(OrderStatus.CONFIRMADA),
                eq(Instant.parse("2026-01-01T00:00:00Z")),
                eq(Instant.parse("2026-02-01T00:00:00Z")),
                eq(1), eq(5)))
                .thenReturn(page);

        mockMvc.perform(get("/api/orders/admin")
                        .param("status", "CONFIRMADA")
                        .param("from", "2026-01-01T00:00:00Z")
                        .param("to", "2026-02-01T00:00:00Z")
                        .param("page", "1")
                        .param("size", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(2))
                .andExpect(jsonPath("$.totalElements").value(6))
                .andExpect(jsonPath("$.totalPages").value(2)); // ceil(6/5)
    }

    @Test
    void should_return_400_when_status_query_param_is_invalid() throws Exception {
        // Enum inválido → MethodArgumentTypeMismatchException → 400 (GlobalExceptionHandler).
        mockMvc.perform(get("/api/orders/admin").param("status", "NO_EXISTE"))
                .andExpect(status().isBadRequest());
    }

    // ─── GET /api/orders/admin/{id} ──────────────────────────────────────────────

    @Test
    void should_return_200_with_order_when_found_by_id() throws Exception {
        when(orderService.getByIdForAdmin(7L)).thenReturn(sampleOrder(7L, "ENVIADO"));

        mockMvc.perform(get("/api/orders/admin/7"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(7))
                .andExpect(jsonPath("$.status").value("ENVIADO"))
                .andExpect(jsonPath("$.total").value(120.00));
    }

    @Test
    void should_return_404_when_order_not_found_by_id() throws Exception {
        when(orderService.getByIdForAdmin(99L))
                .thenThrow(new ResourceNotFoundException("Orden no encontrada: 99"));

        mockMvc.perform(get("/api/orders/admin/99"))
                .andExpect(status().isNotFound());
    }

    // ─── PUT /api/orders/admin/{id}/status ───────────────────────────────────────

    @Test
    void should_return_200_with_updated_order_when_transition_is_valid() throws Exception {
        when(orderService.updateStatusForAdmin(4L, OrderStatus.ENVIADO))
                .thenReturn(sampleOrder(4L, "ENVIADO"));

        mockMvc.perform(put("/api/orders/admin/4/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"ENVIADO\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(4))
                .andExpect(jsonPath("$.status").value("ENVIADO"));
    }

    @Test
    void should_return_422_when_transition_is_illegal() throws Exception {
        when(orderService.updateStatusForAdmin(eq(6L), any(OrderStatus.class)))
                .thenThrow(new OrderStatusTransitionException("Transición de estado inválida"));

        mockMvc.perform(put("/api/orders/admin/6/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"PENDIENTE\"}"))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void should_return_404_when_updating_status_of_missing_order() throws Exception {
        when(orderService.updateStatusForAdmin(eq(50L), any(OrderStatus.class)))
                .thenThrow(new ResourceNotFoundException("Orden no encontrada: 50"));

        mockMvc.perform(put("/api/orders/admin/50/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"CONFIRMADA\"}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void should_return_400_when_status_body_is_missing() throws Exception {
        // @NotNull status ausente → MethodArgumentNotValidException → 400.
        mockMvc.perform(put("/api/orders/admin/4/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }
}
