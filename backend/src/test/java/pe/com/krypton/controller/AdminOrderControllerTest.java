package pe.com.krypton.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import feign.FeignException;
import feign.Request;
import feign.RequestTemplate;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import pe.com.krypton.dto.response.OrderResponse;
import pe.com.krypton.dto.response.PageResponse;
import pe.com.krypton.security.JwtAuthenticationFilter;
import pe.com.krypton.service.AdminOrderService;

/**
 * Web slice for AdminOrderController.
 * SecurityContext disabled via addFilters=false + JwtAuthenticationFilter exclusion.
 * AdminOrderService mocked (respaldado por Feign en producción). Role via @WithMockUser.
 * Cubre el contrato HTTP: status codes, JSON shape, validación, y propagación de los
 * errores de pedidos-service (404/422 que llegan como FeignException → mismo status).
 * Satisfies REQ-OM-10..REQ-OM-13.
 */
@WebMvcTest(controllers = AdminOrderController.class,
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = JwtAuthenticationFilter.class))
@AutoConfigureMockMvc(addFilters = false)
class AdminOrderControllerTest {

    @Autowired MockMvc mvc;
    @MockBean AdminOrderService adminOrderService;

    private static final String JSON = MediaType.APPLICATION_JSON_VALUE;

    private OrderResponse sampleOrder(Long id, String status) {
        return new OrderResponse(id, 3L, Instant.now(), status,
                "BOLETA", "Juan Cliente", "12345678",
                new BigDecimal("299.90"), BigDecimal.ZERO, new BigDecimal("45.75"),
                new BigDecimal("299.90"), List.of());
    }

    private PageResponse<OrderResponse> singlePage(OrderResponse order) {
        return new PageResponse<>(List.of(order), 0, 20, 1L, 1);
    }

    /** Construye una FeignException con el status que devolvería pedidos. */
    private FeignException feignWithStatus(int status, String body) {
        Request req = Request.create(Request.HttpMethod.GET, "http://PEDIDOS/api/orders/admin",
                Collections.emptyMap(), null, StandardCharsets.UTF_8, new RequestTemplate());
        return FeignException.errorStatus("PedidosClient",
                feign.Response.builder()
                        .status(status)
                        .reason("from pedidos")
                        .request(req)
                        .headers(Collections.emptyMap())
                        .body(body, StandardCharsets.UTF_8)
                        .build());
    }

    // ─── GET /api/admin/orders ───────────────────────────────────────────────────

    @Test
    @WithMockUser(roles = "ADMIN")
    void should_return_200_page_when_get_all_orders() throws Exception {
        when(adminOrderService.getAllOrders(any(), any(), any(), anyInt(), anyInt()))
                .thenReturn(singlePage(sampleOrder(1L, "PENDIENTE")));

        mvc.perform(get("/api/admin/orders").param("page", "0").param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].userId").value(3));
    }

    // ─── GET /api/admin/orders/{id} ──────────────────────────────────────────────

    @Test
    @WithMockUser(roles = "ADMIN")
    void should_return_200_with_detail_when_get_admin_order() throws Exception {
        when(adminOrderService.getOrder(10L)).thenReturn(sampleOrder(10L, "PENDIENTE"));

        mvc.perform(get("/api/admin/orders/10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(10));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void should_propagate_404_when_pedidos_not_found() throws Exception {
        // pedidos devuelve 404 → Feign lanza FeignException(404) → el handler re-emite 404.
        when(adminOrderService.getOrder(999L))
                .thenThrow(feignWithStatus(404, "{\"status\":404,\"error\":\"Orden no encontrada: 999\"}"));

        mvc.perform(get("/api/admin/orders/999"))
                .andExpect(status().isNotFound());
    }

    // ─── PUT /api/admin/orders/{id}/status ───────────────────────────────────────

    @Test
    @WithMockUser(roles = "ADMIN")
    void should_return_200_with_updated_order_when_put_status() throws Exception {
        when(adminOrderService.updateStatus(eq(2L), any()))
                .thenReturn(sampleOrder(2L, "CANCELADA"));

        mvc.perform(put("/api/admin/orders/2/status").contentType(JSON)
                        .content("{\"status\":\"CANCELADA\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELADA"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void should_propagate_422_when_pedidos_rejects_illegal_transition() throws Exception {
        // La validación de la transición es de pedidos: rechaza con 422 → FeignException(422)
        // → el handler la re-emite como 422, sin enmascararla con un 500.
        when(adminOrderService.updateStatus(eq(2L), any()))
                .thenThrow(feignWithStatus(422,
                        "{\"status\":422,\"error\":\"Transición inválida: CANCELADA → CONFIRMADA\"}"));

        mvc.perform(put("/api/admin/orders/2/status").contentType(JSON)
                        .content("{\"status\":\"CONFIRMADA\"}"))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void should_return_400_when_put_status_missing_field() throws Exception {
        mvc.perform(put("/api/admin/orders/2/status").contentType(JSON)
                        .content("{}")) // status is @NotNull → validado en el monolito
                .andExpect(status().isBadRequest());
    }

    // ─── Auth notes ──────────────────────────────────────────────────────────────
    // 401 (no token) and 403 (CLIENTE role on admin endpoint) are verified in integration tests
    // because addFilters=false disables the JWT filter in this web slice.
}
