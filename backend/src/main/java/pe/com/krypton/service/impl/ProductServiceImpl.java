package pe.com.krypton.service.impl;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.com.krypton.client.CatalogoSyncPublisher;
import pe.com.krypton.client.CategoriasSoapClient;
import pe.com.krypton.dto.request.ProductRequest;
import pe.com.krypton.dto.response.PageResponse;
import pe.com.krypton.dto.response.ProductResponse;
import pe.com.krypton.exception.CategoriasUnavailableException;
import pe.com.krypton.exception.DuplicateSkuException;
import pe.com.krypton.exception.ResourceNotFoundException;
import pe.com.krypton.mapper.ProductMapper;
import pe.com.krypton.model.Product;
import pe.com.krypton.repository.ProductRepository;
import pe.com.krypton.service.ProductService;
import pe.com.krypton.soap.ws.Categoria;
import pe.com.krypton.spec.ProductSpecification;

@Service
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;
    private final CategoriasSoapClient categoriasSoap;
    private final ProductMapper productMapper;
    private final CatalogoSyncPublisher catalogoSync;

    public ProductServiceImpl(ProductRepository productRepository,
                               CategoriasSoapClient categoriasSoap,
                               ProductMapper productMapper,
                               CatalogoSyncPublisher catalogoSync) {
        this.productRepository = productRepository;
        this.categoriasSoap = categoriasSoap;
        this.productMapper = productMapper;
        this.catalogoSync = catalogoSync;
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<ProductResponse> search(String name, Long categoryId,
                                                 BigDecimal priceMin, BigDecimal priceMax,
                                                 Pageable pageable) {
        Specification<Product> spec = Specification
                .where(ProductSpecification.isActive(true))
                .and(ProductSpecification.nameLike(name))
                .and(ProductSpecification.hasCategory(categoryId))
                .and(ProductSpecification.priceBetween(priceMin, priceMax));

        Map<Long, String> names = categoryNamesLenient();
        Page<ProductResponse> page = productRepository
                .findAll(spec, pageable)
                .map(p -> productMapper.toResponse(p, names.get(p.getCategoryId())));

        return PageResponse.of(page);
    }

    @Override
    @Transactional(readOnly = true)
    public ProductResponse getById(Long id) {
        Product product = findOrThrow(id);
        if (!product.isActive()) {
            throw new ResourceNotFoundException("Producto no encontrado: " + id);
        }
        // toResponseWithImages() accesses the LAZY images collection — must remain inside @Transactional
        return productMapper.toResponseWithImages(product, categoryNamesLenient().get(product.getCategoryId()));
    }

    @Override
    @Transactional
    public ProductResponse create(ProductRequest request) {
        if (productRepository.existsBySku(request.sku())) {
            throw new DuplicateSkuException("El SKU ya está registrado: " + request.sku());
        }
        String categoryName = requireCategoryName(request.categoryId());

        Product product = new Product();
        product.setSku(request.sku());
        product.setName(request.name());
        product.setDescription(request.description());
        product.setPrice(request.price());
        // stock: bootstrap value only — never mutated by catalog operations after this point
        product.setStock(request.stock() != null ? request.stock() : 0);
        product.setImageUrl(request.imageUrl());
        product.setActive(true);
        product.setCategoryId(request.categoryId());

        Product saved = productRepository.save(product);
        catalogoSync.publish(saved);  // propaga el alta al catalogo-service (best-effort)
        return productMapper.toResponse(saved, categoryName);
    }

    @Override
    @Transactional
    public ProductResponse update(Long id, ProductRequest request) {
        Product product = findOrThrow(id);

        if (productRepository.existsBySkuAndIdNot(request.sku(), id)) {
            throw new DuplicateSkuException("El SKU ya está registrado en otro producto: " + request.sku());
        }
        String categoryName = requireCategoryName(request.categoryId());

        product.setSku(request.sku());
        product.setName(request.name());
        product.setDescription(request.description());
        product.setPrice(request.price());
        // stock is READ-ONLY after creation — intentionally NOT updated here
        product.setImageUrl(request.imageUrl());
        product.setCategoryId(request.categoryId());

        Product saved = productRepository.save(product);
        catalogoSync.publish(saved);  // propaga la edicion al catalogo-service (best-effort)
        return productMapper.toResponse(saved, categoryName);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        Product product = findOrThrow(id);
        // SOFT delete: marca como inactivo, no elimina la fila
        product.setActive(false);
        catalogoSync.publish(productRepository.save(product));  // propaga la baja (best-effort)
    }

    // ─── private helpers ────────────────────────────────────────────────────────

    private Product findOrThrow(Long id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Producto no encontrado: " + id));
    }

    /**
     * Nombres de categoría (id→nombre) desde el micro SOAP. LENIENTE: si el micro está caído,
     * devuelve un mapa vacío para no romper las LECTURAS de productos (el nombre saldrá null).
     */
    private Map<Long, String> categoryNamesLenient() {
        try {
            return categoriasSoap.listar().stream()
                    .collect(Collectors.toMap(Categoria::getId, Categoria::getNombre));
        } catch (CategoriasUnavailableException e) {
            return Map.of();
        }
    }

    /**
     * ESTRICTO para ESCRITURAS: valida que la categoría exista en el micro SOAP y devuelve su
     * nombre. 404 si no existe; 503 si el micro está caído (propaga CategoriasUnavailableException).
     */
    private String requireCategoryName(Long categoryId) {
        return categoriasSoap.listar().stream()
                .filter(c -> Objects.equals(c.getId(), categoryId))
                .map(Categoria::getNombre)
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Categoría no encontrada: " + categoryId));
    }
}
