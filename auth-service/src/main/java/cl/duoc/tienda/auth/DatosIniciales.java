package cl.duoc.tienda.auth;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/** Usuarios de demostracion para clase. No usar estas cuentas fuera del laboratorio. */
@Configuration
public class DatosIniciales {

    @Bean
    BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    CommandLineRunner cargarUsuarios(UsuarioRepository repository, BCryptPasswordEncoder encoder) {
        return args -> {
            repository.save(new Usuario("admin", encoder.encode("admin12345"), "ADMIN"));
            repository.save(new Usuario("cliente", encoder.encode("cliente12345"), "CLIENTE"));
        };
    }
}
