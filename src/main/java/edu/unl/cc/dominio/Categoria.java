package edu.unl.cc.dominio;

import jakarta.persistence.Embeddable;

import java.io.Serializable;
import java.util.Objects;

@Embeddable
public class Categoria implements Serializable {

    public static final String VALOR_POR_DEFECTO = "General";

    private String nombre;

    protected Categoria() {
        // Requerido por JPA
    }

    public Categoria(String nombre) {
        this.nombre = validar(nombre);
    }

    private String validar(String texto) {
        if (texto == null || texto.trim().isEmpty()) {
            return VALOR_POR_DEFECTO;
        }
        return texto.trim();
    }

    public String getNombre() {
        return this.nombre;
    }

    public boolean coincideCon(String otroNombre) {
        return this.nombre.equalsIgnoreCase(otroNombre);
    }

    @Override
    public String toString() {
        return this.nombre;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Categoria)) return false;
        Categoria categoria = (Categoria) o;
        return nombre.equalsIgnoreCase(categoria.nombre);
    }

    @Override
    public int hashCode() {
        return Objects.hash(nombre.toLowerCase());
    }
}
