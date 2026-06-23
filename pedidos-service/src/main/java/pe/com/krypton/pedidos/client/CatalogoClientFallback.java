package pe.com.krypton.pedidos.client;

import org.springframework.stereotype.Component;

/**
 * Fallback del circuit-breaker para CatalogoClient. Cuando catalogo-service no responde
 * (timeout, error, circuito abierto), Feign invoca este componente en vez del HTTP real.
 *
 * Devuelve {@code null} para señalar "catálogo no disponible" (degradado). El service
 * traduce ese null a CatalogoUnavailableException → 503, sin filtrar detalles internos.
 */
@Component
public class CatalogoClientFallback implements CatalogoClient {

    @Override
    public ProductDTO getById(Long id) {
        return null;
    }
}
