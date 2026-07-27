package edu.unl.cc.dominio;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;

@Entity
@DiscriminatorValue("GASTO")
public class Gasto extends Transaccion {

    @Enumerated(EnumType.STRING)
    private Prioridad prioridad;

    protected Gasto() {
        // Requerido por JPA
    }

    public Gasto(double monto, String fecha, Categoria categoria, Prioridad prioridad) {
        super(monto, fecha, categoria);
        this.prioridad = (prioridad != null) ? prioridad : Prioridad.MEDIA;
        setTipo("SALIDA");
    }

    public double calcularGasto() {
        return super.getMonto();
    }

    public Prioridad getPrioridad() {
        return this.prioridad;
    }

    public void setPrioridad(Prioridad prioridad) {
        this.prioridad = (prioridad != null) ? prioridad : Prioridad.MEDIA;
    }

    @Override
    public String toString() {
        return super.toString() + " | Prioridad: " + this.prioridad;
    }
}
