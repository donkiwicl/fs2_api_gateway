package cl.duoc.tienda.pedidos;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class PedidoService {
    private final PedidoRepository repository;
    private final ProductoClient productoClient;

    public PedidoService(PedidoRepository repository, ProductoClient productoClient) {
        this.repository = repository;
        this.productoClient = productoClient;
    }

    public Pedido crear(String usuario, Long productoId, int cantidad) {
        ProductoClient.ProductoDto producto = productoClient.buscar(productoId);
        if (producto.stock() < cantidad) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Stock insuficiente");
        }
        return repository.save(new Pedido(usuario, productoId, cantidad, producto.precio() * cantidad));
    }

    public List<Pedido> listar(String usuario, boolean esAdmin) {
        return esAdmin ? repository.findAll() : repository.findByUsuario(usuario);
    }
}
