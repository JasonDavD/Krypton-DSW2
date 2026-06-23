package pe.com.krypton.pedidos.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * Cliente Feign hacia catalogo-service (resuelto por Eureka con el nombre CATALOGO).
 * El path base /api/products replica el contrato real del catálogo.
 *
 * Con el circuit-breaker habilitado (spring.cloud.openfeign.circuitbreaker.enabled=true),
 * cualquier fallo o apertura del circuito deriva en {@link CatalogoClientFallback},
 * que devuelve null (degradado). El service interpreta ese null como "catálogo caído".
 */
@FeignClient(name = "CATALOGO", path = "/api/products", fallback = CatalogoClientFallback.class)
public interface CatalogoClient {

    @GetMapping("/{id}")
    ProductDTO getById(@PathVariable("id") Long id);
}
