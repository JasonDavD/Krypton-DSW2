package pe.com.krypton.catalogo.service;

import pe.com.krypton.catalogo.dto.request.ProductSyncRequest;

/** Sincronización de productos empujada desde el monolito (la fuente de verdad mutable). */
public interface CatalogoSyncService {

    /** Inserta o actualiza el producto {@code id} con el estado completo que envía el monolito. */
    void sync(Long id, ProductSyncRequest request);
}
