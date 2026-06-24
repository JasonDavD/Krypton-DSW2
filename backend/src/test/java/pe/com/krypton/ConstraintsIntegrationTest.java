package pe.com.krypton;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import pe.com.krypton.model.Cart;
import pe.com.krypton.model.Product;
import pe.com.krypton.model.User;
import pe.com.krypton.model.enums.Role;
import pe.com.krypton.repository.CartRepository;
import pe.com.krypton.repository.ProductRepository;
import pe.com.krypton.repository.UserRepository;

/**
 * Cierra los escenarios de rechazo de constraints del spec persistence-schema:
 * unicidad (sku), integridad referencial (FK inválida) y 1 carrito por usuario.
 *
 * @Transactional → cada test hace ROLLBACK al terminar, así no ensucia la base
 * compartida (singleton container) ni rompe los counts de los otros tests.
 */
@Transactional
class ConstraintsIntegrationTest extends AbstractIntegrationTest {

    @Autowired UserRepository users;
    @Autowired ProductRepository products;
    @Autowired CartRepository carts;
    @Autowired JdbcTemplate jdbc;

    @Test
    void rejects_duplicate_sku() {
        products.saveAndFlush(newProduct("SKU-DUP"));

        assertThatThrownBy(() -> products.saveAndFlush(newProduct("SKU-DUP")))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    // NOTA: el test "rejects_product_with_nonexistent_category" se ELIMINÓ a propósito: Category se
    // desacopló a categorias-soap-service y products.category_id ya NO tiene FK (id suelto, como en
    // catalogo-service). La existencia de la categoría la valida el monolito vía SOAP, no la DB.

    @Test
    void rejects_second_cart_for_same_user() {
        User u = users.saveAndFlush(newUser());
        carts.saveAndFlush(newCart(u));

        assertThatThrownBy(() -> carts.saveAndFlush(newCart(u)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    // ----- helpers -----

    private Product newProduct(String sku) {
        Product p = new Product();
        p.setSku(sku);
        p.setName("Producto");
        p.setPrice(new BigDecimal("10.00"));
        p.setStock(0);
        p.setActive(true);
        p.setCategoryId(1L);
        return p;
    }

    private User newUser() {
        User u = new User();
        u.setName("Test");
        u.setEmail("user-" + System.nanoTime() + "@krypton.pe");
        u.setPassword("x");
        u.setRole(Role.CLIENTE);
        u.setCreatedAt(Instant.now());
        return u;
    }

    private Cart newCart(User u) {
        Cart c = new Cart();
        c.setUser(u);
        c.setCreatedAt(Instant.now());
        c.setUpdatedAt(Instant.now());
        return c;
    }
}
