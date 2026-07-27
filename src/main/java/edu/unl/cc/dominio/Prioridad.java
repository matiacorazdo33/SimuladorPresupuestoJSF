package edu.unl.cc.dominio;

public enum Prioridad {
    ALTA,
    MEDIA,
    BAJA;

    public static Prioridad desdeTexto(String texto) {
        if (texto == null || texto.trim().isEmpty()) {
            return MEDIA;
        }
        String normalizado = texto.trim().toUpperCase();
        switch (normalizado) {
            case "ALTA":
            case "A":
                return ALTA;
            case "MEDIA":
            case "M":
                return MEDIA;
            case "BAJA":
            case "B":
                return BAJA;
            default:
                return MEDIA;
        }
    }

    @Override
    public String toString() {
        switch (this) {
            case ALTA:
                return "Alta";
            case MEDIA:
                return "Media";
            case BAJA:
                return "Baja";
            default:
                return "Media";
        }
    }
}
