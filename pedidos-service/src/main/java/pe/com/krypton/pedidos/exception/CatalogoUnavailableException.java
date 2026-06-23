package pe.com.krypton.pedidos.exception;

/**
 * catalogo-service no está disponible (fallback del circuit-breaker devolvió null).
 * Se mapea a 503 Service Unavailable con un mensaje limpio, sin filtrar internos.
 */
public class CatalogoUnavailableException extends RuntimeException {
    public CatalogoUnavailableException(String message) {
        super(message);
    }
}
