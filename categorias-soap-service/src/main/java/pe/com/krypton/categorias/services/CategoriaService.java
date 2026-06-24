package pe.com.krypton.categorias.services;

import org.springframework.stereotype.Service;
import pe.com.krypton.categorias.repository.CategoriaRepository;
import pe.com.krypton.categorias.ws.Categoria;
import pe.com.krypton.categorias.ws.DeleteCategoriaResponse;
import pe.com.krypton.categorias.ws.GetCategoriasResponse;
import pe.com.krypton.categorias.ws.PostCategoriaResponse;
import pe.com.krypton.categorias.ws.PutCategoriaResponse;

/** Capa de negocio: arma los objetos Response del contrato a partir del repository. */
@Service
public class CategoriaService {

    private final CategoriaRepository repo;

    public CategoriaService(CategoriaRepository repo) {
        this.repo = repo;
    }

    public GetCategoriasResponse listar() {
        GetCategoriasResponse r = new GetCategoriasResponse();
        r.getLista().addAll(repo.listar());
        return r;
    }

    public PostCategoriaResponse crear(Categoria c) {
        PostCategoriaResponse r = new PostCategoriaResponse();
        r.setSalida(repo.registrar(c));
        return r;
    }

    public PutCategoriaResponse actualizar(Categoria c) {
        PutCategoriaResponse r = new PutCategoriaResponse();
        r.setSalida(repo.actualizar(c));
        return r;
    }

    public DeleteCategoriaResponse eliminar(long id) {
        DeleteCategoriaResponse r = new DeleteCategoriaResponse();
        r.setSalida(repo.eliminar(id));
        return r;
    }
}
