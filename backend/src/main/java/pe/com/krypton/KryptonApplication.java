package pe.com.krypton;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableFeignClients
public class KryptonApplication {

    public static void main(String[] args) {
        SpringApplication.run(KryptonApplication.class, args);
    }
}
