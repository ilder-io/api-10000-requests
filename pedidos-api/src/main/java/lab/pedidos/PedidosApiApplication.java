package lab.pedidos;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@SpringBootApplication
@EnableCaching
public class PedidosApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(PedidosApiApplication.class, args);
    }
}
