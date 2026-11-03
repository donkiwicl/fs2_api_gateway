package cl.duoc.tienda.gateway;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** El puerto 1 no tiene ningun servicio: simula un microservicio caido. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "PRODUCTOS_URL=http://localhost:1")
class FallbackIntegrationTest {

    @Value("${local.server.port}")
    int puerto;

    @Test
    void servicioCaidoDevuelve503ConMensajeClaro() throws Exception {
        HttpResponse<String> r = HttpClient.newHttpClient().send(
                HttpRequest.newBuilder(URI.create("http://localhost:" + puerto + "/api/productos")).build(),
                HttpResponse.BodyHandlers.ofString());
        assertEquals(503, r.statusCode());
        assertTrue(r.body().contains("productos no esta disponible"), r.body());
    }
}
