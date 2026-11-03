package cl.duoc.tienda.productos;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DatosIniciales {
    @Bean
    CommandLineRunner cargarProductos(ProductoRepository repository) {
        return args -> {
            repository.save(new Producto("Teclado", 25000, 10));
            repository.save(new Producto("Mouse", 12000, 3));
        };
    }
}
