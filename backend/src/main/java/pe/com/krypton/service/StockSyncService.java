package pe.com.krypton.service;

import pe.com.krypton.dto.request.StockSaleRequest;

/**
 * Aplica en el monolito (master del stock) las ventas que ocurren en pedidos-service.
 * Descuenta el stock cacheado, registra el kardex (SALIDA) y propaga al catálogo.
 */
public interface StockSyncService {

    /** Registra una venta: descuenta stock + kardex por cada ítem y re-sincroniza el catálogo. */
    void registerSale(StockSaleRequest request);

    /** Revierte una venta (cancelación): repone stock + kardex (ENTRADA) y re-sincroniza el catálogo. */
    void revertSale(StockSaleRequest request);
}
