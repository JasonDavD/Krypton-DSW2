package pe.com.krypton.exception;

/**
 * El microservicio de pedidos no está disponible (caído, timeout o circuito abierto).
 * Degradación elegante: el monolito responde 503 con un mensaje claro en vez de colgarse.
 * NO se usa para 404/422 reales de pedidos: esos se preservan con su status original.
 */
public class PedidosUnavailableException extends RuntimeException {
    public PedidosUnavailableException(String message) {
        super(message);
    }
}
