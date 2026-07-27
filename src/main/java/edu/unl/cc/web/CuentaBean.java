package edu.unl.cc.web;

import edu.unl.cc.dominio.Presupuesto;
import edu.unl.cc.dominio.Usuario;
import edu.unl.cc.repositorio.UsuarioRepositorio;
import jakarta.enterprise.context.RequestScoped;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import java.io.Serializable;

/**
 * Todo lo relacionado con "mi propia cuenta": estadísticas del dashboard
 * (menu.xhtml), estadísticas de la página de presupuesto (presupuesto.xhtml)
 * y el formulario de autoconfiguración de saldo/límite
 * (cuenta/configForm.xhtml). Se fusionaron en un solo bean porque las tres
 * páginas leen exactamente los mismos datos (saldo, límite, gasto actual)
 * del mismo usuario logueado.
 */
@Named
@RequestScoped
public class CuentaBean implements Serializable {

    @Inject
    private AuthBean authBean;

    @Inject
    private UsuarioRepositorio usuarioRepositorio;

    private Usuario usuarioCache;

    // Campos del formulario de autoconfiguración (cuenta/configForm.xhtml)
    private Double saldoForm;
    private Double limiteForm;

    /**
     * "saldo" o "limite". Se llena desde un campo oculto en el formulario
     * (no desde un parámetro de ui:param pasado como argumento al método de
     * acción): eso no resultó confiable en este motor de EL — a veces el
     * valor no llegaba correctamente al momento de ejecutar la acción
     * (causaba que se guardara el límite como si fuera el saldo). Un campo
     * oculto normal sí pasa por el ciclo de vida estándar de JSF.
     */
    private String campo = "saldo";

    public String getCampo() {
        return campo;
    }

    public void setCampo(String campo) {
        this.campo = campo;
    }

    public boolean isEsLimite() {
        return "limite".equals(campo);
    }

    private Usuario usuario() {
        if (usuarioCache == null) {
            usuarioCache = authBean.getUsuarioActual();
        }
        return usuarioCache;
    }

    private Presupuesto presupuesto() {
        return usuario().getCuenta().getPresupuesto();
    }

    // ---------- Lectura (dashboard + presupuesto) ----------

    public Usuario getUsuario() {
        return usuario();
    }

    public double getSaldo() {
        return usuario().getCuenta().verSaldo();
    }

    public int getXp() {
        return usuario().getCuenta().getPuntosExperiencia();
    }

    public double getLimite() {
        return presupuesto().getLimiteMensual();
    }

    public double getGastado() {
        return presupuesto().getGastoActual();
    }

    public double getRestante() {
        return presupuesto().calcularRestante();
    }

    public boolean isSuperado() {
        return presupuesto().verificarLimite();
    }

    public long getPorcentaje() {
        double limite = getLimite();
        double gastado = getGastado();
        return limite > 0 ? Math.round(Math.min(100.0, (gastado / limite) * 100.0)) : 100L;
    }

    // ---------- Formulario: configurar saldo ----------

    /** Punto de entrada único para el botón "Guardar" de cuenta/configForm.xhtml. */
    public String guardar() {
        return isEsLimite() ? guardarLimite() : guardarSaldo();
    }

    public Double getSaldoForm() {
        if (saldoForm == null) {
            saldoForm = getSaldo();
        }
        return saldoForm;
    }

    public void setSaldoForm(Double saldoForm) {
        this.saldoForm = saldoForm;
    }

    private String guardarSaldo() {
        if (saldoForm == null) {
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, "Ingrese el saldo.", null));
            return null;
        }
        Usuario usuario = usuario();
        usuario.getCuenta().establecerSaldo(saldoForm);
        usuarioRepositorio.guardar(usuario);
        return "/menu.xhtml?faces-redirect=true";
    }

    // ---------- Formulario: configurar límite mensual ----------

    public Double getLimiteForm() {
        if (limiteForm == null) {
            limiteForm = getLimite();
        }
        return limiteForm;
    }

    public void setLimiteForm(Double limiteForm) {
        this.limiteForm = limiteForm;
    }

    private String guardarLimite() {
        if (limiteForm == null) {
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, "Ingrese el límite mensual.", null));
            return null;
        }
        Usuario usuario = usuario();
        double saldoDisponible = usuario.getCuenta().verSaldo();

        if (limiteForm > saldoDisponible) {
            FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR,
                    String.format("El límite no puede ser mayor a tu saldo disponible ($%.2f).", saldoDisponible), null));
            return null;
        }

        usuario.getCuenta().getPresupuesto().setLimiteMensual(limiteForm);
        usuarioRepositorio.guardar(usuario);
        return "/menu.xhtml?faces-redirect=true";
    }
}
