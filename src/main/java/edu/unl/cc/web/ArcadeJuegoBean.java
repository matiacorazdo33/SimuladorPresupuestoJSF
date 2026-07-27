package edu.unl.cc.web;

import edu.unl.cc.dominio.Usuario;
import edu.unl.cc.repositorio.UsuarioRepositorio;
import jakarta.enterprise.inject.spi.CDI;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Ejecuta una partida de cualquiera de los 8 minijuegos. Todos comparten el
 * mismo mecanismo (una serie de rondas de opción múltiple), lo que evita
 * tener una clase e implementación completamente distinta por minijuego;
 * lo que cambia entre ellos es solo el banco de preguntas y, en dos casos
 * ("Ciudad del Ahorro" y "Laberinto de Deudas"), una recompensa extra al
 * ganar (bono de ahorro / vida) que refleja su mecánica original.
 */
@Named
@ViewScoped
public class ArcadeJuegoBean implements Serializable {

    @Inject
    private AuthBean authBean;

    /**
     * UsuarioRepositorio se obtiene explícitamente por llamada (no se
     * inyecta como campo) a propósito: es un bean @Dependent cuyo
     * EntityManager (también @Dependent) no es formalmente serializable, y
     * este bean es @ViewScoped (ámbito "passivating" que exige que toda su
     * cadena de dependencias sea serializable). Inyectarlo como campo
     * rompía el despliegue con un WELD-001413 (UnserializableDependencyException).
     */
    private UsuarioRepositorio usuarioRepositorio() {
        return CDI.current().select(UsuarioRepositorio.class).get();
    }

    /**
     * Antes era un "record" de Java. Se cambió a clase normal con getters
     * clásicos (getEnunciado(), etc.) porque el motor de Expression
     * Language de MyFaces empaquetado en esta versión de Liberty no
     * resuelve correctamente las propiedades de los records vía EL
     * (#{ronda.enunciado} fallaba con "PropertyNotFoundException" aunque
     * el método enunciado() sí existía).
     */
    public static class Ronda implements Serializable {
        private final String enunciado;
        private final List<String> opciones;
        private final int indiceCorrecta;

        public Ronda(String enunciado, List<String> opciones, int indiceCorrecta) {
            this.enunciado = enunciado;
            this.opciones = opciones;
            this.indiceCorrecta = indiceCorrecta;
        }

        public String getEnunciado() {
            return enunciado;
        }

        public List<String> getOpciones() {
            return opciones;
        }

        public int getIndiceCorrecta() {
            return indiceCorrecta;
        }
    }

    /** Par (índice, texto) usado para alimentar f:selectItems con un solo h:selectOneRadio por ronda. */
    public static class OpcionIndexada implements Serializable {
        private final int indice;
        private final String texto;

        public OpcionIndexada(int indice, String texto) {
            this.indice = indice;
            this.texto = texto;
        }

        public int getIndice() {
            return indice;
        }

        public String getTexto() {
            return texto;
        }
    }

    private String juegoId;
    private String nombreJuego;
    private List<Ronda> rondas;

    private int indiceActual = 0;
    private int aciertos = 0;
    private Integer opcionSeleccionada;
    private boolean respondida = false;
    private boolean correctaUltimaRonda = false;

    private boolean terminado = false;
    private boolean gano = false;
    private int xpGanado = 0;
    private String recompensaExtra;

    /**
     * El "id" del juego llega por f:viewParam. Se dispara la carga del
     * banco de preguntas desde este setter (no desde @PostConstruct):
     * en un bean @ViewScoped, el orden entre la creación del bean
     * (@PostConstruct) y la llamada al setter de f:viewParam no está
     * garantizado en todas las implementaciones de JSF, así que es más
     * seguro cargar los datos en el momento en que el valor realmente
     * llega.
     */
    public void setJuegoId(String juegoId) {
        this.juegoId = juegoId;
        if (juegoId != null && rondas == null) {
            cargarBanco(juegoId);
        }
    }

    public String getJuegoId() {
        return juegoId;
    }

    // ---------- Estado expuesto a la vista ----------

    public String getNombreJuego() {
        asegurarCargado();
        return nombreJuego;
    }

    public int getNumeroRonda() {
        asegurarCargado();
        return indiceActual + 1;
    }

    public int getTotalRondas() {
        asegurarCargado();
        return rondas.size();
    }

