package edu.unl.cc.repositorio;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.Disposes;
import jakarta.enterprise.inject.Produces;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;

/**
 * Productor CDI del EntityManager/EntityManagerFactory, en una clase
 * aparte de UsuarioRepositorio a propósito.
 * <p>
 * Antes estaban fusionados en un solo archivo, pero eso rompía el
 * despliegue: UsuarioRepositorio inyectaba un EntityManager que ella misma
 * producía, y Weld (el motor de CDI) no puede resolver esa dependencia
 * circular en un bean @Dependent (no hay proxy de por medio que rompa el
 * ciclo, a diferencia de los ámbitos normales como @RequestScoped). El
 * error era: "WELD-001443: Pseudo scoped bean has circular dependencies".
 */
@Dependent
public class EntityManagerProducer {

    @Produces
    @ApplicationScoped
    public EntityManagerFactory crearEntityManagerFactory() {
        return Persistence.createEntityManagerFactory("simuladorPU");
    }

    public void cerrarEntityManagerFactory(@Disposes EntityManagerFactory factory) {
        if (factory != null && factory.isOpen()) {
            factory.close();
        }
    }

    @Produces
    @Dependent
    public EntityManager crearEntityManager(EntityManagerFactory factory) {
        return factory.createEntityManager();
    }

    public void cerrarEntityManager(@Disposes EntityManager entityManager) {
        if (entityManager != null && entityManager.isOpen()) {
            entityManager.close();
        }
    }
}
