package pe.com.krypton.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pe.com.krypton.client.CategoriasSoapClient;
import pe.com.krypton.dto.request.CategoryRequest;
import pe.com.krypton.dto.response.CategoryResponse;
import pe.com.krypton.exception.CategoryInUseException;
import pe.com.krypton.exception.DuplicateCategoryNameException;
import pe.com.krypton.exception.ResourceNotFoundException;
import pe.com.krypton.repository.ProductRepository;
import pe.com.krypton.service.impl.CategoryServiceImpl;
import pe.com.krypton.soap.ws.Categoria;

/**
 * Unit test de CategoryServiceImpl. El cliente SOAP y el ProductRepository van MOCKEADOS.
 * Las categorías viven en categorias-soap-service; el monolito delega (puente REST→SOAP).
 */
@ExtendWith(MockitoExtension.class)
class CategoryServiceImplTest {

    @Mock CategoriasSoapClient categoriasSoap;
    @Mock ProductRepository productRepository;

    CategoryServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new CategoryServiceImpl(categoriasSoap, productRepository);
    }

    private Categoria cat(long id, String nombre) {
        Categoria c = new Categoria();
        c.setId(id);
        c.setNombre(nombre);
        c.setDescripcion("Desc of " + nombre);
        return c;
    }

    // ─── list ───────────────────────────────────────────────────────────────────

    @Test
    void should_return_all_categories() {
        when(categoriasSoap.listar()).thenReturn(List.of(cat(1, "Electronics"), cat(2, "Books")));

        List<CategoryResponse> result = service.list();

        assertThat(result).hasSize(2);
        assertThat(result.get(0).name()).isEqualTo("Electronics");
    }

    // ─── getById ────────────────────────────────────────────────────────────────

    @Test
    void should_return_category_when_found() {
        when(categoriasSoap.listar()).thenReturn(List.of(cat(1, "Electronics")));

        CategoryResponse result = service.getById(1L);

        assertThat(result.id()).isEqualTo(1L);
        assertThat(result.name()).isEqualTo("Electronics");
    }

    @Test
    void should_throw_not_found_when_category_missing() {
        when(categoriasSoap.listar()).thenReturn(List.of());

        assertThatThrownBy(() -> service.getById(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ─── create ─────────────────────────────────────────────────────────────────

    @Test
    void should_create_category_when_name_is_unique() {
        when(categoriasSoap.listar()).thenReturn(List.of(cat(1, "Electronics")));
        when(categoriasSoap.crear(any())).thenReturn(5L);

        CategoryResponse result = service.create(new CategoryRequest("NewCat", "A new category"));

        assertThat(result.id()).isEqualTo(5L);
        assertThat(result.name()).isEqualTo("NewCat");
    }

    @Test
    void should_reject_create_when_name_already_exists() {
        when(categoriasSoap.listar()).thenReturn(List.of(cat(1, "Electronics")));

        assertThatThrownBy(() -> service.create(new CategoryRequest("Electronics", "Desc")))
                .isInstanceOf(DuplicateCategoryNameException.class);
        verify(categoriasSoap, never()).crear(any());
    }

    // ─── update ─────────────────────────────────────────────────────────────────

    @Test
    void should_update_category_when_new_name_is_unique() {
        when(categoriasSoap.listar()).thenReturn(List.of(cat(1, "OldName")));
        when(categoriasSoap.actualizar(any())).thenReturn(1);

        CategoryResponse result = service.update(1L, new CategoryRequest("NewName", "Updated desc"));

        assertThat(result.name()).isEqualTo("NewName");
    }

    @Test
    void should_allow_update_keeping_same_name() {
        when(categoriasSoap.listar()).thenReturn(List.of(cat(1, "Electronics")));
        when(categoriasSoap.actualizar(any())).thenReturn(1);

        CategoryResponse result = service.update(1L, new CategoryRequest("Electronics", "Updated desc"));

        assertThat(result.name()).isEqualTo("Electronics");
    }

    @Test
    void should_reject_update_when_name_belongs_to_another_category() {
        when(categoriasSoap.listar()).thenReturn(List.of(cat(1, "OldName"), cat(2, "TakenName")));

        assertThatThrownBy(() -> service.update(1L, new CategoryRequest("TakenName", "Desc")))
                .isInstanceOf(DuplicateCategoryNameException.class);
        verify(categoriasSoap, never()).actualizar(any());
    }

    // ─── delete ─────────────────────────────────────────────────────────────────

    @Test
    void should_delete_category_when_no_products_reference_it() {
        when(categoriasSoap.listar()).thenReturn(List.of(cat(3, "Empty")));
        when(productRepository.existsByCategoryId(3L)).thenReturn(false);

        service.delete(3L);

        verify(categoriasSoap).eliminar(3L);
    }

    @Test
    void should_throw_category_in_use_when_products_exist() {
        when(categoriasSoap.listar()).thenReturn(List.of(cat(1, "Electronics")));
        when(productRepository.existsByCategoryId(1L)).thenReturn(true);

        assertThatThrownBy(() -> service.delete(1L))
                .isInstanceOf(CategoryInUseException.class);
        verify(categoriasSoap, never()).eliminar(anyLong());
    }

    @Test
    void should_throw_not_found_on_delete_when_category_missing() {
        when(categoriasSoap.listar()).thenReturn(List.of());

        assertThatThrownBy(() -> service.delete(99L))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(categoriasSoap, never()).eliminar(anyLong());
    }
}
