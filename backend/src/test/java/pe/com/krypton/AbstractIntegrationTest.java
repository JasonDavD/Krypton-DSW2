package pe.com.krypton;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Import;
import org.testcontainers.containers.MySQLContainer;
import pe.com.krypton.support.CategoriasSoapTestConfig;

/**
 * Base para tests de integración: levanta UN MySQL real (Testcontainers,
 * mysql:8 — paridad con prod) compartido entre TODAS las subclases.
 *
 * Patrón "singleton container": el contenedor se arranca a mano en el bloque
 * estático y NO se anota con @Testcontainers/@Container. Así vive durante todo
 * el JVM de tests (Ryuk lo limpia al salir) y NO se apaga entre clases de test
 * — evitar eso es justamente el pitfall de compartir un container con @Testcontainers.
 *
 * @ServiceConnection cablea el datasource al contenedor automáticamente.
 */
@SpringBootTest
@Import(CategoriasSoapTestConfig.class) // categorias por SOAP → fake en memoria en integración
abstract class AbstractIntegrationTest {

    @ServiceConnection
    static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8");

    static {
        MYSQL.start();
    }
}
