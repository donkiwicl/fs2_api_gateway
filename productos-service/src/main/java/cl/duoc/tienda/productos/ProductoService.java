package cl.duoc.tienda.productos;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class ProductoService {
    private final ProductoRepository repository;

    public ProductoService(ProductoRepository repository) {
        this.repository = repository;
    }

    public List<Producto> listar() {
        return repository.findAll();
    }

    public Producto buscar(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Producto no encontrado"));
    }

    public Producto crear(Producto producto) {
        return repository.save(new Producto(producto.getNombre(), producto.getPrecio(), producto.getStock()));
    }

    @Transactional
    public Producto actualizar(Long id, Producto datos) {
        Producto producto = buscar(id);
        producto.setNombre(datos.getNombre());
        producto.setPrecio(datos.getPrecio());
        producto.setStock(datos.getStock());
        return producto;
    }

    public void eliminar(Long id) {
        repository.delete(buscar(id));
    }
}
