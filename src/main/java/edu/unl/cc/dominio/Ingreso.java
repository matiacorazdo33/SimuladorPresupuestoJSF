package edu.unl.cc.dominio;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

@Entity
@DiscriminatorValue("INGRESO")
public class Ingreso extends Transaccion {

    private String fuente;

    protected Ingreso() {
        // Requerido por JPA
    }

    public Ingreso(double monto, String fecha, Categoria categoria, String fuente) {
        super(monto, fecha, categoria);
        this.fuente = validarFuente(fuente);
        setTipo("ENTRADA");
    }

    public double calcularIngreso() {
        return super.getMonto();
    }

    private String validarFuente(String fuente) {
        if (fuente == null || fuente.trim().isEmpty()) {
            return "Fuente No Especificada"; // Valor por defecto seguro
        }
        return fuente;
    }

    public String getFuente() {
        return this.fuente;
    }

    public void setFuente(String fuente) {
        this.fuente = validarFuente(fuente);
    }

    @Override
    public String toString() {
        return super.toString() + " | Fuente: " + this.fuente;
    }
}
