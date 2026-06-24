package pe.com.krypton.catalogo.controller;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import pe.com.krypton.catalogo.dto.request.ProductSyncRequest;
import pe.com.krypton.catalogo.service.CatalogoSyncService;

/**
 * Endpoints INTERNOS de sincronización. Bajo {@code /internal/**} (no {@code /api/**}), así que
 * el gateway NO los rutea: no son accesibles desde el browser. Solo el monolito los invoca por
 * Feign sobre la red interna (descubrimiento Eureka).
 */
@RestController
@RequestMapping("/internal/products")
public class ProductSyncController {

    private final CatalogoSyncService syncService;

    public ProductSyncController(CatalogoSyncService syncService) {
        this.syncService = syncService;
    }

    /** PUT /internal/products/{id} — upsert del producto con el estado que envía el monolito. */
    @PutMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void sync(@PathVariable Long id, @RequestBody ProductSyncRequest request) {
        syncService.sync(id, request);
    }
}
