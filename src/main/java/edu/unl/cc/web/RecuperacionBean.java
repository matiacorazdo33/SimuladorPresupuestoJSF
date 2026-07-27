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

@Named
@RequestScoped
public class RecuperacionBean implements Serializable {

    @Inject
    private UsuarioRepositorio usuarioRepositorio;

    private String token;
    private String contrasena;
    private String confirmarContrasena;

    private boolean exito;

    public void setToken(String token) {
        this.token = token;
    }

    public String getToken() {
        return token;
    }

    /**
     * Se recalcula en cada llamada (no se cachea) para que siga siendo
     * correcto incluso después de un postback del formulario, donde el
     * "token" se restaura vía un campo oculto y el ciclo de vida de JSF crea
     * una instancia nueva de este bean antes de que ese valor esté disponible.
     */
    public boolean isTokenValido() {
        return token != null && usuarioRepositorio.buscarPorTokenRecuperacion(token)
                .map(u -> u.tokenRecuperacionValido(token))
                .orElse(false);
    }

    public String guardar() {
        Optional<Usuario> candidato = usuarioRepositorio.buscarPorTokenRecuperacion(token);
        if (candidato.isEmpty() || !candidato.get().tokenRecuperacionValido(token)) {
            return null;
        }

        if (contrasena == null || !contrasena.equals(confirmarContrasena)) {
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, "Las contraseñas no coinciden", null));
            return null;
        }

        Usuario usuario = candidato.get();
        usuario.cambiarContrasena(contrasena);
        usuario.limpiarTokenRecuperacion();
        usuarioRepositorio.guardar(usuario);

        this.exito = true;
        return null;
    }

    public boolean isExito() {
        return exito;
    }

    public String getContrasena() {
        return contrasena;
    }

    public void setContrasena(String contrasena) {
        this.contrasena = contrasena;
    }

    public String getConfirmarContrasena() {
        return confirmarContrasena;
    }

    public void setConfirmarContrasena(String confirmarContrasena) {
        this.confirmarContrasena = confirmarContrasena;
    }
}
