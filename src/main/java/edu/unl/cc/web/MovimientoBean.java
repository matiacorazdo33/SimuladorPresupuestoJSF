package edu.unl.cc.web;

import edu.unl.cc.dominio.Categoria;
import edu.unl.cc.dominio.Prioridad;
import edu.unl.cc.dominio.Usuario;
import edu.unl.cc.repositorio.UsuarioRepositorio;
import jakarta.enterprise.context.RequestScoped;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import java.io.Serializable;
import java.time.LocalDate;

/**
 * Registrar un ingreso y registrar un gasto son casi el mismo formulario
 * (monto, fecha, categoría) con un campo extra distinto (fuente vs.
 * prioridad); por eso comparten un único bean. La página
 * movimientoForm.xhtml decide cuál de los dos es, vía el parámetro de URL
 * "tipo" (ingreso|gasto).
 */
@Named
@RequestScoped
public class MovimientoBean implements Serializable {

    @Inject
    private AuthBean authBean;

    @Inject
    private UsuarioRepositorio usuarioRepositorio;

    private Double monto;
    // String en formato "yyyy-MM-dd" a propósito: coincide exactamente con
    // lo que envía un <input type="date"> nativo del navegador, así no
    // hace falta ningún convertidor de fecha de JSF (fuente frecuente de
    // problemas en este proyecto).
    private String fecha = LocalDate.now().toString();
    private String categoria;
    private String fuente;
    private String prioridad = "MEDIA";

    /**
     * "ingreso" o "gasto". Se llena desde un campo oculto en el formulario
     * (no desde un parámetro de ui:param pasado como argumento al método de
     * acción): pasar una variable de ui:param como argumento de un método
     * de acción resultó no ser confiable en este motor de EL — a veces el
     * valor no llegaba correctamente al momento de ejecutar la acción, lo
     * que causaba que un gasto se guardara como ingreso. Un campo oculto
     * normal sí pasa por el ciclo de vida estándar de JSF (se actualiza en
     * la fase Update Model Values, igual que cualquier otro input).
     */
    private String tipo = "ingreso";

    public String getTipo() {
        return tipo;
    }

    public void setTipo(String tipo) {
        this.tipo = tipo;
    }

    public boolean isEsGasto() {
        return "gasto".equals(tipo);
    }

    public Prioridad[] getPrioridades() {
        return Prioridad.values();
    }

    /** Punto de entrada único para el botón "Guardar" del formulario. */
    public String guardar() {
        return isEsGasto() ? registrarGasto() : registrarIngreso();
    }

    private String registrarIngreso() {
        Usuario usuario = authBean.getUsuarioActual();
        usuario.getCuenta().registrarIngreso(
                monto,
                fecha,
                new Categoria(categoria),
                fuente
        );
        // Relación directa Usuario-Transaccion (además de Presupuesto-Transaccion)
        var nueva = usuario.getCuenta().getUltimaTransaccionRegistrada();
        if (nueva != null) {
            nueva.setUsuario(usuario);
        }
        usuarioRepositorio.guardar(usuario);
        return "/saldo.xhtml?faces-redirect=true";
    }

    private String registrarGasto() {
        Usuario usuario = authBean.getUsuarioActual();

        boolean registrado = usuario.getCuenta().registrarGasto(
                monto,
                fecha,
                new Categoria(categoria),
                Prioridad.desdeTexto(prioridad)
        );

        if (registrado) {
            var nueva = usuario.getCuenta().getUltimaTransaccionRegistrada();
            if (nueva != null) {
                nueva.setUsuario(usuario);
            }
            usuarioRepositorio.guardar(usuario);
            return "/saldo.xhtml?faces-redirect=true";
        }

        FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR,
                String.format("No puede registrar este gasto, supera su saldo disponible ($%.2f).", usuario.getCuenta().verSaldo()),
                null));
        return null;
    }

    public Double getMonto() {
        return monto;
    }

    public void setMonto(Double monto) {
        this.monto = monto;
    }

    public String getFecha() {
        return fecha;
    }

    public void setFecha(String fecha) {
        this.fecha = fecha;
    }

    public String getCategoria() {
        return categoria;
    }

    public void setCategoria(String categoria) {
        this.categoria = categoria;
    }

    public String getFuente() {
        return fuente;
    }

    public void setFuente(String fuente) {
        this.fuente = fuente;
    }

    public String getPrioridad() {
        return prioridad;
    }

    public void setPrioridad(String prioridad) {
        this.prioridad = prioridad;
    }
}
