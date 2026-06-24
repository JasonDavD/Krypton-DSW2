package pe.com.krypton.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import pe.com.krypton.dto.request.ProductSyncRequest;
import pe.com.krypton.model.Product;

/**
 * Propaga el estado de un producto a catalogo-service (Feign) tras un cambio del admin.
 * BEST-EFFORT: si el catálogo no responde, loguea y NO rompe la operación del monolito —
 * la fuente de verdad ya guardó; el catálogo se re-alinea en el próximo cambio del producto.
 */
@Component
public class CatalogoSyncPublisher {

    private static final Logger log = LoggerFactory.getLogger(CatalogoSyncPublisher.class);

    private final CatalogoSyncClient client;

    public CatalogoSyncPublisher(CatalogoSyncClient client) {
        this.client = client;
    }

    /** Empuja el producto al catálogo. Tolera fallos del micro (no propaga la excepción). */
    public void publish(Product p) {
        try {
            client.sync(p.getId(), new ProductSyncRequest(
                    p.getSku(), p.getName(), p.getDescription(), p.getPrice(),
                    p.getStock(), p.getImageUrl(), p.isActive(), p.getCategoryId()));
        } catch (Exception e) {
            log.warn("No se pudo sincronizar el producto {} al catalogo (best-effort): {}",
                    p.getId(), e.getMessage());
        }
    }
}
