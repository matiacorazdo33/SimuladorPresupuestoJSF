package edu.unl.cc.web;

import edu.unl.cc.dominio.Usuario;
import edu.unl.cc.repositorio.UsuarioRepositorio;
import jakarta.enterprise.context.SessionScoped;
import jakarta.enterprise.inject.spi.CDI;
import jakarta.faces.context.FacesContext;
import jakarta.inject.Named;

import java.io.Serializable;

/**
 * Sesión de autenticación: quién está logueado, intentos fallidos (máx. 3),
 * bloqueo, correo de recuperación, y datos rápidos del usuario que necesita
 * la plantilla común (sidebar/avatar) en cualquier página protegida —
 * absorbe lo que antes eran LogoutBean y EstadoCuentaBean, ya que son
 * responsabilidades pequeñas y muy ligadas a "quién está logueado".
 * <p>
 * Nota: UsuarioRepositorio se obtiene explícitamente por llamada (no se
 * inyecta como campo) a propósito. Al ser @Dependent y este bean
 * @SessionScoped, una inyección de campo quedaría "pegada" a la sesión
 * completa junto con su EntityManager, arriesgando lecturas obsoletas
 * (caché de primer nivel) entre distintas peticiones de la misma sesión.
 */
@Named
@SessionScoped
public class AuthBean implements Serializable {

    public static final int INTENTOS_MAXIMOS = 3;

    private String usuarioId;
    private int intentos = 0;
    private boolean bloqueado = false;
    private boolean correoRecuperacionEnviado = false;

    private UsuarioRepositorio usuarioRepositorio() {
        return CDI.current().select(UsuarioRepositorio.class).get();
    }

    // ---------- Estado de sesión ----------

    public boolean isAutenticado() {
        return usuarioId != null;
    }

    public boolean isBloqueado() {
        return bloqueado;
    }

    public int getIntentosRestantes() {
        return INTENTOS_MAXIMOS - intentos;
    }

    public boolean isCorreoRecuperacionEnviado() {
        return correoRecuperacionEnviado;
    }

    public void iniciarSesion(Usuario usuario) {
        this.usuarioId = usuario.getId();
        this.intentos = 0;
    }

    /** @return true si con este intento se llegó al límite (cuenta bloqueada) */
    public boolean registrarIntentoFallido() {
        intentos++;
        if (intentos >= INTENTOS_MAXIMOS) {
            bloqueado = true;
            return true;
        }
        return false;
    }

    public void marcarCorreoEnviado(boolean enviado) {
        this.correoRecuperacionEnviado = enviado;
    }

    /** Botón "Cerrar sesión" del sidebar/avatar: invalida la sesión HTTP y vuelve al login. */
    public String cerrarSesionYRedirigir() {
        this.usuarioId = null;
        this.intentos = 0;
        this.bloqueado = false;
        this.correoRecuperacionEnviado = false;
        FacesContext.getCurrentInstance().getExternalContext().invalidateSession();
        return "/acceso.xhtml?modo=login&faces-redirect=true";
    }

    // ---------- Navegación de páginas de acceso ----------

    /** Usado por index.xhtml para decidir a dónde redirigir al entrar a la app. */
    public String redirigirInicio() {
        return isAutenticado() ? "/menu.xhtml?faces-redirect=true" : "/acceso.xhtml?faces-redirect=true";
    }

    /** Usado por acceso.xhtml: si ya hay sesión iniciada, saltar directo al menú. */
    public String redirigirSiYaAutenticado() {
        return isAutenticado() ? "/menu.xhtml?faces-redirect=true" : null;
    }

    /** Usado por acceso.xhtml: si la sesión está bloqueada, ir a la página de estado. */
    public String redirigirSiBloqueado() {
        return isBloqueado() ? "/estado.xhtml?tipo=bloqueado&faces-redirect=true" : null;
    }

    // ---------- Datos del usuario para la plantilla (sidebar/avatar) ----------

    /** Devuelve el usuario autenticado actual, releído de la base de datos. */
    public Usuario getUsuarioActual() {
        if (usuarioId == null) {
            return null;
        }
        return usuarioRepositorio().buscarPorId(usuarioId).orElse(null);
    }

    /** true si el usuario superó su límite mensual; activa el modo emergencia en toda la UI. */
    public boolean isEmergencia() {
        Usuario usuario = getUsuarioActual();
        return usuario != null && usuario.getCuenta().getPresupuesto().verificarLimite();
    }

    public String getInicial() {
        Usuario usuario = getUsuarioActual();
        if (usuario == null || usuario.getNombre() == null || usuario.getNombre().isBlank()) {
            return "?";
        }
        return usuario.getNombre().substring(0, 1).toUpperCase();
    }

    public double getSaldoActual() {
        Usuario usuario = getUsuarioActual();
        return usuario != null ? usuario.getCuenta().verSaldo() : 0.0;
    }
}