    public Ronda getRondaActual() {
        asegurarCargado();
        return terminado ? null : rondas.get(indiceActual);
    }

    public List<OpcionIndexada> getOpcionesActuales() {
        Ronda ronda = getRondaActual();
        if (ronda == null) {
            return List.of();
        }
        List<OpcionIndexada> resultado = new ArrayList<>();
        List<String> opciones = ronda.getOpciones();
        for (int i = 0; i < opciones.size(); i++) {
            resultado.add(new OpcionIndexada(i, opciones.get(i)));
        }
        return resultado;
    }

    public String getTextoRespuestaCorrecta() {
        Ronda ronda = getRondaActual();
        return ronda == null ? "" : ronda.getOpciones().get(ronda.getIndiceCorrecta());
    }

    private void asegurarCargado() {
        if (rondas == null) {
            cargarBanco(juegoId);
        }
    }

    public Integer getOpcionSeleccionada() {
        return opcionSeleccionada;
    }

    public void setOpcionSeleccionada(Integer opcionSeleccionada) {
        this.opcionSeleccionada = opcionSeleccionada;
    }

    public boolean isRespondida() {
        return respondida;
    }

    public boolean isCorrectaUltimaRonda() {
        return correctaUltimaRonda;
    }

    public boolean isTerminado() {
        return terminado;
    }

    public boolean isGano() {
        return gano;
    }

    public int getAciertos() {
        return aciertos;
    }

    public int getXpGanado() {
        return xpGanado;
    }

    public String getRecompensaExtra() {
        return recompensaExtra;
    }

    public long getPorcentajeAvance() {
        asegurarCargado();
        return Math.round(((double) indiceActual / rondas.size()) * 100);
    }

    // ---------- Acciones ----------

    public void responder() {
        asegurarCargado();
        if (opcionSeleccionada == null || respondida) {
            return;
        }
        Ronda ronda = rondas.get(indiceActual);
        correctaUltimaRonda = opcionSeleccionada == ronda.getIndiceCorrecta();
        if (correctaUltimaRonda) {
            aciertos++;
        }
        respondida = true;
    }

    public void siguiente() {
        asegurarCargado();
        indiceActual++;
        opcionSeleccionada = null;
        respondida = false;

        if (indiceActual >= rondas.size()) {
            finalizarPartida();
        }
    }

    private void finalizarPartida() {
        terminado = true;
        double porcentaje = (double) aciertos / rondas.size();
        gano = porcentaje >= 0.7;

        Usuario usuario = authBean.getUsuarioActual();
        xpGanado = aciertos * 10;
        usuario.getCuenta().ganarPuntos(xpGanado);

        recompensaExtra = null;
        if (gano && "ciudad-ahorro".equals(juegoId)) {
            usuario.getCuenta().incrementarBonoCiudadAhorro(15.0);
            recompensaExtra = "+ $15.00 de bono de ahorro";
        } else if ("laberinto-deudas".equals(juegoId)) {
            if (gano) {
                usuario.getCuenta().adicionarVidas(1);
                recompensaExtra = "+ 1 vida";
            } else {
                usuario.getCuenta().adicionarVidas(-1);
                recompensaExtra = "− 1 vida (te atrapó una trampa de deuda)";
            }
        }

        usuarioRepositorio().guardar(usuario);
    }

    // ---------- Bancos de preguntas ----------

    private void cargarBanco(String idParam) {
        String id = (idParam == null || idParam.isBlank()) ? "quiz" : idParam;
        this.juegoId = id;

        Map<String, String> nombres = Map.of(
                "quiz", "Quiz Financiero",
                "adivina-gasto", "Adivina el Gasto",
                "reto-ahorro", "Reto de Ahorro",
                "simulador", "Simulador de Decisiones",
                "retos", "Retos Financieros",
                "ciudad-ahorro", "Ciudad del Ahorro",
                "laberinto-deudas", "Laberinto de Deudas"
        );
        nombreJuego = nombres.getOrDefault(id, "Minijuego");

        rondas = new ArrayList<>(switch (id) {
            case "adivina-gasto" -> bancoAdivinaGasto();
            case "reto-ahorro" -> bancoRetoAhorro();
            case "simulador" -> bancoSimulador();
            case "retos" -> bancoRetos();
            case "ciudad-ahorro" -> bancoCiudadAhorro();
            case "laberinto-deudas" -> bancoLaberinto();
            default -> bancoQuiz();
        });

        // "Ciudad del Ahorro" y "Laberinto de Deudas" tienen una narrativa
        // secuencial (Turno 1..8, Sala 1..8 salida) que debe respetarse;
        // los demás son preguntas sueltas, así que sí se barajan para
        // variar el orden en cada partida.
        boolean esNarrativoSecuencial = "ciudad-ahorro".equals(id) || "laberinto-deudas".equals(id);
        if (!esNarrativoSecuencial) {
            Collections.shuffle(rondas);
        }
    }

