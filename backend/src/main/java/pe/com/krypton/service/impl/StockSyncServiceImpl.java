package pe.com.krypton.service.impl;

import java.time.Instant;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.com.krypton.client.CatalogoSyncPublisher;
import pe.com.krypton.dto.request.StockSaleRequest;
import pe.com.krypton.exception.InsufficientStockException;
import pe.com.krypton.exception.ResourceNotFoundException;
import pe.com.krypton.model.Product;
import pe.com.krypton.model.StockMovement;
import pe.com.krypton.model.enums.MovementType;
import pe.com.krypton.repository.ProductRepository;
import pe.com.krypton.repository.StockMovementRepository;
import pe.com.krypton.service.StockSyncService;

/**
 * Aplica las ventas de pedidos-service sobre el stock del monolito. Espejo del Pass B del
 * checkout original: bloquea (PESSIMISTIC_WRITE), valida, descuenta {@code products.stock},
 * registra {@code StockMovement(SALIDA)} y propaga el nuevo estado al catálogo. Todo en UNA
 * transacción: o se aplican todas las líneas o ninguna (consistencia stock cacheado ↔ kardex).
 */
@Service
public class StockSyncServiceImpl implements StockSyncService {

    private final ProductRepository productRepository;
    private final StockMovementRepository stockMovementRepository;
    private final CatalogoSyncPublisher catalogoSync;

    public StockSyncServiceImpl(ProductRepository productRepository,
                                StockMovementRepository stockMovementRepository,
                                CatalogoSyncPublisher catalogoSync) {
        this.productRepository = productRepository;
        this.stockMovementRepository = stockMovementRepository;
        this.catalogoSync = catalogoSync;
    }

    @Override
    @Transactional
    public void registerSale(StockSaleRequest request) {
        for (StockSaleRequest.Line line : request.items()) {
            Long productId = line.productId();
            Product product = productRepository.findByIdWithLock(productId)
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Producto no encontrado: " + productId));
            int qty = line.quantity();
            if (qty > product.getStock()) {
                throw new InsufficientStockException(
                        "Stock insuficiente para el producto " + productId
                        + ": solicitado=" + qty + ", disponible=" + product.getStock());
            }

            product.setStock(product.getStock() - qty);
            productRepository.save(product);

            StockMovement movement = new StockMovement();
            movement.setProduct(product);
            movement.setType(MovementType.SALIDA);
            movement.setQuantity(qty);
            movement.setReason("Venta orden #" + request.reference());
            movement.setReference("ORDER-" + request.reference());
            movement.setCreatedAt(Instant.now());
            movement.setCreatedBy(null);
            stockMovementRepository.save(movement);

            // Re-sincroniza el catálogo con el stock ya descontado (reusa la Fase 1, best-effort).
            catalogoSync.publish(product);
        }
    }
}
