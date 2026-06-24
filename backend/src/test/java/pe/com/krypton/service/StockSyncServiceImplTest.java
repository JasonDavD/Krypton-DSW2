package pe.com.krypton.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pe.com.krypton.client.CatalogoSyncPublisher;
import pe.com.krypton.dto.request.StockSaleRequest;
import pe.com.krypton.dto.request.StockSaleRequest.Line;
import pe.com.krypton.exception.InsufficientStockException;
import pe.com.krypton.exception.ResourceNotFoundException;
import pe.com.krypton.model.Product;
import pe.com.krypton.model.StockMovement;
import pe.com.krypton.model.enums.MovementType;
import pe.com.krypton.repository.ProductRepository;
import pe.com.krypton.repository.StockMovementRepository;
import pe.com.krypton.service.impl.StockSyncServiceImpl;

/**
 * Unit test de StockSyncServiceImpl. Repositories y el publisher del catálogo van mockeados.
 * Verifica que una venta notificada por pedidos descuenta el stock cacheado, registra el
 * kardex (SALIDA) y propaga el nuevo estado del producto al catálogo — todo o nada.
 */
@ExtendWith(MockitoExtension.class)
class StockSyncServiceImplTest {

    @Mock ProductRepository productRepository;
    @Mock StockMovementRepository stockMovementRepository;
    @Mock CatalogoSyncPublisher catalogoSync;

    StockSyncServiceImpl service;

    void initService() {
        service = new StockSyncServiceImpl(productRepository, stockMovementRepository, catalogoSync);
    }

    private Product product(int stock) {
        Product p = new Product();
        p.setStock(stock);
        p.setPrice(new BigDecimal("100.00"));
        return p;
    }

    private StockSaleRequest sale(String ref, Line... lines) {
        return new StockSaleRequest(ref, List.of(lines));
    }

    @Test
    void should_decrement_stock_record_kardex_and_publish_when_sale_registered() {
        initService();
        Product mouse = product(5);
        when(productRepository.findByIdWithLock(10L)).thenReturn(Optional.of(mouse));

        service.registerSale(sale("123", new Line(10L, 2)));

        // stock cacheado: 5 − 2 = 3, persistido
        assertThat(mouse.getStock()).isEqualTo(3);
        verify(productRepository).save(mouse);

        // kardex: SALIDA por la cantidad vendida, referenciando la orden
        ArgumentCaptor<StockMovement> captor = ArgumentCaptor.forClass(StockMovement.class);
        verify(stockMovementRepository).save(captor.capture());
        StockMovement mov = captor.getValue();
        assertThat(mov.getType()).isEqualTo(MovementType.SALIDA);
        assertThat(mov.getQuantity()).isEqualTo(2);
        assertThat(mov.getProduct()).isSameAs(mouse);
        assertThat(mov.getReason()).isEqualTo("Venta orden #123");
        assertThat(mov.getReference()).isEqualTo("ORDER-123");

        // propaga el nuevo estado (con el stock ya descontado) al catálogo
        verify(catalogoSync).publish(mouse);
    }

    @Test
    void should_increment_stock_record_entrada_and_publish_when_sale_reverted() {
        initService();
        Product mouse = product(3);
        when(productRepository.findByIdWithLock(10L)).thenReturn(Optional.of(mouse));

        service.revertSale(sale("123", new Line(10L, 2)));

        // stock cacheado: 3 + 2 = 5 (repuesto), persistido
        assertThat(mouse.getStock()).isEqualTo(5);
        verify(productRepository).save(mouse);

        // kardex: ENTRADA por la cancelación
        ArgumentCaptor<StockMovement> captor = ArgumentCaptor.forClass(StockMovement.class);
        verify(stockMovementRepository).save(captor.capture());
        StockMovement mov = captor.getValue();
        assertThat(mov.getType()).isEqualTo(MovementType.ENTRADA);
        assertThat(mov.getQuantity()).isEqualTo(2);
        assertThat(mov.getProduct()).isSameAs(mouse);
        assertThat(mov.getReason()).isEqualTo("Cancelación orden #123");
        assertThat(mov.getReference()).isEqualTo("ORDER-123");

        verify(catalogoSync).publish(mouse);
    }

    @Test
    void should_process_every_line_when_sale_has_multiple_items() {
        initService();
        Product a = product(10);
        Product b = product(4);
        when(productRepository.findByIdWithLock(10L)).thenReturn(Optional.of(a));
        when(productRepository.findByIdWithLock(20L)).thenReturn(Optional.of(b));

        service.registerSale(sale("77", new Line(10L, 3), new Line(20L, 1)));

        assertThat(a.getStock()).isEqualTo(7);
        assertThat(b.getStock()).isEqualTo(3);
        verify(catalogoSync).publish(a);
        verify(catalogoSync).publish(b);
        verify(stockMovementRepository, org.mockito.Mockito.times(2)).save(any());
    }

    @Test
    void should_throw_ResourceNotFound_and_touch_nothing_when_product_missing() {
        initService();
        when(productRepository.findByIdWithLock(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.registerSale(sale("5", new Line(99L, 1))))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(productRepository, never()).save(any());
        verify(stockMovementRepository, never()).save(any());
        verify(catalogoSync, never()).publish(any());
    }

    @Test
    void should_throw_InsufficientStock_and_not_persist_when_quantity_exceeds_stock() {
        initService();
        when(productRepository.findByIdWithLock(10L)).thenReturn(Optional.of(product(1)));

        assertThatThrownBy(() -> service.registerSale(sale("5", new Line(10L, 5))))
                .isInstanceOf(InsufficientStockException.class);

        verify(productRepository, never()).save(any());
        verify(stockMovementRepository, never()).save(any());
        verify(catalogoSync, never()).publish(any());
    }
}