    private List<Ronda> bancoQuiz() {
        return List.of(
                new Ronda("¿Qué es un presupuesto?", List.of(
                        "Un plan de ingresos y gastos", "Un tipo de préstamo", "Una tarjeta de crédito", "Un impuesto"), 0),
                new Ronda("¿Qué significa \"pagar de contado\"?", List.of(
                        "Pagar a plazos", "Pagar el total de una vez", "No pagar", "Pagar con tarjeta de crédito"), 1),
                new Ronda("¿Qué es el interés compuesto?", List.of(
                        "Interés que se cobra una sola vez", "Interés que se calcula sobre el capital y los intereses acumulados",
                        "Un tipo de multa", "Un descuento por pago anticipado"), 1),
                new Ronda("Un fondo de emergencia sirve para...", List.of(
                        "Comprar lujos", "Cubrir gastos imprevistos", "Pagar vacaciones", "Invertir en bolsa"), 1),
                new Ronda("¿Qué es la inflación?", List.of(
                        "La subida generalizada de precios", "La bajada de impuestos", "El aumento del salario", "Un tipo de ahorro"), 0),
                new Ronda("Diversificar tus ahorros significa...", List.of(
                        "Ponerlo todo en un solo lugar", "Repartirlo en distintas opciones para reducir el riesgo",
                        "Gastarlo todo", "Guardarlo en efectivo únicamente"), 1),
                new Ronda("¿Qué es una deuda \"buena\"?", List.of(
                        "La que financia algo que genera valor a futuro (ej. estudios)", "Cualquier préstamo",
                        "La que no se paga nunca", "Un regalo"), 0),
                new Ronda("El \"score\" o historial crediticio refleja...", List.of(
                        "Tu edad", "Qué tan confiable eres pagando deudas", "Tu salario exacto", "Tus ahorros totales"), 1)
        );
    }

    private List<Ronda> bancoAdivinaGasto() {
        return List.of(
                new Ronda("¿Cuál de estos es un gasto FIJO típico?", List.of(
                        "Renta mensual", "Salida al cine", "Regalo de cumpleaños", "Comida en restaurante"), 0),
                new Ronda("¿Cuál de estos es un gasto VARIABLE?", List.of(
                        "Pago del internet", "Alimentación semanal", "Cuota del seguro anual", "Renta"), 1),
                new Ronda("¿Cuál de estas categorías suele ser el mayor gasto mensual de un hogar?", List.of(
                        "Entretenimiento", "Vivienda", "Ropa", "Suscripciones"), 1),
                new Ronda("¿Qué gasto es más fácil de reducir rápidamente?", List.of(
                        "Renta", "Suscripciones de streaming", "Impuesto predial", "Colegiatura"), 1),
                new Ronda("Un gasto \"hormiga\" es...", List.of(
                        "Un gasto grande pero pequeño en frecuencia", "Un pequeño gasto diario que suma mucho al mes",
                        "Un gasto anual", "Un ahorro automático"), 1),
                new Ronda("¿Cuál de estos NO es un gasto esencial?", List.of(
                        "Alimentación", "Salud", "Suscripción a videojuegos premium", "Vivienda"), 2)
        );
    }

