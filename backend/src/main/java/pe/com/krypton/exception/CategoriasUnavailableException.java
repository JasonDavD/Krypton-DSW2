package pe.com.krypton.exception;

/**
 * El microservicio SOAP de categorías no está disponible (caído, timeout). Degradación elegante:
 * el monolito responde 503 con un mensaje claro en vez de un error crudo.
 */
public class CategoriasUnavailableException extends RuntimeException {
    public CategoriasUnavailableException(String message) {
        super(message);
    }
}
