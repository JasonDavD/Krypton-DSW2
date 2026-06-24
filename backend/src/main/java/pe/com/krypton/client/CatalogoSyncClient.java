package pe.com.krypton.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import pe.com.krypton.dto.request.ProductSyncRequest;

/**
 * Cliente Feign hacia {@code catalogo-service} (Eureka app {@code CATALOGO}) para empujar el
 * estado de un producto tras un cambio del admin. Pega al endpoint INTERNO {@code /internal/**}
 * del micro (no expuesto por el gateway).
 */
@FeignClient(name = "CATALOGO")
public interface CatalogoSyncClient {

    @PutMapping("/internal/products/{id}")
    void sync(@PathVariable("id") Long id, @RequestBody ProductSyncRequest request);
}
