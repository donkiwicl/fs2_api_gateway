package cl.duoc.tienda.auth;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.List;

/** Unico lugar del sistema donde se FIRMAN tokens. */
@Service
public class JwtService {
    private final SecretKey key;
    private final String issuer;
    private final Duration duracion;

    public JwtService(@Value("${jwt.secret}") String secret,
                      @Value("${jwt.issuer}") String issuer,
                      @Value("${jwt.expiracion-minutos}") long minutos) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.issuer = issuer;
        this.duracion = Duration.ofMinutes(minutos);
    }

    public String generar(Usuario usuario) {
        Instant ahora = Instant.now();
        return Jwts.builder()
                .issuer(issuer)
                .subject(usuario.getNombre())
                .claim("roles", List.of(usuario.getRol()))
                .issuedAt(Date.from(ahora))
                .expiration(Date.from(ahora.plus(duracion)))
                .signWith(key)
                .compact();
    }

    public long segundosDeVida() {
        return duracion.toSeconds();
    }
}
