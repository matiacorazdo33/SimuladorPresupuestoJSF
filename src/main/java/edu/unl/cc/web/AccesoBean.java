package edu.unl.cc.web;

import edu.unl.cc.dominio.Usuario;
import edu.unl.cc.repositorio.UsuarioRepositorio;
import jakarta.enterprise.context.RequestScoped;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import java.io.Serializable;
import java.util.Optional;

/**
 * Cubre las dos páginas previas al login (crear cuenta e iniciar sesión),
 * que comparten la misma pantalla de marca y un flujo de "paso 1 / paso 2".
 * acceso.xhtml decide cuál mostrar vía el parámetro de URL "modo"
 * (registro|login).
 */
@Named
@RequestScoped
public class AccesoBean implements Serializable {

    @Inject
    private UsuarioRepositorio usuarioRepositorio;

    @Inject
    private AuthBean authBean;

    @Inject
    private EmailService emailService;

    // Controla si se muestra el formulario de login o el de registro
    // (llega vía f:viewParam desde acceso.xhtml?modo=login|registro)
    private String modo = "login";

    public String getModo() {
        return modo;
    }

    public void setModo(String modo) {
        this.modo = modo;
    }

    public boolean isEsLogin() {
        return "login".equals(modo);
    }

    // ---------- Registro ----------

    private String nombre;
    private String correo;
    private String contrasenaRegistro;

    private boolean registrado = false;
    private String nombreRegistrado;

    public String registrar() {
        if (usuarioRepositorio.existeNombreOCorreo(nombre == null ? "" : nombre, correo == null ? "" : correo)) {
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, "Ese nombre de usuario o correo ya está registrado", null));
            return null;
        }

        // Las cuentas nuevas arrancan en $0; saldo y límite se configuran
        // después desde el dashboard.
        Usuario usuario = new Usuario(nombre.trim(), correo.trim(), contrasenaRegistro, 0.0, 0.0);
        usuarioRepositorio.guardar(usuario);

        this.registrado = true;
        this.nombreRegistrado = usuario.getNombre();
        return null;
    }

    // ---------- Login ----------

    private String identificador;
    private String contrasenaLogin;

    public String iniciarSesion() {
        if (authBean.isBloqueado()) {
            return "/estado.xhtml?tipo=bloqueado&faces-redirect=true";
        }

        Optional<Usuario> candidato = usuarioRepositorio.buscarPorIdentificador(identificador);
        boolean credencialesValidas = candidato.isPresent()
                && candidato.get().iniciarSesion(identificador, contrasenaLogin);

        if (credencialesValidas) {
            authBean.iniciarSesion(candidato.get());
            return "/menu.xhtml?faces-redirect=true";
        }

        boolean quedoBloqueado = authBean.registrarIntentoFallido();
        if (quedoBloqueado) {
            boolean correoEnviado = enviarCorreoRecuperacionSiCorresponde(candidato);
            authBean.marcarCorreoEnviado(correoEnviado);
            return "/estado.xhtml?tipo=bloqueado&faces-redirect=true";
        }

        FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_ERROR, "Credenciales incorrectas. Inténtelo de nuevo.", null));
        return null;
    }

    private boolean enviarCorreoRecuperacionSiCorresponde(Optional<Usuario> candidato) {
        if (candidato.isEmpty()) {
            return false;
        }
        Usuario usuario = candidato.get();
        String token = usuario.generarTokenRecuperacion();
        usuarioRepositorio.guardar(usuario);

        String enlace = EmailService.BASE_URL + "/recuperar.xhtml?token=" + token;
        emailService.enviarCorreoRecuperacion(usuario.getCorreo(), usuario.getNombre(), enlace);
        return true;
    }

    // ---------- Getters/setters ----------

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getCorreo() {
        return correo;
    }

    public void setCorreo(String correo) {
        this.correo = correo;
    }

    public String getContrasenaRegistro() {
        return contrasenaRegistro;
    }

    public void setContrasenaRegistro(String contrasenaRegistro) {
        this.contrasenaRegistro = contrasenaRegistro;
    }

    public boolean isRegistrado() {
        return registrado;
    }

    public String getNombreRegistrado() {
        return nombreRegistrado;
    }

    public String getIdentificador() {
        return identificador;
    }

    public void setIdentificador(String identificador) {
        this.identificador = identificador;
    }

    public String getContrasenaLogin() {
        return contrasenaLogin;
    }

    public void setContrasenaLogin(String contrasenaLogin) {
        this.contrasenaLogin = contrasenaLogin;
    }
}
