package pe.com.krypton.controller;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import pe.com.krypton.dto.request.StockSaleRequest;
import pe.com.krypton.service.StockSyncService;

/**
 * Endpoint INTERNO de stock. Bajo {@code /internal/**} (no {@code /api/**}) → el gateway NO lo
 * rutea: no es accesible desde el browser. Solo pedidos-service lo invoca por Feign (URL fija)
 * tras un checkout, para que el monolito (master del stock) descuente y re-sincronice el catálogo.
 */
@RestController
@RequestMapping("/internal/stock")
public class StockSyncController {

    private final StockSyncService stockSyncService;

    public StockSyncController(StockSyncService stockSyncService) {
        this.stockSyncService = stockSyncService;
    }

    /** POST /internal/stock/sale — aplica la venta (descuenta stock + kardex + sync catálogo). */
    @PostMapping("/sale")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void registerSale(@RequestBody StockSaleRequest request) {
        stockSyncService.registerSale(request);
    }

    /** POST /internal/stock/revert — revierte la venta al cancelar (repone stock + kardex). */
    @PostMapping("/revert")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void revertSale(@RequestBody StockSaleRequest request) {
        stockSyncService.revertSale(request);
    }
}
