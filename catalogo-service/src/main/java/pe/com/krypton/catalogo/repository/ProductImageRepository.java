package pe.com.krypton.catalogo.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import pe.com.krypton.catalogo.model.ProductImage;

/**
 * Repositorio de imágenes de producto. La colección se carga vía la relación LAZY de Product
 * en el detalle. El sync desde el monolito REEMPLAZA la galería: borra las filas del producto
 * e inserta las nuevas (queries nativas, mismo patrón que el upsert de productos).
 */
public interface ProductImageRepository extends JpaRepository<ProductImage, Long> {

    @Modifying
    @Query(value = "DELETE FROM product_image WHERE product_id = :productId", nativeQuery = true)
    void deleteByProductId(@Param("productId") Long productId);

    @Modifying
    @Query(value = """
            INSERT INTO product_image (product_id, path, display_order, is_cover)
            VALUES (:productId, :path, :displayOrder, :cover)
            """, nativeQuery = true)
    void insertImage(@Param("productId") Long productId,
                     @Param("path") String path,
                     @Param("displayOrder") short displayOrder,
                     @Param("cover") boolean cover);
}
