package edu.unl.cc.web;

import edu.unl.cc.dominio.Usuario;
import edu.unl.cc.repositorio.UsuarioRepositorio;
import jakarta.enterprise.context.RequestScoped;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import java.io.Serializable;
import java.util.List;

/**
 * Pantalla principal de la Zona Arcade: catálogo de los 8 minijuegos y la
 * tienda donde se canjean los puntos de experiencia (XP) ganados jugando.
 * La partida en sí (rondas, aciertos, resultado) vive en
 * {@link ArcadeJuegoBean}, con su propio ámbito de vista.
 */
@Named
@RequestScoped
public class ArcadeBean implements Serializable {

    @Inject
    private AuthBean authBean;

    @Inject
    private UsuarioRepositorio usuarioRepositorio;

    /**
     * Catálogo fijo de los 8 minijuegos (id, nombre, descripción, ícono).
     * Clase normal (no "record"): el motor de Expression Language de
     * MyFaces empaquetado en esta versión de Liberty no resuelve bien las
     * propiedades de los records vía EL.
     */
    public static class JuegoInfo implements Serializable {
        private final String id;
        private final String nombre;
        private final String descripcion;
        private final String icono;

        public JuegoInfo(String id, String nombre, String descripcion, String icono) {
            this.id = id;
            this.nombre = nombre;
            this.descripcion = descripcion;
            this.icono = icono;
        }

        public String getId() {
            return id;
        }

        public String getNombre() {
            return nombre;
        }

        public String getDescripcion() {
            return descripcion;
        }

        public String getIcono() {
            return icono;
        }
    }

    private static final List<JuegoInfo> CATALOGO = List.of(
            new JuegoInfo("quiz", "Quiz Financiero", "Preguntas rápidas sobre finanzas personales", "quiz"),
            new JuegoInfo("adivina-gasto", "Adivina el Gasto", "Estima cuál de las opciones es la más realista", "gasto"),
            new JuegoInfo("reto-ahorro", "Reto de Ahorro", "Elige la mejor estrategia para ahorrar", "ahorro"),
            new JuegoInfo("simulador", "Simulador de Decisiones", "Enfrenta escenarios financieros cotidianos", "simulador"),
            new JuegoInfo("retos", "Retos Financieros", "Preguntas rápidas de cultura financiera", "retos"),
            new JuegoInfo("ciudad-ahorro", "Ciudad del Ahorro", "12 turnos construyendo tu ciudad ahorrando", "ciudad"),
            new JuegoInfo("laberinto-deudas", "Laberinto de Deudas", "Escapa de 12 salas esquivando trampas de deuda", "laberinto")
    );

    public List<JuegoInfo> getCatalogo() {
        return CATALOGO;
    }

    public Usuario getUsuario() {
        return authBean.getUsuarioActual();
    }

    public int getXp() {
        return getUsuario().getCuenta().getPuntosExperiencia();
    }

    public int getVidas() {
        return getUsuario().getCuenta().getVidasJuegoPrincipal();
    }

    public double getBono() {
        return getUsuario().getCuenta().getBonoCiudadAhorro();
    }

    // ---------- Tienda ----------

    public static class ItemTienda implements Serializable {
        private final String id;
        private final String nombre;
        private final String descripcion;
        private final int costoXp;

        public ItemTienda(String id, String nombre, String descripcion, int costoXp) {
            this.id = id;
            this.nombre = nombre;
            this.descripcion = descripcion;
            this.costoXp = costoXp;
        }

        public String getId() {
            return id;
        }

        public String getNombre() {
            return nombre;
        }

        public String getDescripcion() {
            return descripcion;
        }

        public int getCostoXp() {
            return costoXp;
        }
    }

    private static final List<ItemTienda> TIENDA = List.of(
            new ItemTienda("vida", "Vida extra", "+1 vida para el Laberinto de Deudas", 50),
            new ItemTienda("bono", "Bono de ahorro", "+$20 de bono en Ciudad del Ahorro", 80),
            new ItemTienda("recarga", "Recarga completa", "Restaura tus vidas a 3", 120)
    );

    public List<ItemTienda> getTienda() {
        return TIENDA;
    }

    public String canjear(String itemId) {
        Usuario usuario = getUsuario();
        int xpActual = usuario.getCuenta().getPuntosExperiencia();

        ItemTienda item = TIENDA.stream().filter(i -> i.getId().equals(itemId)).findFirst().orElse(null);
        if (item == null) {
            return null;
        }
        if (xpActual < item.getCostoXp()) {
            FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR,
                    "No tienes suficiente XP para canjear \"" + item.getNombre() + "\" (necesitas " + item.getCostoXp() + ").", null));
            return null;
        }

        usuario.getCuenta().ganarPuntos(-item.getCostoXp());
        switch (item.getId()) {
            case "vida" -> usuario.getCuenta().adicionarVidas(1);
            case "bono" -> usuario.getCuenta().incrementarBonoCiudadAhorro(20);
            case "recarga" -> usuario.getCuenta().adicionarVidas(3 - usuario.getCuenta().getVidasJuegoPrincipal());
            default -> { }
        }
        usuarioRepositorio.guardar(usuario);

        FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_INFO,
                "¡Canjeaste \"" + item.getNombre() + "\"!", null));
        return "/arcade.xhtml?faces-redirect=true";
    }
}
