package cl.duoc.tienda.auth;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "usuarios")
public class Usuario {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String nombre;

    @Column(nullable = false)
    private String passwordHash;

    @Column(nullable = false)
    private String rol;

    protected Usuario() {}

    public Usuario(String nombre, String passwordHash, String rol) {
        this.nombre = nombre;
        this.passwordHash = passwordHash;
        this.rol = rol;
    }

    public String getNombre() { return nombre; }
    public String getPasswordHash() { return passwordHash; }
    public String getRol() { return rol; }
}
