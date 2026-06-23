package pe.com.krypton.pedidos.exception;

/** Stock insuficiente frente al stock reportado por el catálogo. Se mapea a 409 Conflict. */
public class InsufficientStockException extends RuntimeException {
    public InsufficientStockException(String message) {
        super(message);
    }
}
