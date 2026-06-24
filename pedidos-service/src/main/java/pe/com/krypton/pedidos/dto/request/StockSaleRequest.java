package pe.com.krypton.pedidos.dto.request;

import java.util.List;

/**
 * Venta que pedidos-service notifica al monolito (master del stock) tras un checkout, para que
 * descuente el stock cacheado, registre el kardex y re-sincronice el catálogo. Espeja el shape
 * que consume {@code /internal/stock/sale} del monolito.
 */
public record StockSaleRequest(String reference, List<Line> items) {

    /** Línea vendida: cuánto descontar de qué producto (por id surrogate del catálogo). */
    public record Line(Long productId, int quantity) {
    }
}
