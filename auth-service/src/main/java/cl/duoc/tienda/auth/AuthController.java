package cl.duoc.tienda.auth;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    public record Credenciales(@NotBlank @Size(max = 50) String nombre,
                               @NotBlank @Size(min = 8, max = 72) String password) {}
    public record TokenResponse(String token, String tipo, long expiraEnSegundos, String rol) {}

    private final UsuarioRepository repository;
    private final JwtService jwtService;
    private final BCryptPasswordEncoder encoder;

    public AuthController(UsuarioRepository repository, JwtService jwtService, BCryptPasswordEncoder encoder) {
        this.repository = repository;
        this.jwtService = jwtService;
        this.encoder = encoder;
    }

    @PostMapping("/login")
    public TokenResponse login(@Valid @RequestBody Credenciales credenciales) {
        Usuario usuario = repository.findByNombre(credenciales.nombre())
                .filter(u -> encoder.matches(credenciales.password(), u.getPasswordHash()))
                // Mismo mensaje para usuario o clave incorrectos: no revela que cuentas existen.
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Credenciales incorrectas"));
        return respuesta(usuario);
    }

    @PostMapping("/registro")
    @ResponseStatus(HttpStatus.CREATED)
    public TokenResponse registro(@Valid @RequestBody Credenciales credenciales) {
        if (repository.existsByNombre(credenciales.nombre())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "El usuario ya existe");
        }
        // El rol no viene del cliente: nadie puede registrarse como ADMIN.
        Usuario usuario = repository.save(new Usuario(
                credenciales.nombre(), encoder.encode(credenciales.password()), "CLIENTE"));
        return respuesta(usuario);
    }

    private TokenResponse respuesta(Usuario usuario) {
        return new TokenResponse(jwtService.generar(usuario), "Bearer", jwtService.segundosDeVida(), usuario.getRol());
    }
}
