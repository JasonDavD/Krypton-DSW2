package pe.com.krypton.categorias.endpoint;

import org.springframework.ws.server.endpoint.annotation.Endpoint;
import org.springframework.ws.server.endpoint.annotation.PayloadRoot;
import org.springframework.ws.server.endpoint.annotation.RequestPayload;
import org.springframework.ws.server.endpoint.annotation.ResponsePayload;
import pe.com.krypton.categorias.services.CategoriaService;
import pe.com.krypton.categorias.ws.DeleteCategoriaRequest;
import pe.com.krypton.categorias.ws.DeleteCategoriaResponse;
import pe.com.krypton.categorias.ws.GetCategoriasRequest;
import pe.com.krypton.categorias.ws.GetCategoriasResponse;
import pe.com.krypton.categorias.ws.PostCategoriaRequest;
import pe.com.krypton.categorias.ws.PostCategoriaResponse;
import pe.com.krypton.categorias.ws.PutCategoriaRequest;
import pe.com.krypton.categorias.ws.PutCategoriaResponse;

/** Capa web SOAP: rutea cada mensaje (por su localPart en el XSD) al service. */
@Endpoint
public class CategoriaEndpoint {

    private static final String NS = "http://krypton.com/soap/categorias";

    private final CategoriaService service;

    public CategoriaEndpoint(CategoriaService service) {
        this.service = service;
    }

    @PayloadRoot(namespace = NS, localPart = "getCategoriasRequest")
    @ResponsePayload
    public GetCategoriasResponse listar(@RequestPayload GetCategoriasRequest req) {
        return service.listar();
    }

    @PayloadRoot(namespace = NS, localPart = "postCategoriaRequest")
    @ResponsePayload
    public PostCategoriaResponse crear(@RequestPayload PostCategoriaRequest req) {
        return service.crear(req.getCategoria());
    }

    @PayloadRoot(namespace = NS, localPart = "putCategoriaRequest")
    @ResponsePayload
    public PutCategoriaResponse actualizar(@RequestPayload PutCategoriaRequest req) {
        return service.actualizar(req.getCategoria());
    }

    @PayloadRoot(namespace = NS, localPart = "deleteCategoriaRequest")
    @ResponsePayload
    public DeleteCategoriaResponse eliminar(@RequestPayload DeleteCategoriaRequest req) {
        return service.eliminar(req.getId());
    }
}
