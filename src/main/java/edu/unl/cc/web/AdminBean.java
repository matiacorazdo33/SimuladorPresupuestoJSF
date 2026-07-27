package edu.unl.cc.web;

import edu.unl.cc.dominio.Gasto;
import edu.unl.cc.dominio.Ingreso;
import edu.unl.cc.dominio.Transaccion;
import edu.unl.cc.dominio.Usuario;
import edu.unl.cc.repositorio.UsuarioRepositorio;
import jakarta.enterprise.context.RequestScoped;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import java.io.Serializable;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * Todo el panel de administración: estadísticas globales (vista
 * "dashboard"), lista de usuarios (vista "usuarios") y detalle/edición de
 * un usuario puntual (vista "detalle", con "id" en la URL). Las tres
 * comparten un solo bean porque admin/panel.xhtml es una única página que
 * cambia de contenido según el parámetro "vista".
 */
@Named
@RequestScoped
public class AdminBean implements Serializable {

    @Inject
    private AuthBean authBean;

    @Inject
    private UsuarioRepositorio usuarioRepositorio;

    // "id" del usuario a ver en detalle, llega por f:viewParam o campo oculto
    private String id;
    private Usuario objetivo;
    private Double saldo;
    private Double limitePresupuesto;

    public Usuario getUsuario() {
        return authBean.getUsuarioActual();
    }

    // ---------- Vista: dashboard (estadísticas globales) ----------

    public int getTotalUsuarios() {
        return usuarioRepositorio.listarTodos().size();
    }

    public double getSaldoTotal() {
        return usuarioRepositorio.listarTodos().stream()
                .mapToDouble(u -> u.getCuenta().verSaldo()).sum();
    }

    public double getSaldoPromedio() {
        List<Usuario> todos = usuarioRepositorio.listarTodos();
        return todos.isEmpty() ? 0.0 : getSaldoTotal() / todos.size();
    }

    public double getIngresosTotales() {
        return todasLasTransacciones()
                .filter(t -> t instanceof Ingreso)
                .mapToDouble(Transaccion::getMonto).sum();
    }

    public double getGastosTotales() {
        return todasLasTransacciones()
                .filter(t -> t instanceof Gasto)
                .mapToDouble(Transaccion::getMonto).sum();
    }

    public int getTransaccionesTotales() {
        return (int) todasLasTransacciones().count();
    }

    public String getUsuarioMayorSaldo() {
        return usuarioRepositorio.listarTodos().stream()
                .max(Comparator.comparingDouble(u -> u.getCuenta().verSaldo()))
                .map(Usuario::getNombre)
                .orElse("—");
    }

    private java.util.stream.Stream<Transaccion> todasLasTransacciones() {
        return usuarioRepositorio.listarTodos().stream()
                .flatMap(u -> u.getCuenta().getTodasLasTransacciones().stream());
    }

    // ---------- Vista: lista de usuarios ----------

    public List<Usuario> getUsuarios() {
        return usuarioRepositorio.listarTodos().stream()
                .sorted(Comparator.comparing(Usuario::getNombre, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    public String eliminarUsuarioDeLista(String idAEliminar) {
        Usuario admin = authBean.getUsuarioActual();
        if (idAEliminar.equals(admin.getId())) {
            FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR,
                    "No puedes eliminar tu propia cuenta de administrador.", null));
            return null;
        }
        Optional<Usuario> encontrado = usuarioRepositorio.buscarPorId(idAEliminar);
        if (encontrado.isEmpty()) {
            return "/admin/panel.xhtml?vista=usuarios&faces-redirect=true";
        }
        usuarioRepositorio.eliminar(idAEliminar);
        FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_INFO,
                "Usuario " + encontrado.get().getNombre() + " eliminado.", null));
        return "/admin/panel.xhtml?vista=usuarios&faces-redirect=true";
    }

    // ---------- Vista: detalle de un usuario ----------

    /**
     * El "id" llega por f:viewParam (GET) o por un campo oculto en el
     * formulario (POST). Se carga "objetivo" en el momento en que el valor
     * está disponible (no en @PostConstruct, que en un bean @RequestScoped
     * corre antes de que el valor llegue en un postback).
     */
    public void setId(String id) {
        this.id = id;
        if (id == null) {
            return;
        }
        objetivo = usuarioRepositorio.buscarPorId(id).orElse(null);
        if (objetivo != null && saldo == null) {
            saldo = objetivo.getCuenta().verSaldo();
            limitePresupuesto = objetivo.getCuenta().getPresupuesto().getLimiteMensual();
        }
    }

    public String getId() {
        return id;
    }

    public Usuario getObjetivo() {
        return objetivo;
    }

    public boolean isEsUnoMismo() {
        Usuario admin = getUsuario();
        return objetivo != null && admin != null && objetivo.getId().equals(admin.getId());
    }

    public List<Transaccion> getTransacciones() {
        return objetivo.getCuenta().getTodasLasTransacciones();
    }

    /** Inicial (mayúscula) del nombre de un usuario, para el avatar circular de la tabla. */
    public String inicialDe(Usuario u) {
        if (u == null || u.getNombre() == null || u.getNombre().isBlank()) {
            return "?";
        }
        return u.getNombre().substring(0, 1).toUpperCase();
    }

    public boolean esIngreso(Transaccion t) {
        return t instanceof Ingreso;
    }

    public String guardarEdicion() {
        objetivo.getCuenta().establecerSaldo(saldo);
        objetivo.getCuenta().getPresupuesto().setLimiteMensual(limitePresupuesto);
        usuarioRepositorio.guardar(objetivo);

        FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_INFO,
                "Datos de " + objetivo.getNombre() + " actualizados.", null));
        return "/admin/panel.xhtml?vista=detalle&id=" + id + "&faces-redirect=true";
    }

    public String eliminarMovimiento(Long transaccionId) {
        boolean eliminado = objetivo.getCuenta().eliminarTransaccion(transaccionId);
        if (eliminado) {
            usuarioRepositorio.guardar(objetivo);
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_INFO, "Movimiento eliminado.", null));
        } else {
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, "No se encontró el movimiento.", null));
        }
        return "/admin/panel.xhtml?vista=detalle&id=" + id + "&faces-redirect=true";
    }

    public String eliminarUsuarioObjetivo() {
        Usuario admin = getUsuario();
        if (objetivo.getId().equals(admin.getId())) {
            FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR,
                    "No puedes eliminar tu propia cuenta de administrador.", null));
            return null;
        }
        usuarioRepositorio.eliminar(objetivo.getId());
        return "/admin/panel.xhtml?vista=usuarios&faces-redirect=true";
    }

    public Double getSaldo() {
        return saldo;
    }

    public void setSaldo(Double saldo) {
        this.saldo = saldo;
    }

    public Double getLimitePresupuesto() {
        return limitePresupuesto;
    }

    public void setLimitePresupuesto(Double limitePresupuesto) {
        this.limitePresupuesto = limitePresupuesto;
    }
}
