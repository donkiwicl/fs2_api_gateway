package cl.duoc.tienda.pedidos;

import io.swagger.v3.oas.annotations.Parameter;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.List;

/**
 * El usuario llega en headers que pone el gateway tras validar el JWT.
 * Confiar en ellos solo es seguro si este servicio NO se publica hacia internet.
 */
@RestController
@RequestMapping("/api/pedidos")
public class PedidoController {

    public record NuevoPedido(@NotNull Long productoId, @NotNull @Min(1) Integer cantidad) {}

    private final PedidoService service;

    public PedidoController(PedidoService service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Pedido crear(@Parameter(hidden = true) @RequestHeader("X-User-Name") String usuario,
                        @Valid @RequestBody NuevoPedido pedido) {
        return service.crear(usuario, pedido.productoId(), pedido.cantidad());
    }

    @GetMapping
    public List<Pedido> listar(@Parameter(hidden = true) @RequestHeader("X-User-Name") String usuario,
                               @Parameter(hidden = true) @RequestHeader("X-User-Roles") String roles) {
        return service.listar(usuario, Arrays.asList(roles.split(",")).contains("ADMIN"));
    }
}
