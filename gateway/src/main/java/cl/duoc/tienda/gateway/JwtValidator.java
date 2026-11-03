package cl.duoc.tienda.gateway;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;

/** Verifica firma, emisor y expiracion del token. El gateway no crea tokens: solo los valida. */
@Component
public class JwtValidator {

    public record Usuario(String nombre, List<String> roles) {}

    private final SecretKey key;
    private final String issuer;

    public JwtValidator(@Value("${jwt.secret}") String secret, @Value("${jwt.issuer}") String issuer) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.issuer = issuer;
    }

    public Optional<Usuario> validar(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(key)
                    .requireIssuer(issuer)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            List<?> roles = claims.get("roles", List.class);
            return Optional.of(new Usuario(
                    claims.getSubject(),
                    roles == null ? List.of() : roles.stream().map(String::valueOf).toList()));
        } catch (JwtException | IllegalArgumentException e) {
            return Optional.empty();
        }
    }
}
