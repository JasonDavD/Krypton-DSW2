package pe.com.krypton.exception;

import static org.assertj.core.api.Assertions.assertThat;

import feign.FeignException;
import feign.Request;
import feign.RequestTemplate;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

/**
 * Unit test del mapeo de FeignException → status del micro pedidos.
 * El monolito es un proxy: 404 de pedidos sale 404, 422 sale 422, error de
 * transporte (status <= 0) cae a 502 Bad Gateway. NUNCA un 500 opaco.
 */
class GlobalExceptionHandlerFeignTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    private Request dummyRequest() {
        return Request.create(Request.HttpMethod.GET, "http://PEDIDOS/api/orders/admin/1",
                Collections.emptyMap(), null, StandardCharsets.UTF_8, new RequestTemplate());
    }

    private FeignException feignWithStatus(int status, String body) {
        return FeignException.errorStatus(
                "PedidosClient#getById(Long)",
                feign.Response.builder()
                        .status(status)
                        .reason("from pedidos")
                        .request(dummyRequest())
                        .headers(Collections.emptyMap())
                        .body(body, StandardCharsets.UTF_8)
                        .build());
    }

    @Test
    void should_propagate_404_when_pedidos_returns_not_found() {
        FeignException ex = feignWithStatus(404, "{\"status\":404,\"error\":\"Orden no encontrada: 999\"}");

        ResponseEntity<ApiError> response = handler.handleFeign(ex);

        assertThat(response.getStatusCode().value()).isEqualTo(404);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().status()).isEqualTo(404);
    }

    @Test
    void should_propagate_422_when_pedidos_returns_unprocessable() {
        FeignException ex = feignWithStatus(422, "{\"status\":422,\"error\":\"Transición inválida\"}");

        ResponseEntity<ApiError> response = handler.handleFeign(ex);

        assertThat(response.getStatusCode().value()).isEqualTo(422);
        assertThat(response.getBody().status()).isEqualTo(422);
    }

    @Test
    void should_fall_back_to_502_when_status_is_not_positive() {
        // status -1 = error de transporte (pedidos caído / DNS / timeout): no es un
        // status HTTP válido → 502 Bad Gateway, no un 500 ni un -1.
        FeignException ex = feignWithStatus(-1, null);

        ResponseEntity<ApiError> response = handler.handleFeign(ex);

        assertThat(response.getStatusCode().value()).isEqualTo(502);
        assertThat(response.getBody().status()).isEqualTo(502);
    }
}
