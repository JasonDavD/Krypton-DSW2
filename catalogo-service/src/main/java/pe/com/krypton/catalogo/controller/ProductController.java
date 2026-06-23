package pe.com.krypton.catalogo.controller;

import java.math.BigDecimal;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import pe.com.krypton.catalogo.dto.response.PageResponse;
import pe.com.krypton.catalogo.dto.response.ProductResponse;
import pe.com.krypton.catalogo.service.ProductService;

/**
 * Endpoints públicos de catálogo — solo lectura. El servicio queda abierto
 * detrás del gateway (sin Spring Security por ahora). Mismo path y contrato
 * que el monolito: /api/products.
 */
@RestController
@RequestMapping("/api/products")
public class ProductController {

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @GetMapping
    public PageResponse<ProductResponse> search(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) BigDecimal priceMin,
            @RequestParam(required = false) BigDecimal priceMax,
            Pageable pageable) {
        return productService.search(name, categoryId, priceMin, priceMax, pageable);
    }

    @GetMapping("/{id}")
    public ProductResponse getById(@PathVariable Long id) {
        return productService.getById(id);
    }
}
