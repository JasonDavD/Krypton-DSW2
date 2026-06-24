package pe.com.krypton.categorias.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pe.com.krypton.categorias.repository.CategoriaRepository;
import pe.com.krypton.categorias.ws.Categoria;
import pe.com.krypton.categorias.ws.DeleteCategoriaResponse;
import pe.com.krypton.categorias.ws.GetCategoriasResponse;
import pe.com.krypton.categorias.ws.PostCategoriaResponse;
import pe.com.krypton.categorias.ws.PutCategoriaResponse;

/** Unit test de la capa de negocio SOAP. El repository (JdbcTemplate) va mockeado. */
@ExtendWith(MockitoExtension.class)
class CategoriaServiceTest {

    @Mock CategoriaRepository repo;
    @InjectMocks CategoriaService service;

    private Categoria categoria(long id, String nombre) {
        Categoria c = new Categoria();
        c.setId(id);
        c.setNombre(nombre);
        return c;
    }

    @Test
    void listar_mapea_las_categorias_del_repo() {
        when(repo.listar()).thenReturn(List.of(categoria(1L, "Laptops"), categoria(2L, "Audio")));

        GetCategoriasResponse r = service.listar();

        assertThat(r.getLista()).hasSize(2);
        assertThat(r.getLista().get(0).getNombre()).isEqualTo("Laptops");
    }

    @Test
    void crear_devuelve_el_id_generado() {
        Categoria nueva = categoria(0L, "Redes");
        when(repo.registrar(nueva)).thenReturn(42L);

        PostCategoriaResponse r = service.crear(nueva);

        assertThat(r.getSalida()).isEqualTo(42L);
    }

    @Test
    void actualizar_devuelve_filas_afectadas() {
        Categoria c = categoria(3L, "Componentes");
        when(repo.actualizar(c)).thenReturn(1);

        PutCategoriaResponse r = service.actualizar(c);

        assertThat(r.getSalida()).isEqualTo(1);
    }

    @Test
    void eliminar_devuelve_filas_afectadas() {
        when(repo.eliminar(5L)).thenReturn(1);

        DeleteCategoriaResponse r = service.eliminar(5L);

        assertThat(r.getSalida()).isEqualTo(1);
    }
}
