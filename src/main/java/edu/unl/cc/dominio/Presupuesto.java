package edu.unl.cc.dominio;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "presupuestos")
public class Presupuesto implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private double limiteMensual;

    /**
     * Mes al que corresponde este presupuesto, formato "yyyy-MM" (ej.
     * "2026-07"). Presupuesto es mensual: se crea un registro nuevo cada
     * mes en vez de reutilizar siempre el mismo (ver
     * CuentaJuego.getPresupuesto()), así queda un historial consultable.
     */
    @Column(name = "mes")
    private String mes;

    /** A qué cuenta (usuario) pertenece este presupuesto mensual. */
    @ManyToOne
    @JoinColumn(name = "cuenta_id")
    private CuentaJuego cuenta;

    // Valor calculado a partir de las transacciones; no se persiste como columna.
    @Transient
    private double gastoActual;

    @OneToMany(mappedBy = "presupuesto", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @OrderBy("id ASC")
    private List<Transaccion> transacciones = new ArrayList<>();

    protected Presupuesto() {
        // Requerido por JPA
    }

    public Presupuesto(double limiteMensual) {
        setLimiteMensual(limiteMensual); // Reutiliza validación del setter
        this.gastoActual = 0.0;
        this.transacciones = new ArrayList<>();
    }

    public Presupuesto(double limiteMensual, double saldoDisponible) {
        double limiteValidado = (limiteMensual > saldoDisponible) ? saldoDisponible : limiteMensual;
        setLimiteMensual(limiteValidado);
        this.gastoActual = 0.0;
        this.transacciones = new ArrayList<>();
    }

    public Long getId() {
        return this.id;
    }

    public String getMes() {
        return this.mes;
    }

    public void setMes(String mes) {
        this.mes = mes;
    }

    public CuentaJuego getCuenta() {
        return this.cuenta;
    }

    public void setCuenta(CuentaJuego cuenta) {
        this.cuenta = cuenta;
    }

    public void actualizarGastoActual() {
        double acumulado = 0.0;
        for (Transaccion transaccion : this.transacciones) {
            if (transaccion instanceof Gasto) {
                acumulado += ((Gasto) transaccion).calcularGasto();
            }
        }
        this.gastoActual = acumulado;
    }

    public double calcularRestante() {
        actualizarGastoActual();
        return this.limiteMensual - this.gastoActual;
    }

    public boolean verificarLimite() {
        actualizarGastoActual();
        return this.gastoActual > this.limiteMensual;
    }

    public void agregarTransaccion(Transaccion transaccion) {
        if (transaccion != null) {
            transaccion.setPresupuesto(this);
            this.transacciones.add(transaccion);
            actualizarGastoActual();
        }
    }

    public Transaccion buscarTransaccion(Long transaccionId) {
        if (transaccionId == null) {
            return null;
        }
        for (Transaccion t : this.transacciones) {
            if (transaccionId.equals(t.getId())) {
                return t;
            }
        }
        return null;
    }

    public boolean eliminarTransaccion(Long transaccionId) {
        boolean removido = this.transacciones.removeIf(t -> transaccionId != null && transaccionId.equals(t.getId()));
        if (removido) {
            actualizarGastoActual();
        }
        return removido;
    }

    public double getLimiteMensual() {
        return this.limiteMensual;
    }

    public void setLimiteMensual(double limiteMensual) {
        if (limiteMensual < 0) {
            this.limiteMensual = 0.0;
        } else {
            this.limiteMensual = limiteMensual;
        }
    }

    public void setLimiteMensual(double limiteMensual, double saldoDisponible) {
        double limiteValidado = (limiteMensual > saldoDisponible) ? saldoDisponible : limiteMensual;
        setLimiteMensual(limiteValidado);
    }

    public double getGastoActual() {
        actualizarGastoActual();
        return this.gastoActual;
    }

    public List<Transaccion> getTransacciones() {
        return this.transacciones;
    }

    public void setTransacciones(List<Transaccion> transacciones) {
        if (transacciones != null) {
            this.transacciones = transacciones;
            actualizarGastoActual();
        }
    }

    @Override
    public String toString() {
        actualizarGastoActual();
        return String.format("Presupuesto[límite=$%.2f, gastado=$%.2f, restante=$%.2f, transacciones=%d]",
                this.limiteMensual, this.gastoActual, calcularRestante(), this.transacciones.size());
    }
}
