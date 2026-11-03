package cl.duoc.tienda.gateway;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * Punto unico de autenticacion y autorizacion.
 * Se registra en FiltrosConfig; corre antes de que el gateway enrute la peticion.
 */
public class SeguridadFilter extends OncePerRequestFilter {

    static final String HEADER_USUARIO = "X-User-Name";
    static final String HEADER_ROLES = "X-User-Roles";

    private final JwtValidator jwtValidator;

    public SeguridadFilter(JwtValidator jwtValidator) {
        this.jwtValidator = jwtValidator;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {

        String ruta = request.getRequestURI();
        if (ReglasAcceso.esRutaSospechosa(ruta)) {
            error(response, 400, "Ruta no valida");
            return;
        }

        ReglasAcceso.Nivel nivel = ReglasAcceso.evaluar(request.getMethod(), ruta);
        JwtValidator.Usuario usuario = extraerToken(request).flatMap(jwtValidator::validar).orElse(null);

        if (nivel != ReglasAcceso.Nivel.PUBLICO && usuario == null) {
            response.setHeader(HttpHeaders.WWW_AUTHENTICATE, "Bearer");
            error(response, 401, "Token ausente, invalido o expirado");
            return;
        }
        if (nivel == ReglasAcceso.Nivel.ADMIN && !usuario.roles().contains("ADMIN")) {
            error(response, 403, "Se requiere el rol ADMIN");
            return;
        }

        // Los X-User-* que envia el cliente se descartan SIEMPRE, incluso en rutas publicas:
        // de lo contrario cualquiera podria hacerse pasar por otro usuario.
        Map<String, String> confiables = new HashMap<>();
        if (usuario != null) {
            confiables.put(HEADER_USUARIO, usuario.nombre());
            confiables.put(HEADER_ROLES, String.join(",", usuario.roles()));
        }
        chain.doFilter(new RequestConHeaders(request, confiables), response);
    }

    private java.util.Optional<String> extraerToken(HttpServletRequest request) {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header != null && header.startsWith("Bearer ")) {
            return java.util.Optional.of(header.substring(7));
        }
        return java.util.Optional.empty();
    }

    private void error(HttpServletResponse response, int status, String mensaje) throws IOException {
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write("{\"status\":" + status + ",\"mensaje\":\"" + mensaje + "\"}");
    }

    /** Oculta los X-User-* originales y agrega los que el gateway verifico. */
    private static final class RequestConHeaders extends HttpServletRequestWrapper {
        private static final Set<String> PROTEGIDOS = Set.of(HEADER_USUARIO.toLowerCase(), HEADER_ROLES.toLowerCase());
        private final Map<String, String> confiables = new HashMap<>();

        RequestConHeaders(HttpServletRequest request, Map<String, String> confiables) {
            super(request);
            confiables.forEach((k, v) -> this.confiables.put(k.toLowerCase(), v));
        }

        @Override
        public String getHeader(String name) {
            String clave = name.toLowerCase();
            if (PROTEGIDOS.contains(clave)) {
                return confiables.get(clave);
            }
            return super.getHeader(name);
        }

        @Override
        public Enumeration<String> getHeaders(String name) {
            String clave = name.toLowerCase();
            if (PROTEGIDOS.contains(clave)) {
                String valor = confiables.get(clave);
                return valor == null ? Collections.emptyEnumeration() : Collections.enumeration(List.of(valor));
            }
            return super.getHeaders(name);
        }

        @Override
        public Enumeration<String> getHeaderNames() {
            Set<String> nombres = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
            Collections.list(super.getHeaderNames()).stream()
                    .filter(n -> !PROTEGIDOS.contains(n.toLowerCase()))
                    .forEach(nombres::add);
            nombres.addAll(confiables.keySet());
            return Collections.enumeration(nombres);
        }
    }
}
