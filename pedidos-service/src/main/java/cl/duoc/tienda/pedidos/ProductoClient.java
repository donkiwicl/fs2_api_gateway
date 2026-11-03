package cl.duoc.tienda.pedidos;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;

/** Comunicacion REST entre microservicios (contenido de DSY1103). */
@Component
public class ProductoClient {

    public record ProductoDto(Long id, String nombre, Integer precio, Integer stock) {}

    private final RestClient restClient;

    public ProductoClient(@Value("${productos.url}") String url) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(2));
        factory.setReadTimeout(Duration.ofSeconds(3));
        this.restClient = RestClient.builder().baseUrl(url).requestFactory(factory).build();
    }

    public ProductoDto buscar(Long id) {
        try {
            return restClient.get().uri("/api/productos/{id}", id).retrieve().body(ProductoDto.class);
        } catch (HttpClientErrorException.NotFound e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El producto " + id + " no existe");
        } catch (ResourceAccessException | RestClientResponseException e) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "No se pudo consultar el catalogo");
        }
    }
}
