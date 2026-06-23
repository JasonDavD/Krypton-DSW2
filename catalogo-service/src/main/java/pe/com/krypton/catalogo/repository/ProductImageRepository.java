package pe.com.krypton.catalogo.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pe.com.krypton.catalogo.model.ProductImage;

/**
 * Repositorio de imágenes de producto. La colección se carga vía la relación
 * LAZY de Product en el detalle; no se requieren queries propias por ahora.
 */
public interface ProductImageRepository extends JpaRepository<ProductImage, Long> {
}
