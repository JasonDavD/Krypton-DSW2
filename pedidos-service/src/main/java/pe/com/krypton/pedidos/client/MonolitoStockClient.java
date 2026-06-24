package pe.com.krypton.pedidos.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import pe.com.krypton.pedidos.dto.request.StockSaleRequest;

/**
 * Cliente Feign hacia el MONOLITO (master del stock). El monolito NO se registra en Eureka, así
 * que se resuelve por URL FIJA (parametrizable con {@code MONOLITH_URI} para Docker), igual que
 * hace el gateway. Pega al endpoint interno {@code /internal/stock/sale}.
 *
 * Con el circuit-breaker habilitado, si el monolito no responde Feign deriva a
 * {@link MonolitoStockClientFallback} (no-op): la compra se completa igual (degradación elegante).
 */
@FeignClient(name = "monolito", url = "${MONOLITH_URI:http://localhost:8080}",
        fallback = MonolitoStockClientFallback.class)
public interface MonolitoStockClient {

    @PostMapping("/internal/stock/sale")
    void registerSale(@RequestBody StockSaleRequest request);
}
