package pe.com.krypton.catalogo.service.impl;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.com.krypton.catalogo.dto.request.ProductSyncRequest;
import pe.com.krypton.catalogo.repository.ProductRepository;
import pe.com.krypton.catalogo.service.CatalogoSyncService;

/** Reusa el upsert nativo del repositorio para reflejar el estado que el monolito propaga. */
@Service
public class CatalogoSyncServiceImpl implements CatalogoSyncService {

    private final ProductRepository productRepository;

    public CatalogoSyncServiceImpl(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    @Override
    @Transactional
    public void sync(Long id, ProductSyncRequest r) {
        productRepository.upsert(id, r.sku(), r.name(), r.description(),
                r.price(), r.stock(), r.imageUrl(), r.active(), r.categoryId());
    }
}
