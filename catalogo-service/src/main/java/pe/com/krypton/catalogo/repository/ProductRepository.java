package pe.com.krypton.catalogo.repository;

import java.math.BigDecimal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import pe.com.krypton.catalogo.model.Product;

/**
 * Repositorio de productos. La superficie de LECTURA del catálogo (lista + detalle) más el
 * {@link #upsert} interno que usa el monolito para sincronizar (no expuesto al público).
 */
public interface ProductRepository extends JpaRepository<Product, Long>,
        JpaSpecificationExecutor<Product> {

    /**
     * Inserta o actualiza un producto POR ID (sync desde el monolito). Usa el upsert nativo de
     * MySQL para poder fijar el id explícito (el catálogo usa AUTO_INCREMENT, que ignoraría un
     * id seteado vía save()). Idempotente: si el id existe, reemplaza sus columnas mutables.
     */
    @Modifying
    @Query(value = """
            INSERT INTO products (id, sku, name, description, price, stock, image_url, active, category_id)
            VALUES (:id, :sku, :name, :description, :price, :stock, :imageUrl, :active, :categoryId)
            ON DUPLICATE KEY UPDATE
              sku = VALUES(sku), name = VALUES(name), description = VALUES(description),
              price = VALUES(price), stock = VALUES(stock), image_url = VALUES(image_url),
              active = VALUES(active), category_id = VALUES(category_id)
            """, nativeQuery = true)
    void upsert(@Param("id") Long id,
                @Param("sku") String sku,
                @Param("name") String name,
                @Param("description") String description,
                @Param("price") BigDecimal price,
                @Param("stock") int stock,
                @Param("imageUrl") String imageUrl,
                @Param("active") boolean active,
                @Param("categoryId") Long categoryId);
}
