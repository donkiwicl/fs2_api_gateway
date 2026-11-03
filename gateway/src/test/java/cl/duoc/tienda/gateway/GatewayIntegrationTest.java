package cl.duoc.tienda.gateway;

import com.sun.net.httpserver.HttpServer;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Levanta el gateway real contra un microservicio simulado que registra lo que recibe. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class GatewayIntegrationTest {

    private static final String SECRETO = "secreto-solo-para-pruebas-de-integracion-123456";
    private static final AtomicReference<String> USUARIO_RECIBIDO = new AtomicReference<>();
    private static final AtomicReference<String> RUTA_RECIBIDA = new AtomicReference<>();
    private static final HttpServer STUB = iniciarStub();

    @Value("${local.server.port}")
    int puerto;

    private final HttpClient http = HttpClient.newHttpClient();

    private static HttpServer iniciarStub() {
        try {
            HttpServer server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
            server.createContext("/", exchange -> {
                USUARIO_RECIBIDO.set(exchange.getRequestHeaders().getFirst("X-User-Name"));
                RUTA_RECIBIDA.set(exchange.getRequestURI().getPath());
                exchange.getRequestBody().readAllBytes();
                byte[] body = "{\"ok\":true}".getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().add("Content-Type", "application/json");
                exchange.sendResponseHeaders(200, body.length);
                exchange.getResponseBody().write(body);
                exchange.close();
            });
            server.start();
            return server;
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    @DynamicPropertySource
    static void propiedades(DynamicPropertyRegistry registry) {
        String url = "http://localhost:" + STUB.getAddress().getPort();
        registry.add("AUTH_URL", () -> url);
        registry.add("PRODUCTOS_URL", () -> url);
        registry.add("PEDIDOS_URL", () -> url);
        registry.add("JWT_SECRET", () -> SECRETO);
    }

    @AfterAll
    static void detenerStub() {
        STUB.stop(0);
    }

    @BeforeEach
    void limpiar() {
        USUARIO_RECIBIDO.set(null);
        RUTA_RECIBIDA.set(null);
    }

    private String token(String secreto, String rol, Instant expira) {
        return Jwts.builder()
                .issuer("tienda-auth").subject("ana").claim("roles", List.of(rol))
                .expiration(Date.from(expira))
                .signWith(Keys.hmacShaKeyFor(secreto.getBytes(StandardCharsets.UTF_8)))
                .compact();
    }

    private String tokenValido(String rol) {
        return token(SECRETO, rol, Instant.now().plusSeconds(60));
    }

    private HttpRequest.Builder peticion(String ruta) {
        return HttpRequest.newBuilder(URI.create("http://localhost:" + puerto + ruta));
    }

    private HttpResponse<String> enviar(HttpRequest.Builder peticion) throws Exception {
        return http.send(peticion.build(), HttpResponse.BodyHandlers.ofString());
    }

    @Test
    void rutaPublicaPasaSinToken() throws Exception {
        assertEquals(200, enviar(peticion("/api/productos")).statusCode());
        assertEquals("/api/productos", RUTA_RECIBIDA.get());
    }

    @Test
    void rutaProtegidaSinTokenDevuelve401YNoLlegaAlServicio() throws Exception {
        assertEquals(401, enviar(peticion("/api/pedidos")).statusCode());
        assertNull(RUTA_RECIBIDA.get());
    }

    @Test
    void tokenFirmadoConOtroSecretoDevuelve401() throws Exception {
        String falso = token("otro-secreto-que-no-es-el-del-gateway-1234567890", "ADMIN", Instant.now().plusSeconds(60));
        assertEquals(401, enviar(peticion("/api/pedidos").header("Authorization", "Bearer " + falso)).statusCode());
    }

    @Test
    void tokenExpiradoDevuelve401() throws Exception {
        String expirado = token(SECRETO, "ADMIN", Instant.now().minusSeconds(60));
        assertEquals(401, enviar(peticion("/api/pedidos").header("Authorization", "Bearer " + expirado)).statusCode());
    }

    @Test
    void clienteNoPuedeCrearProductos() throws Exception {
        HttpResponse<String> r = enviar(peticion("/api/productos")
                .header("Authorization", "Bearer " + tokenValido("CLIENTE"))
                .POST(HttpRequest.BodyPublishers.ofString("{}")));
        assertEquals(403, r.statusCode());
        assertNull(RUTA_RECIBIDA.get());
    }

    @Test
    void adminPuedeCrearProductos() throws Exception {
        HttpResponse<String> r = enviar(peticion("/api/productos")
                .header("Authorization", "Bearer " + tokenValido("ADMIN"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString("{}")));
        assertEquals(200, r.statusCode());
    }

    @Test
    void gatewayPropagaElUsuarioDelToken() throws Exception {
        enviar(peticion("/api/pedidos").header("Authorization", "Bearer " + tokenValido("CLIENTE")));
        assertEquals("ana", USUARIO_RECIBIDO.get());
    }

    @Test
    void headerFalsificadoSeDescartaConToken() throws Exception {
        enviar(peticion("/api/pedidos")
                .header("Authorization", "Bearer " + tokenValido("CLIENTE"))
                .header("X-User-Name", "admin"));
        assertEquals("ana", USUARIO_RECIBIDO.get());
    }

    @Test
    void headerFalsificadoSeDescartaEnRutaPublica() throws Exception {
        enviar(peticion("/api/productos").header("X-User-Name", "admin"));
        assertEquals("/api/productos", RUTA_RECIBIDA.get());
        assertNull(USUARIO_RECIBIDO.get());
    }

    @Test
    void rutaConPuntosEsRechazada() throws Exception {
        HttpResponse<String> r = enviar(peticion("/api/productos/%2e%2e/pedidos"));
        assertEquals(400, r.statusCode());
        assertNull(RUTA_RECIBIDA.get());
    }

    @Test
    void docsSoloExponeElJsonOpenApi() throws Exception {
        assertEquals(200, enviar(peticion("/docs/productos")).statusCode());
        assertEquals("/v3/api-docs", RUTA_RECIBIDA.get());
    }

    @Test
    void docsNoPermiteLlegarAOtrosEndpoints() throws Exception {
        HttpResponse<String> r = enviar(peticion("/docs/productos/api/productos")
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString("{}")));
        assertTrue(r.statusCode() == 401, "esperaba 401 y fue " + r.statusCode());
        assertNull(RUTA_RECIBIDA.get());
    }

    @Test
    void preflightCorsNoExigeToken() throws Exception {
        HttpResponse<String> r = enviar(peticion("/api/pedidos")
                .method("OPTIONS", HttpRequest.BodyPublishers.noBody())
                .header("Origin", "http://localhost:5173")
                .header("Access-Control-Request-Method", "POST"));
        assertEquals(200, r.statusCode());
        assertEquals("http://localhost:5173", r.headers().firstValue("Access-Control-Allow-Origin").orElse(null));
    }

    @Test
    void origenNoAutorizadoEsRechazado() throws Exception {
        HttpResponse<String> r = enviar(peticion("/api/pedidos")
                .method("OPTIONS", HttpRequest.BodyPublishers.noBody())
                .header("Origin", "http://sitio-malicioso.example")
                .header("Access-Control-Request-Method", "POST"));
        assertEquals(403, r.statusCode());
    }
}
