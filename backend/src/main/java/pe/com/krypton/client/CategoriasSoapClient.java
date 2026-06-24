package pe.com.krypton.client;

import java.util.List;
import org.springframework.stereotype.Component;
import org.springframework.ws.client.WebServiceIOException;
import org.springframework.ws.client.core.WebServiceTemplate;
import pe.com.krypton.exception.CategoriasUnavailableException;
import pe.com.krypton.soap.ws.Categoria;
import pe.com.krypton.soap.ws.DeleteCategoriaRequest;
import pe.com.krypton.soap.ws.DeleteCategoriaResponse;
import pe.com.krypton.soap.ws.GetCategoriasRequest;
import pe.com.krypton.soap.ws.GetCategoriasResponse;
import pe.com.krypton.soap.ws.PostCategoriaRequest;
import pe.com.krypton.soap.ws.PostCategoriaResponse;
import pe.com.krypton.soap.ws.PutCategoriaRequest;
import pe.com.krypton.soap.ws.PutCategoriaResponse;

/**
 * Cliente SOAP del monolito hacia categorias-soap-service. Traduce las operaciones a mensajes
 * SOAP (marshalling JAXB). Si el transporte falla (micro caído/timeout) lanza
 * {@link CategoriasUnavailableException} → 503 (degradación elegante).
 */
@Component
public class CategoriasSoapClient {

    private final WebServiceTemplate template;

    public CategoriasSoapClient(WebServiceTemplate categoriasWebServiceTemplate) {
        this.template = categoriasWebServiceTemplate;
    }

    public List<Categoria> listar() {
        return ((GetCategoriasResponse) call(new GetCategoriasRequest())).getLista();
    }

    public long crear(Categoria categoria) {
        PostCategoriaRequest req = new PostCategoriaRequest();
        req.setCategoria(categoria);
        return ((PostCategoriaResponse) call(req)).getSalida();
    }

    public int actualizar(Categoria categoria) {
        PutCategoriaRequest req = new PutCategoriaRequest();
        req.setCategoria(categoria);
        return ((PutCategoriaResponse) call(req)).getSalida();
    }

    public int eliminar(long id) {
        DeleteCategoriaRequest req = new DeleteCategoriaRequest();
        req.setId(id);
        return ((DeleteCategoriaResponse) call(req)).getSalida();
    }

    private Object call(Object request) {
        try {
            return template.marshalSendAndReceive(request);
        } catch (WebServiceIOException e) {
            throw new CategoriasUnavailableException(
                    "El servicio de categorías no está disponible. Intentá de nuevo en unos minutos.");
        }
    }
}