    private List<Ronda> bancoRetoAhorro() {
        return List.of(
                new Ronda("¿Cuál es una buena meta inicial de ahorro?", List.of(
                        "Ahorrar el 1% de tus ingresos", "Ahorrar entre el 10% y el 20% de tus ingresos",
                        "No ahorrar hasta ganar más", "Ahorrar solo si sobra dinero"), 1),
                new Ronda("La regla \"págate a ti mismo primero\" significa...", List.of(
                        "Gastar primero y ahorrar lo que sobre", "Apartar el ahorro apenas recibes tu ingreso, antes de gastar",
                        "Pedir prestado para ahorrar", "Ahorrar solo al final del año"), 1),
                new Ronda("¿Qué estrategia ayuda más a ahorrar sin esfuerzo?", List.of(
                        "Ahorro automático programado", "Recordar hacerlo manualmente cada mes",
                        "Ahorrar solo si te acuerdas", "Esperar un bono"), 0),
                new Ronda("Si tienes una meta de ahorro a corto plazo, ¿dónde conviene guardarla?", List.of(
                        "En inversiones de alto riesgo", "En una cuenta de fácil acceso y bajo riesgo",
                        "En efectivo debajo del colchón", "En criptomonedas volátiles"), 1),
                new Ronda("¿Cuál de estas es una \"trampa\" común al intentar ahorrar?", List.of(
                        "Automatizar el ahorro", "Comprar por impulso antes de ahorrar",
                        "Tener metas claras", "Revisar tu presupuesto"), 1)
        );
    }

    private List<Ronda> bancoSimulador() {
        return List.of(
                new Ronda("Te suben el sueldo. ¿Qué es más recomendable?", List.of(
                        "Aumentar tus gastos al mismo ritmo", "Aumentar tu ahorro junto con tus gastos",
                        "Gastarlo todo de inmediato", "Pedir un préstamo adicional"), 1),
                new Ronda("Tienes una deuda con interés alto y algo de ahorro. ¿Qué priorizar?", List.of(
                        "Pagar la deuda de interés alto primero", "Ignorar la deuda", "Invertir todo el ahorro en bolsa",
                        "Pedir otra tarjeta de crédito"), 0),
                new Ronda("Un imprevisto médico te cuesta dinero. ¿Qué fuente usar primero?", List.of(
                        "Tu fondo de emergencia", "Un préstamo de interés alto", "Vender tus inversiones a pérdida", "Otra tarjeta de crédito"), 0),
                new Ronda("Quieres comprar algo caro que no necesitas urgente. ¿Qué hacer?", List.of(
                        "Comprarlo de inmediato a crédito", "Esperar y evaluar si realmente lo necesitas, ahorrando para ello",
                        "Pedir prestado a un amigo", "Usar el fondo de emergencia"), 1),
                new Ronda("Te ofrecen una inversión con \"ganancia garantizada altísima\". ¿Qué hacer?", List.of(
                        "Invertir todo de inmediato", "Desconfiar: las ganancias altas garantizadas suelen ser estafas",
                        "Pedir un préstamo para invertir más", "Invertir sin investigar"), 1)
        );
    }

    private List<Ronda> bancoRetos() {
        return List.of(
                new Ronda("¿Qué documento resume tus ingresos y gastos de un periodo?", List.of(
                        "Presupuesto", "Contrato", "Factura", "Cheque"), 0),
                new Ronda("¿Qué es más riesgoso, en general?", List.of(
                        "Una cuenta de ahorro", "Invertir todo en una sola acción", "Un fondo de emergencia", "Pagar deudas"), 1),
                new Ronda("¿Qué significa \"vivir por encima de tus posibilidades\"?", List.of(
                        "Gastar menos de lo que ganas", "Gastar más de lo que ganas", "Ahorrar todo tu ingreso", "Invertir sabiamente"), 1),
                new Ronda("¿Qué es un \"activo\"?", List.of(
                        "Algo que te genera valor o ingresos", "Una deuda", "Un gasto fijo", "Un impuesto"), 0),
                new Ronda("¿Qué es un \"pasivo\" en finanzas personales?", List.of(
                        "Algo que te genera ingresos", "Una obligación que te cuesta dinero", "Un ahorro", "Una inversión rentable"), 1)
        );
    }

