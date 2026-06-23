package pe.com.krypton.catalogo.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import pe.com.krypton.catalogo.model.Product;

/**
 * Repositorio de productos. Solo se conservan los métodos necesarios para la
 * superficie de LECTURA del catálogo (lista con filtros + detalle por id).
 */
public interface ProductRepository extends JpaRepository<Product, Long>,
        JpaSpecificationExecutor<Product> {
}
