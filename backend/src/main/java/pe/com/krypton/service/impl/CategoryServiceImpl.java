package pe.com.krypton.service.impl;

import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Service;
import pe.com.krypton.client.CategoriasSoapClient;
import pe.com.krypton.dto.request.CategoryRequest;
import pe.com.krypton.dto.response.CategoryResponse;
import pe.com.krypton.exception.CategoryInUseException;
import pe.com.krypton.exception.DuplicateCategoryNameException;
import pe.com.krypton.exception.ResourceNotFoundException;
import pe.com.krypton.repository.ProductRepository;
import pe.com.krypton.service.CategoryService;
import pe.com.krypton.soap.ws.Categoria;

/**
 * Categorías delegadas a categorias-soap-service (SOAP). El monolito actúa de PUENTE: mantiene el
 * contrato REST (lo usa el frontend) pero la persistencia vive en el micro SOAP. Si el micro está
 * caído, las llamadas lanzan CategoriasUnavailableException → 503 (degradación elegante).
 * El guard de "categoría en uso" sigue siendo local: los productos viven en el monolito.
 */
@Service
public class CategoryServiceImpl implements CategoryService {

    private final CategoriasSoapClient categoriasSoap;
    private final ProductRepository productRepository;

    public CategoryServiceImpl(CategoriasSoapClient categoriasSoap, ProductRepository productRepository) {
        this.categoriasSoap = categoriasSoap;
        this.productRepository = productRepository;
    }

    @Override
    public List<CategoryResponse> list() {
        return categoriasSoap.listar().stream().map(this::toResponse).toList();
    }

    @Override
    public CategoryResponse getById(Long id) {
        return toResponse(findOrThrow(id));
    }

    @Override
    public CategoryResponse create(CategoryRequest request) {
        if (existsByName(request.name(), null)) {
            throw new DuplicateCategoryNameException("El nombre de categoría ya está registrado: " + request.name());
        }
        Categoria c = new Categoria();
        c.setNombre(request.name());
        c.setDescripcion(request.description());
        long newId = categoriasSoap.crear(c);
        return new CategoryResponse(newId, request.name(), request.description());
    }

    @Override
    public CategoryResponse update(Long id, CategoryRequest request) {
        findOrThrow(id); // 404 si no existe
        if (existsByName(request.name(), id)) {
            throw new DuplicateCategoryNameException(
                    "El nombre de categoría ya está registrado en otra categoría: " + request.name());
        }
        Categoria c = new Categoria();
        c.setId(id);
        c.setNombre(request.name());
        c.setDescripcion(request.description());
        categoriasSoap.actualizar(c);
        return new CategoryResponse(id, request.name(), request.description());
    }

    @Override
    public void delete(Long id) {
        findOrThrow(id); // 404 si no existe
        // Guard ANTES de escribir: los productos viven en el monolito → chequeo local.
        if (productRepository.existsByCategoryId(id)) {
            throw new CategoryInUseException(
                    "La categoría tiene productos asociados y no puede eliminarse: " + id);
        }
        categoriasSoap.eliminar(id);
    }

    // ─── helpers ────────────────────────────────────────────────────────────────

    private Categoria findOrThrow(Long id) {
        return categoriasSoap.listar().stream()
                .filter(c -> Objects.equals(c.getId(), id))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Categoría no encontrada: " + id));
    }

    private boolean existsByName(String name, Long excludeId) {
        return categoriasSoap.listar().stream()
                .anyMatch(c -> c.getNombre().equalsIgnoreCase(name)
                        && (excludeId == null || !Objects.equals(c.getId(), excludeId)));
    }

    private CategoryResponse toResponse(Categoria c) {
        return new CategoryResponse(c.getId(), c.getNombre(), c.getDescripcion());
    }
}
