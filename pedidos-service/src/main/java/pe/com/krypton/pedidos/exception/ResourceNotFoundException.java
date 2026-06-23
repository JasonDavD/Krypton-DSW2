package pe.com.krypton.pedidos.exception;

/** Recurso inexistente (o no perteneciente al usuario — IDOR). Se mapea a 404 Not Found. */
public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String message) {
        super(message);
    }
}
