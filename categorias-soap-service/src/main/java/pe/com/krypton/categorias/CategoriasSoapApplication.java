package pe.com.krypton.categorias;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Microservicio SOAP de Categorías (contract-first). Expone CRUD de categorías por SOAP en
 * /ws/* con su propia base MySQL. Desacoplado del monolito (que lo consume como puente REST→SOAP).
 */
@SpringBootApplication
public class CategoriasSoapApplication {
    public static void main(String[] args) {
        SpringApplication.run(CategoriasSoapApplication.class, args);
    }
}
