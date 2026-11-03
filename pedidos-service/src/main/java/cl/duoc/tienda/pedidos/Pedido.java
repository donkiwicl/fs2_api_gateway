package cl.duoc.tienda.pedidos;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Guarda solo el id del producto: la tabla de productos pertenece a otro servicio. */
@Entity
@Table(name = "pedidos")
public class Pedido {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String usuario;

    @Column(nullable = false)
    private Long productoId;

    @Column(nullable = false)
    private Integer cantidad;

    @Column(nullable = false)
    private Integer total;

    protected Pedido() {}

    public Pedido(String usuario, Long productoId, Integer cantidad, Integer total) {
        this.usuario = usuario;
        this.productoId = productoId;
        this.cantidad = cantidad;
        this.total = total;
    }

    public Long getId() { return id; }
    public String getUsuario() { return usuario; }
    public Long getProductoId() { return productoId; }
    public Integer getCantidad() { return cantidad; }
    public Integer getTotal() { return total; }
}
