package cl.duoc.tienda.gateway;

import org.springframework.http.server.PathContainer;
import org.springframework.web.util.pattern.PathPattern;
import org.springframework.web.util.pattern.PathPatternParser;

import java.util.Set;

/**
 * Tabla de autorizacion del gateway. Regla por defecto: si algo no aparece aqui,
 * exige token (denegar por defecto es mas seguro que permitir por defecto).
 */
final class ReglasAcceso {

    enum Nivel { PUBLICO, AUTENTICADO, ADMIN }

    private static final PathPatternParser PARSER = new PathPatternParser();
    private static final PathPattern PRODUCTOS = PARSER.parse("/api/productos/**");
    private static final PathPattern DOCS = PARSER.parse("/docs/*");
    private static final PathPattern SWAGGER = PARSER.parse("/swagger-ui/**");
    private static final PathPattern API_DOCS = PARSER.parse("/v3/api-docs/**");
    private static final PathPattern FALLBACK = PARSER.parse("/fallback/**");
    private static final Set<String> LECTURA = Set.of("GET", "HEAD");

    private ReglasAcceso() {}

    static Nivel evaluar(String metodo, String ruta) {
        PathContainer path = PathContainer.parsePath(ruta);

        if ("POST".equals(metodo) && (ruta.equals("/api/auth/login") || ruta.equals("/api/auth/registro"))) {
            return Nivel.PUBLICO;
        }
        if (PRODUCTOS.matches(path)) {
            return LECTURA.contains(metodo) ? Nivel.PUBLICO : Nivel.ADMIN;
        }
        if (LECTURA.contains(metodo) && (DOCS.matches(path) || SWAGGER.matches(path) || API_DOCS.matches(path)
                || ruta.equals("/swagger-ui.html") || ruta.equals("/actuator/health"))) {
            return Nivel.PUBLICO;
        }
        if (FALLBACK.matches(path)) {
            return Nivel.PUBLICO;
        }
        return Nivel.AUTENTICADO;
    }

    /** Rechaza rutas ambiguas como /api/productos/../pedidos, que podrian burlar las reglas. */
    static boolean esRutaSospechosa(String ruta) {
        String r = ruta.toLowerCase();
        return r.contains("..") || r.contains("%2e") || r.contains("%2f") || r.contains("%5c")
                || r.contains("\\") || r.contains(";") || r.contains("//");
    }
}
