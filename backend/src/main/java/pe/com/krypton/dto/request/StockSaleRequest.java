package pe.com.krypton.dto.request;

import java.util.List;

/**
 * Venta que pedidos-service notifica al monolito para que descuente el stock cacheado y
 * registre el kardex (SALIDA). El monolito es el MASTER del stock; tras descontar, propaga
 * el nuevo estado del producto al catálogo vía {@code CatalogoSyncPublisher} (Fase 1).
 */
public record StockSaleRequest(String reference, List<Line> items) {

    /** Línea vendida: cuánto descontar de qué producto (por id surrogate). */
    public record Line(Long productId, int quantity) {
    }
}
