package edu.unl.cc.dominio;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorColumn;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.io.Serializable;

@Entity
@Table(name = "transacciones")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "tipo_transaccion")
public abstract class Transaccion implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private double monto;
    private String fecha;

    @Embedded
    private Categoria categoria;

    @ManyToOne
    @JoinColumn(name = "presupuesto_id")
    private Presupuesto presupuesto;

    /**
     * Relación directa con el usuario dueño de esta transacción, separada
     * de la cadena Usuario→CuentaJuego→Presupuesto→Transaccion. Se fija
     * desde la capa web justo después de crear la transacción (ver
     * CuentaJuego.getUltimaTransaccionRegistrada() y su uso en
     * MovimientoBean), ya que esta clase no tiene por sí sola una
     * referencia hacia arriba hasta Usuario.
     */
    @ManyToOne
    @JoinColumn(name = "usuario_id")
    private Usuario usuario;

    /**
     * "ENTRADA" o "SALIDA", según el tipo real de movimiento. Es una
     * columna real en la base de datos (no un valor calculado solo en
     * Java): cada subclase (Ingreso/Gasto) la fija en su propio
     * constructor llamando a setTipo(...). Se deja "nullable" (sin
     * restricción NOT NULL) a propósito, para que Hibernate pueda agregar
     * esta columna sin fallar si ya existían filas de antes en la tabla.
     */
    @Column(name = "tipo")
    private String tipo;

    protected Transaccion() {
        // Requerido por JPA
    }

    protected Transaccion(double monto, String fecha, Categoria categoria) {
        setMonto(monto); // Reutiliza la validación defensiva del setter
        this.fecha = validarCadena(fecha, "Fecha No Definida");
        this.categoria = (categoria != null) ? categoria : new Categoria(null);
    }

    public Long getId() {
        return this.id;
    }

    public String getTipo() {
        return this.tipo;
    }

    protected void setTipo(String tipo) {
        this.tipo = tipo;
    }

    public double getMonto() {
        return this.monto;
    }

    public void setMonto(double monto) {
        if (monto < 0) {
            this.monto = 0.0;
        } else {
            this.monto = monto;
        }
    }

    private String validarCadena(String texto, String valorPorDefecto) {
        if (texto == null || texto.trim().isEmpty()) {
            return valorPorDefecto;
        }
        return texto;
    }

    public String getFecha() {
        return this.fecha;
    }

    public void setFecha(String fecha) {
        this.fecha = validarCadena(fecha, "Fecha No Definida");
    }

    public Categoria getCategoria() {
        return this.categoria;
    }

    public void setCategoria(Categoria categoria) {
        this.categoria = (categoria != null) ? categoria : new Categoria(null);
    }

    public Presupuesto getPresupuesto() {
        return this.presupuesto;
    }

    public void setPresupuesto(Presupuesto presupuesto) {
        this.presupuesto = presupuesto;
    }

    public Usuario getUsuario() {
        return this.usuario;
    }

    public void setUsuario(Usuario usuario) {
        this.usuario = usuario;
    }

    @Override
    public String toString() {
        return String.format("[%s] Categoría: %s | Monto: $%.2f", this.fecha, this.categoria, this.monto);
    }
}

