package edu.unl.cc.web;

import edu.unl.cc.dominio.Ingreso;
import edu.unl.cc.dominio.Transaccion;
import edu.unl.cc.dominio.Usuario;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.RequestScoped;
import jakarta.faces.context.FacesContext;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Named
@RequestScoped
public class HistorialBean implements Serializable {

    private static final DateTimeFormatter FORMATO_MES =
            DateTimeFormatter.ofPattern("MMMM yyyy", new Locale("es", "ES"));

    @Inject
    private AuthBean authBean;

    private Usuario usuario;
    private YearMonth mes;
    private List<CalendarDia> calendarioDias;

    @PostConstruct
    public void init() {
        usuario = authBean.getUsuarioActual();

        String mesParam = FacesContext.getCurrentInstance()
                .getExternalContext().getRequestParameterMap().get("mes");
        try {
            mes = (mesParam != null && !mesParam.isBlank()) ? YearMonth.parse(mesParam) : YearMonth.now();
        } catch (DateTimeParseException e) {
            mes = YearMonth.now();
        }

        calendarioDias = construirCalendario(mes, getTransacciones());
    }

    public Usuario getUsuario() {
        return usuario;
    }

    public double getSaldo() {
        return usuario.getCuenta().verSaldo();
    }

    public int getXp() {
        return usuario.getCuenta().getPuntosExperiencia();
    }

    public List<Transaccion> getTransacciones() {
        return usuario.getCuenta().getTodasLasTransacciones();
    }

    public List<CalendarDia> getCalendarioDias() {
        return calendarioDias;
    }

    public String getMesTexto() {
        String texto = mes.format(FORMATO_MES);
        return Character.toUpperCase(texto.charAt(0)) + texto.substring(1);
    }

    public String getMesAnterior() {
        return mes.minusMonths(1).toString();
    }

    public String getMesSiguiente() {
        return mes.plusMonths(1).toString();
    }

    public boolean esIngreso(Transaccion t) {
        return t instanceof Ingreso;
    }

    private List<CalendarDia> construirCalendario(YearMonth mes, List<Transaccion> transacciones) {
        Map<Integer, String> estadoPorDia = new HashMap<>();

        for (Transaccion t : transacciones) {
            LocalDate fecha = parsearFecha(t.getFecha());
            if (fecha == null || !YearMonth.from(fecha).equals(mes)) {
                continue;
            }
            int dia = fecha.getDayOfMonth();
            String tipo = (t instanceof Ingreso) ? "ingreso" : "gasto";
            String actual = estadoPorDia.get(dia);
            if (actual == null) {
                estadoPorDia.put(dia, tipo);
            } else if (!actual.equals(tipo)) {
                estadoPorDia.put(dia, "ambos");
            }
        }

        LocalDate primerDia = mes.atDay(1);
        int relleno = primerDia.getDayOfWeek().getValue() - 1; // lunes = 0
        int diasEnMes = mes.lengthOfMonth();
        LocalDate hoy = LocalDate.now();

        List<CalendarDia> celdas = new ArrayList<>();
        for (int i = 0; i < relleno; i++) {
            celdas.add(new CalendarDia(0, null, false));
        }
        for (int dia = 1; dia <= diasEnMes; dia++) {
            boolean esHoy = YearMonth.from(hoy).equals(mes) && hoy.getDayOfMonth() == dia;
            celdas.add(new CalendarDia(dia, estadoPorDia.get(dia), esHoy));
        }
        while (celdas.size() % 7 != 0) {
            celdas.add(new CalendarDia(0, null, false));
        }
        return celdas;
    }

    private LocalDate parsearFecha(String fecha) {
        if (fecha == null) {
            return null;
        }
        try {
            return LocalDate.parse(fecha);
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    /**
     * Una celda del calendario mensual. Antes vivía en su propio paquete
     * "util"; se anidó aquí porque solo la usa esta clase.
     */
    public static class CalendarDia implements Serializable {

        private final int dia;
        private final String estado;
        private final boolean hoy;

        public CalendarDia(int dia, String estado, boolean hoy) {
            this.dia = dia;
            this.estado = estado;
            this.hoy = hoy;
        }

        public int getDia() {
            return dia;
        }

        /** Clase CSS a aplicar en la celda: vacio / ingreso / gasto / ambos / "" */
        public String getClaseCss() {
            if (dia == 0) {
                return "vacio";
            }
            return estado != null ? estado : "";
        }

        public boolean isHoy() {
            return hoy;
        }

        public boolean isVacio() {
            return dia == 0;
        }
    }
}
