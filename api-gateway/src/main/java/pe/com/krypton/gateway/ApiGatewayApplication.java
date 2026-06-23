package pe.com.krypton.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * Puerta de entrada unica. Es una FACHADA: preserva los paths /api/** que ya
 * consume el frontend, asi el front solo cambia su base-url a este puerto (8094).
 *
 * Rutea por path:
 *   /api/products/** -> lb://CATALOGO   (descubierto por Eureka)
 *   /api/orders/**   -> lb://PEDIDOS    (descubierto por Eureka)
 *   /api/**          -> monolito        (auth, carrito, categorias, admin...)
 */
@EnableDiscoveryClient
@SpringBootApplication
public class ApiGatewayApplication {
    public static void main(String[] args) {
        SpringApplication.run(ApiGatewayApplication.class, args);
    }
}
