package pe.com.krypton.pedidos;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;

/**
 * Microservicio de Pedidos: checkout y gestión de órdenes del cliente.
 * Persiste en MongoDB (documento agregado: Order con OrderItems embebidos),
 * se registra en Eureka y reprecia los ítems llamando a catalogo-service vía Feign
 * (con circuit-breaker + fallback degradado).
 */
@SpringBootApplication
@EnableDiscoveryClient
@EnableFeignClients
public class PedidosApplication {

    public static void main(String[] args) {
        SpringApplication.run(PedidosApplication.class, args);
    }
}
