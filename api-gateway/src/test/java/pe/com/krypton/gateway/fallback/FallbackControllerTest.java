package pe.com.krypton.gateway.fallback;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

/** El fallback del gateway responde 503 con un mensaje claro (no un 500/timeout crudo). */
class FallbackControllerTest {

    private final FallbackController controller = new FallbackController();

    @Test
    void catalogo_returns_503_with_error_message() {
        var resp = controller.catalogo();
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
        assertThat(resp.getBody()).containsEntry("status", 503);
        assertThat((String) resp.getBody().get("error")).contains("catálogo");
    }

    @Test
    void pedidos_returns_503_with_error_message() {
        var resp = controller.pedidos();
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
        assertThat(resp.getBody()).containsEntry("status", 503);
        assertThat((String) resp.getBody().get("error")).contains("pedidos");
    }
}
