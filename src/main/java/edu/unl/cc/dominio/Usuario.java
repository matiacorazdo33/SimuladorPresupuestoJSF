package edu.unl.cc.dominio;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "usuarios", uniqueConstraints = {
        @UniqueConstraint(columnNames = "nombre"),
        @UniqueConstraint(columnNames = "correo")
})
public class Usuario implements Serializable {

    @Id
    private String id;

    @Column(nullable = false)
    private String nombre;

    @Column(nullable = false)
    private String correo;

    @Column(nullable = false)
    private String contrasena;

    @OneToOne(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @JoinColumn(name = "cuenta_id")
    private CuentaJuego cuenta;

    @Enumerated(EnumType.STRING)
    private Rol rol;

    @Column(name = "token_recuperacion")
    private String tokenRecuperacion;

    @Column(name = "token_recuperacion_expira")
    private LocalDateTime tokenRecuperacionExpira;

    protected Usuario() {
        // Requerido por JPA
    }

    public Usuario(String nombre, String correo, String contrasena, double limitePresupuesto, double saldoInicial) {
        this(nombre, correo, contrasena, limitePresupuesto, saldoInicial, Rol.USUARIO);
    }

    public Usuario(String nombre, String correo, String contrasena, double limitePresupuesto, double saldoInicial, Rol rol) {
        this.id = UUID.randomUUID().toString();
        this.nombre = nombre;
        this.correo = correo;
        this.contrasena = contrasena;
        this.cuenta = new CuentaJuego(saldoInicial, limitePresupuesto);
        this.rol = (rol != null) ? rol : Rol.USUARIO;
    }

    public boolean iniciarSesion(String identificador, String contrasena) {
        return (identificador.equals(this.nombre) || identificador.equals(this.correo))
                && contrasena.equals(this.contrasena);
    }

    public String getId() {
        return this.id;
    }

    public String getNombre() {
        return this.nombre;
    }

    public String getCorreo() {
        return this.correo;
    }

    public CuentaJuego getCuenta() {
        return this.cuenta;
    }

    public Rol getRol() {
        return this.rol;
    }

    public boolean esAdmin() {
        return this.rol == Rol.ADMIN;
    }

    public void cambiarContrasena(String nuevaContrasena) {
        this.contrasena = nuevaContrasena;
    }

    /**
     * Genera un token de recuperación de un solo uso, válido por 30 minutos,
     * y lo guarda en el usuario (hay que persistirlo aparte con el repositorio).
     */
    public String generarTokenRecuperacion() {
        this.tokenRecuperacion = UUID.randomUUID().toString();
        this.tokenRecuperacionExpira = LocalDateTime.now().plusMinutes(30);
        return this.tokenRecuperacion;
    }

    public boolean tokenRecuperacionValido(String token) {
        return this.tokenRecuperacion != null
                && this.tokenRecuperacion.equals(token)
                && this.tokenRecuperacionExpira != null
                && this.tokenRecuperacionExpira.isAfter(LocalDateTime.now());
    }

    public void limpiarTokenRecuperacion() {
        this.tokenRecuperacion = null;
        this.tokenRecuperacionExpira = null;
    }

    @Override
    public String toString() {
        return String.format("Usuario[nombre=%s, correo=%s, %s]", this.nombre, this.correo, this.cuenta);
    }
}
