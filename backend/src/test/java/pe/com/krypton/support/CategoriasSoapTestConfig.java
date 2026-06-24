package pe.com.krypton.support;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import pe.com.krypton.client.CategoriasSoapClient;
import pe.com.krypton.soap.ws.Categoria;

/**
 * Reemplaza el cliente SOAP real por un fake EN MEMORIA para los tests de integración
 * (no hay categorias-soap-service corriendo en el test). Pre-seedeado con las 5 categorías
 * base (ids 1-5), para que los productos que referencian category_id 1..5 sigan siendo válidos.
 */
@TestConfiguration
public class CategoriasSoapTestConfig {

    @Bean
    @Primary
    public CategoriasSoapClient fakeCategoriasSoapClient() {
        return new InMemoryCategoriasSoapClient();
    }

    /** Implementación stateful en memoria del contrato del cliente SOAP. */
    public static class InMemoryCategoriasSoapClient implements CategoriasSoapClient {

        private final Map<Long, Categoria> store = new ConcurrentHashMap<>();
        private final AtomicLong seq = new AtomicLong(5);

        public InMemoryCategoriasSoapClient() {
            seed(1, "Laptops", "Notebooks y ultrabooks");
            seed(2, "Audio", "Audífonos, parlantes y micrófonos");
            seed(3, "Componentes", "GPU, CPU, RAM y almacenamiento");
            seed(4, "Periféricos", "Teclados, mouse y accesorios");
            seed(5, "Monitores", "Monitores y pantallas");
        }

        private void seed(long id, String nombre, String descripcion) {
            Categoria c = new Categoria();
            c.setId(id);
            c.setNombre(nombre);
            c.setDescripcion(descripcion);
            store.put(id, c);
        }

        @Override
        public List<Categoria> listar() {
            return new ArrayList<>(store.values());
        }

        @Override
        public long crear(Categoria categoria) {
            long id = seq.incrementAndGet();
            categoria.setId(id);
            store.put(id, categoria);
            return id;
        }

        @Override
        public int actualizar(Categoria categoria) {
            if (!store.containsKey(categoria.getId())) {
                return 0;
            }
            store.put(categoria.getId(), categoria);
            return 1;
        }

        @Override
        public int eliminar(long id) {
            return store.remove(id) != null ? 1 : 0;
        }
    }
}
