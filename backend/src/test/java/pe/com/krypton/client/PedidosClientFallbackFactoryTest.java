package pe.com.krypton.client;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import feign.FeignException;
import org.junit.jupiter.api.Test;
import pe.com.krypton.exception.PedidosUnavailableException;

/**
 * Unit test del fallback de PedidosClient. Cuando pedidos responde con un status HTTP real
 * (404/422) la causa se PRESERVA (re-lanza la FeignException). Cuando pedidos está caído
 * (timeout/connection refused/circuito abierto → status <= 0), degrada a 503 elegante.
 */
class PedidosClientFallbackFactoryTest {

    private final PedidosClientFallbackFactory factory = new PedidosClientFallbackFactory();

    @Test
    void should_rethrow_feign_exception_when_pedidos_responds_4xx() {
        FeignException notFound = mock(FeignException.class);
        when(notFound.status()).thenReturn(404);

        PedidosClient fb = factory.create(notFound);

        assertThatThrownBy(() -> fb.getById(1L)).isSameAs(notFound); // 404 preservado, no enmascarado
    }

    @Test
    void should_degrade_to_503_when_pedidos_is_down_no_http_status() {
        FeignException down = mock(FeignException.class);
        when(down.status()).thenReturn(-1); // sin respuesta: caído/timeout

        PedidosClient fb = factory.create(down);

        assertThatThrownBy(() -> fb.listAll(null, null, null, null, null))
                .isInstanceOf(PedidosUnavailableException.class);
    }

    @Test
    void should_degrade_to_503_on_generic_connection_error() {
        PedidosClient fb = factory.create(new RuntimeException("Connection refused"));

        assertThatThrownBy(() -> fb.updateStatus(1L, null))
                .isInstanceOf(PedidosUnavailableException.class);
    }
}
