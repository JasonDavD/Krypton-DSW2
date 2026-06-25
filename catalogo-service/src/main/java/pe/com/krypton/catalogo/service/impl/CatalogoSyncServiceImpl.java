package pe.com.krypton.catalogo.service.impl;

import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.com.krypton.catalogo.dto.request.ProductImageSyncRequest;
import pe.com.krypton.catalogo.dto.request.ProductSyncRequest;
import pe.com.krypton.catalogo.repository.ProductImageRepository;
import pe.com.krypton.catalogo.repository.ProductRepository;
import pe.com.krypton.catalogo.service.CatalogoSyncService;

/**
 * Reusa el upsert nativo del repositorio para reflejar el estado que el monolito propaga,
 * y REEMPLAZA la galería del producto con la que viene en el request (borra + inserta).
 */
@Service
public class CatalogoSyncServiceImpl implements CatalogoSyncService {

    private final ProductRepository productRepository;
    private final ProductImageRepository productImageRepository;

    public CatalogoSyncServiceImpl(ProductRepository productRepository,
                                   ProductImageRepository productImageRepository) {
        this.productRepository = productRepository;
        this.productImageRepository = productImageRepository;
    }

    @Override
    @Transactional
    public void sync(Long id, ProductSyncRequest r) {
        // 1. Upsert del producto (debe existir antes de insertar sus imágenes por la FK).
        productRepository.upsert(id, r.sku(), r.name(), r.description(),
                r.price(), r.stock(), r.imageUrl(), r.active(), r.categoryId());

        // 2. Reemplazo de la galería: el monolito es la fuente de verdad.
        productImageRepository.deleteByProductId(id);
        List<ProductImageSyncRequest> images = r.images();
        if (images != null) {
            for (ProductImageSyncRequest img : images) {
                productImageRepository.insertImage(id, img.path(), img.displayOrder(), img.cover());
            }
        }
    }
}
