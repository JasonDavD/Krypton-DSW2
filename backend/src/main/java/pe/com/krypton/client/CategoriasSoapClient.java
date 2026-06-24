package pe.com.krypton.client;

import java.util.List;
import pe.com.krypton.soap.ws.Categoria;

/**
 * Cliente del monolito hacia categorias-soap-service. Interfaz (impl SOAP real en
 * {@link CategoriasSoapClientImpl}) para poder mockear/fakear en tests. Ante caída del micro,
 * la impl lanza {@code CategoriasUnavailableException} → 503 (degradación elegante).
 */
public interface CategoriasSoapClient {

    List<Categoria> listar();

    long crear(Categoria categoria);

    int actualizar(Categoria categoria);

    int eliminar(long id);
}
