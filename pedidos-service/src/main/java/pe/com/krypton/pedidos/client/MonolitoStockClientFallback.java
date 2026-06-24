package pe.com.krypton.pedidos.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import pe.com.krypton.pedidos.dto.request.StockSaleRequest;

/**
 * Fallback del circuit-breaker para MonolitoStockClient. Si el monolito no responde, NO rompemos
 * la compra: la orden ya quedó persistida en pedidos. Best-effort — el stock se re-alineará con el
 * próximo cambio del producto desde el admin. Degradación elegante: micro caído ≠ checkout roto.
 */
@Component
public class MonolitoStockClientFallback implements MonolitoStockClient {

    private static final Logger log = LoggerFactory.getLogger(MonolitoStockClientFallback.class);

    @Override
    public void registerSale(StockSaleRequest request) {
        log.warn("No se pudo notificar la venta {} al monolito (best-effort): stock NO descontado",
                request != null ? request.reference() : "?");
    }
}
