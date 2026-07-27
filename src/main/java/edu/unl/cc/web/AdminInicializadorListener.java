package edu.unl.cc.web;

import edu.unl.cc.dominio.Rol;
import edu.unl.cc.dominio.Usuario;
import edu.unl.cc.repositorio.UsuarioRepositorio;
import jakarta.enterprise.inject.spi.CDI;
import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.annotation.WebListener;

/**
 * Equivalente a AdminInicializador (CommandLineRunner) de la version Spring:
 * crea la cuenta de administrador la primera vez que arranca la aplicacion,
 * si todavia no existe ningun usuario con ese correo.
 */
@WebListener
public class AdminInicializadorListener implements ServletContextListener {

    // Cambia estas credenciales por defecto si lo necesitas.
    private static final String ADMIN_NOMBRE = "admin";
    private static final String ADMIN_CORREO = "admin@simulador.com";
    private static final String ADMIN_CONTRASENA = "admin123";

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        try {
            UsuarioRepositorio usuarioRepositorio = CDI.current().select(UsuarioRepositorio.class).get();

            if (usuarioRepositorio.existeNombreOCorreo(ADMIN_NOMBRE, ADMIN_CORREO)) {
                return;
            }

            Usuario admin = new Usuario(ADMIN_NOMBRE, ADMIN_CORREO, ADMIN_CONTRASENA, 0.0, 0.0, Rol.ADMIN);
            usuarioRepositorio.guardar(admin);

            System.out.println("==============================================");
            System.out.println(" Cuenta de administrador creada");
            System.out.println(" Usuario/correo: " + ADMIN_CORREO + " (o \"" + ADMIN_NOMBRE + "\")");
            System.out.println(" Contraseña:     " + ADMIN_CONTRASENA);
            System.out.println(" (cámbialas en AdminInicializadorListener.java)");
            System.out.println("==============================================");
        } catch (Exception e) {
            System.out.println("No se pudo inicializar la cuenta admin todavía (¿está PostgreSQL corriendo?): " + e.getMessage());
        }
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        // sin recursos que liberar
    }
}
