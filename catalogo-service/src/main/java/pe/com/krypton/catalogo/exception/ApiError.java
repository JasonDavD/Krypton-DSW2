package pe.com.krypton.catalogo.exception;

/** Cuerpo JSON estándar para errores de la API. */
public record ApiError(int status, String error) {
}
