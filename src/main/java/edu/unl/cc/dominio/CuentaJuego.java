package edu.unl.cc.dominio;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;

import java.io.Serializable;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

/**
 * Antes se llamaba "Cuenta". Se renombró a "CuentaJuego" porque mezclaba
 * dos responsabilidades bajo un nombre demasiado genérico: el saldo
 * financiero real (saldoDisponible) y los datos de gamificación de la
 * Zona Arcade (puntosExperiencia, vidasJuegoPrincipal, bonoCiudadAhorro).
 * El nombre nuevo deja claro que es la "cuenta" ligada al aspecto de
 * juego/recompensas, no solo un dato financiero plano.
 * <p>
 * Nota de compatibilidad: los nombres de los métodos (getPresupuesto(),
 * registrarIngreso(), etc.) se mantuvieron sin cambios a propósito, para
 * que el resto del código (Usuario, los beans web) no tuviera que
 * modificarse — solo cambió el nombre de la CLASE.
 */
@Entity
@Table(name = "cuentas_juego")
public class CuentaJuego implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private double saldoDisponible;
    private int puntosExperiencia;

    /**
     * Antes era un único Presupuesto (uno para toda la vida de la cuenta).
     * Ahora es un historial: se crea un Presupuesto nuevo cada mes
     * (identificado por "mes", formato "yyyy-MM"), conservando los de
     * meses anteriores para consulta.
     */
    @OneToMany(mappedBy = "cuenta", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @OrderBy("mes DESC")
    private List<Presupuesto> presupuestos = new ArrayList<>();

    private int vidasJuegoPrincipal;
    private double bonoCiudadAhorro;

    /** No se persiste: referencia rápida a la última transacción creada, para que la capa web pueda etiquetarla con el Usuario dueño. */
    @Transient
    private Transaccion ultimaTransaccionRegistrada;

    protected CuentaJuego() {
        // Requerido por JPA
    }

    public CuentaJuego(double saldoInicial, double limitePresupuesto) {
        this.saldoDisponible = (saldoInicial < 0) ? 0.0 : saldoInicial;
        this.puntosExperiencia = 0;
        this.vidasJuegoPrincipal = 3;
        this.bonoCiudadAhorro = 0.0;

        double limiteValidado = (limitePresupuesto > this.saldoDisponible) ? this.saldoDisponible : limitePresupuesto;
        Presupuesto inicial = new Presupuesto(limiteValidado);
        inicial.setMes(mesActual());
        inicial.setCuenta(this);
        this.presupuestos.add(inicial);
    }

    public Long getId() {
        return this.id;
    }

    private static String mesActual() {
        return YearMonth.now().toString(); // "2026-07"
    }

    public void registrarIngreso(double monto, String fecha, Categoria categoria, String fuente) {
        Ingreso nuevoIngreso = new Ingreso(monto, fecha, categoria, fuente);
        this.saldoDisponible += nuevoIngreso.getMonto();
        getPresupuesto().agregarTransaccion(nuevoIngreso);
        this.ultimaTransaccionRegistrada = nuevoIngreso;
    }

    public boolean registrarGasto(double monto, String fecha, Categoria categoria, Prioridad prioridad) {
        if (monto > this.saldoDisponible) {
            return false;
        }
        Gasto nuevoGasto = new Gasto(monto, fecha, categoria, prioridad);
        this.saldoDisponible -= nuevoGasto.getMonto();
        getPresupuesto().agregarTransaccion(nuevoGasto);
        this.ultimaTransaccionRegistrada = nuevoGasto;
        return true;
    }

    /** Última transacción creada por registrarIngreso/registrarGasto (para que la capa web la asocie al Usuario dueño). */
    public Transaccion getUltimaTransaccionRegistrada() {
        return this.ultimaTransaccionRegistrada;
    }

    /**
     * Elimina un movimiento del historial (buscándolo en cualquier mes, no
     * solo el actual) y revierte su efecto sobre el saldo: si era un
     * gasto, el dinero "vuelve"; si era un ingreso, se descuenta.
     */
    public boolean eliminarTransaccion(Long transaccionId) {
        for (Presupuesto p : this.presupuestos) {
            Transaccion transaccion = p.buscarTransaccion(transaccionId);
            if (transaccion != null) {
                if (transaccion instanceof Ingreso) {
                    this.saldoDisponible -= transaccion.getMonto();
                    if (this.saldoDisponible < 0) {
                        this.saldoDisponible = 0.0;
                    }
                } else if (transaccion instanceof Gasto) {
                    this.saldoDisponible += transaccion.getMonto();
                }
                return p.eliminarTransaccion(transaccionId);
            }
        }
        return false;
    }

    public void ganarPuntos(int puntos) {
        this.puntosExperiencia += puntos;
    }

    /**
     * Ajuste directo del saldo, sin pasar por una transacción de ingreso/gasto.
     * Lo usa tanto el propio usuario (para configurar su saldo inicial desde
     * el dashboard) como el panel de administración.
     */
    public void establecerSaldo(double nuevoSaldo) {
        this.saldoDisponible = (nuevoSaldo < 0) ? 0.0 : nuevoSaldo;
    }

    public double verSaldo() {
        return this.saldoDisponible;
    }

    public int getPuntosExperiencia() {
        return this.puntosExperiencia;
    }

    /**
     * Devuelve el presupuesto del MES ACTUAL, creándolo automáticamente
     * (heredando el límite del mes más reciente anterior, o 0 si es el
     * primero) si todavía no existe uno para este mes. El nombre del
     * método se mantuvo igual a propósito, así el resto del código
     * (dashboard, panel de administración, etc.) sigue funcionando sin
     * cambios — antes devolvía "el" presupuesto único, ahora devuelve "el
     * presupuesto de este mes".
     */
    public Presupuesto getPresupuesto() {
        String mesActual = mesActual();
        for (Presupuesto p : this.presupuestos) {
            if (mesActual.equals(p.getMes())) {
                return p;
            }
        }
        double limiteHeredado = this.presupuestos.isEmpty() ? 0.0 : this.presupuestos.get(0).getLimiteMensual();
        Presupuesto nuevo = new Presupuesto(limiteHeredado);
        nuevo.setMes(mesActual);
        nuevo.setCuenta(this);
        this.presupuestos.add(0, nuevo);
        return nuevo;
    }

    /** Historial completo de presupuestos (uno por mes), más reciente primero. */
    public List<Presupuesto> getHistorialPresupuestos() {
        return this.presupuestos;
    }

    /** Todas las transacciones de todos los meses (no solo el actual) — usado por el historial completo. */
    public List<Transaccion> getTodasLasTransacciones() {
        List<Transaccion> todas = new ArrayList<>();
        for (Presupuesto p : this.presupuestos) {
            todas.addAll(p.getTransacciones());
        }
        return todas;
    }

    public int getVidasJuegoPrincipal() {
        return this.vidasJuegoPrincipal;
    }

    public void adicionarVidas(int cantidad) {
        this.vidasJuegoPrincipal += cantidad;
        if (this.vidasJuegoPrincipal < 0) {
            this.vidasJuegoPrincipal = 0;
        }
    }

    public double getBonoCiudadAhorro() {
        return this.bonoCiudadAhorro;
    }

    public void incrementarBonoCiudadAhorro(double cantidad) {
        this.bonoCiudadAhorro += cantidad;
    }

    @Override
    public String toString() {
        return String.format(
                "CuentaJuego[saldo=$%.2f, xp=%d, vidas=%d, bonoCiudadAhorro=$%.2f, presupuestos=%d]",
                this.saldoDisponible, this.puntosExperiencia, this.vidasJuegoPrincipal,
                this.bonoCiudadAhorro, this.presupuestos.size()
        );
    }
}
