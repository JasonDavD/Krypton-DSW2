package pe.com.krypton.catalogo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * Microservicio de Catálogo: expone la lectura pública de productos
 * (lista/filtros y detalle) detrás del API Gateway. Se registra en Eureka.
 */
@SpringBootApplication
@EnableDiscoveryClient
public class CatalogoApplication {

    public static void main(String[] args) {
        SpringApplication.run(CatalogoApplication.class, args);
    }
}
