package pe.com.krypton.eureka;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.eureka.server.EnableEurekaServer;

/**
 * Servidor de descubrimiento. Los micros se registran acá y se encuentran entre
 * si por nombre logico (lb://CATALOGO, lb://PEDIDOS) en vez de URLs fijas.
 *
 * Orden de arranque (respetar SIEMPRE): Bases -> Eureka -> micros -> Gateway.
 */
@EnableEurekaServer
@SpringBootApplication
public class EurekaServerApplication {
    public static void main(String[] args) {
        SpringApplication.run(EurekaServerApplication.class, args);
    }
}
