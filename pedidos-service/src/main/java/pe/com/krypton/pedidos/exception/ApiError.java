package pe.com.krypton.pedidos.exception;

/** Cuerpo JSON estándar para errores de la API. Mismo shape que el monolito. */
public record ApiError(int status, String error) {
}