    private List<Ronda> bancoCiudadAhorro() {
        return List.of(
                new Ronda("Turno 1: llega tu primer ingreso del mes. ¿Qué construyes primero?", List.of(
                        "Una tienda de lujos", "Tu fondo de emergencia", "Un casino", "Nada, lo gastas todo"), 1),
                new Ronda("Turno 2: te ofrecen invertir en el \"barrio de las deudas fáciles\". ¿Qué haces?", List.of(
                        "Aceptas sin revisar los términos", "Investigas y comparas antes de decidir", "Rechazas todo tipo de crédito para siempre", "Pides el doble"), 1),
                new Ronda("Turno 3: tienes un excedente. ¿Cómo lo usas para tu ciudad?", List.of(
                        "Lo gastas en decoración innecesaria", "Lo destinas a ahorro o inversión de tu ciudad", "Lo prestas sin garantías", "Lo escondes sin registrar"), 1),
                new Ronda("Turno 4: un vecino te pide prestado. ¿Qué es más prudente?", List.of(
                        "Prestar todos tus ahorros", "Evaluar si puedes prestar sin comprometer tu fondo de emergencia", "Negarte a ayudar siempre", "Prestar sin ningún acuerdo"), 1),
                new Ronda("Turno 5: sube el costo de los servicios de tu ciudad. ¿Qué haces?", List.of(
                        "Ajustas tu presupuesto", "Ignoras el aumento", "Dejas de pagar servicios", "Te endeudas para cubrirlo sin plan"), 0),
                new Ronda("Turno 6: te llega un bono inesperado. ¿Qué es más sano para tu ciudad?", List.of(
                        "Gastarlo todo en un solo día", "Repartirlo entre ahorro, algo de gusto y metas", "Prestarlo sin condiciones", "Guardarlo sin ningún plan"), 1),
                new Ronda("Turno 7: se acerca una fecha de pago importante. ¿Qué haces?", List.of(
                        "Lo pagas a tiempo con lo presupuestado", "Lo ignoras", "Pides un préstamo de emergencia sin comparar opciones", "Dejas de comer para pagarlo"), 0),
                new Ronda("Turno 8 (final): revisas el balance de tu ciudad. ¿Qué indica una ciudad financieramente sana?", List.of(
                        "Deudas más altas que ahorros", "Un fondo de emergencia sólido y deudas bajo control", "Cero ahorro pero muchos lujos", "Gastar todo el excedente siempre"), 1)
        );
    }

    private List<Ronda> bancoLaberinto() {
        return List.of(
                new Ronda("Sala 1: el Monstruo del Interés Alto bloquea la puerta. ¿Cómo lo evitas?", List.of(
                        "Aceptando cualquier préstamo que te ofrezca", "Comparando tasas antes de aceptar un crédito",
                        "Pidiendo el préstamo más grande posible", "Cerrando los ojos y firmando"), 1),
                new Ronda("Sala 2: encuentras una tarjeta de crédito brillante. ¿Qué haces?", List.of(
                        "La usas sin límite", "La usas solo si puedes pagar el total a fin de mes",
                        "La regalas a un desconocido", "La ignoras aunque la necesites de verdad"), 1),
                new Ronda("Sala 3: el Fantasma de los Pagos Atrasados aparece. ¿Cómo lo esquivas?", List.of(
                        "Programando recordatorios y pagando a tiempo", "Ignorando las fechas de pago",
                        "Pagando solo cuando te acuerdes", "Dejando de revisar tus cuentas"), 0),
                new Ronda("Sala 4: hay una trampa de \"solo paga el mínimo\". ¿Qué sabes de ella?", List.of(
                        "Pagar el mínimo no genera más intereses", "Pagar solo el mínimo alarga la deuda y aumenta el interés pagado",
                        "Es la mejor estrategia siempre", "No tiene ningún costo extra"), 1),
                new Ronda("Sala 5: el Dragón del Sobreendeudamiento custodia el tesoro. ¿Cómo lo derrotas?", List.of(
                        "Pidiendo más deudas para pagar las anteriores", "Haciendo un plan realista de pago y evitando deuda nueva",
                        "Ignorando todas tus deudas", "Declarándote en quiebra sin evaluar opciones"), 1),
                new Ronda("Sala 6: encuentras un cofre de \"refinanciamiento\". ¿Cuándo conviene abrirlo?", List.of(
                        "Nunca es útil", "Cuando te consigue una tasa de interés mejor y un plan más manejable",
                        "Solo para gastar más", "Siempre, sin comparar condiciones"), 1),
                new Ronda("Sala 7: una trampa de \"compra ahora, paga después\" se activa. ¿Qué es más sano?", List.of(
                        "Usarla para todo sin control", "Usarla solo si ya tienes el dinero disponible para pagar a tiempo",
                        "Usarla para lo que no puedes pagar nunca", "Ignorar el plazo de pago"), 1),
                new Ronda("Sala 8 (salida): para escapar del laberinto, ¿cuál es la clave?", List.of(
                        "Acumular toda la deuda posible", "Gastar solo lo que puedes pagar y priorizar deudas de interés alto",
                        "No revisar nunca tus estados de cuenta", "Pedir prestado para pagar otro préstamo indefinidamente"), 1)
        );
    }
}
