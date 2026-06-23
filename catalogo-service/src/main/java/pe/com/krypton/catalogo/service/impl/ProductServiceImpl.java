package pe.com.krypton.catalogo.service.impl;

import java.math.BigDecimal;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.com.krypton.catalogo.dto.response.PageResponse;
import pe.com.krypton.catalogo.dto.response.ProductResponse;
import pe.com.krypton.catalogo.exception.ResourceNotFoundException;
import pe.com.krypton.catalogo.mapper.ProductMapper;
import pe.com.krypton.catalogo.model.Product;
import pe.com.krypton.catalogo.repository.ProductRepository;
import pe.com.krypton.catalogo.service.ProductService;
import pe.com.krypton.catalogo.spec.ProductSpecification;

/**
 * Implementación de catálogo — solo lectura.
 *
 * ADAPTACIÓN MICROSERVICIOS: respecto al monolito se eliminó la dependencia de
 * CategoryRepository (la categoría vive en otro servicio) y las operaciones de
 * escritura. Este servicio solo lee productos.
 */
@Service
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;
    private final ProductMapper productMapper;

    public ProductServiceImpl(ProductRepository productRepository,
                              ProductMapper productMapper) {
        this.productRepository = productRepository;
        this.productMapper = productMapper;
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

        Page<ProductResponse> page = productRepository
                .findAll(spec, pageable)
                .map(productMapper::toResponse);

        return PageResponse.of(page);
    }

    @Override
    @Transactional(readOnly = true)
    public ProductResponse getById(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Producto no encontrado: " + id));
        if (!product.isActive()) {
            throw new ResourceNotFoundException("Producto no encontrado: " + id);
        }
        // toResponseWithImages() accede a la colección LAZY images — debe permanecer
        // dentro de @Transactional.
        return productMapper.toResponseWithImages(product);
    }
}
