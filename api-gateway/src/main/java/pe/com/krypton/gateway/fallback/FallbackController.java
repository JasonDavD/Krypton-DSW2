package pe.com.krypton.gateway.fallback;

import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Degradación elegante en el gateway: cuando un micro está caído o su circuito está abierto,
 * el filtro {@code CircuitBreaker} de la ruta forwardea acá (fallbackUri) en lugar de devolver
 * un 500/timeout crudo. Respondemos 503 con un mensaje claro que el frontend puede mostrar.
 */
@RestController
public class FallbackController {

    @RequestMapping("/fallback/catalogo")
    public ResponseEntity<Map<String, Object>> catalogo() {
        return unavailable("El catálogo no está disponible en este momento. Intentá de nuevo en unos minutos.");
    }

    @RequestMapping("/fallback/pedidos")
    public ResponseEntity<Map<String, Object>> pedidos() {
        return unavailable("El servicio de pedidos no está disponible en este momento. Intentá de nuevo en unos minutos.");
    }

    private ResponseEntity<Map<String, Object>> unavailable(String message) {
        // Forma {status, error}: misma que ApiError del monolito, para que el frontend la lea igual.
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(Map.of("status", 503, "error", message));
    }
}
