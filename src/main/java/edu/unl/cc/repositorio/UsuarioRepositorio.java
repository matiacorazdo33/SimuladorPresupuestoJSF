package edu.unl.cc.repositorio;

import edu.unl.cc.dominio.Usuario;
import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityTransaction;
import jakarta.persistence.NoResultException;
import jakarta.persistence.TypedQuery;

import java.io.Serializable;
import java.util.List;
import java.util.Optional;

/**
 * Repositorio de usuarios, usando JPA/EntityManager directamente (JSF/CDI
 * no trae Spring Data). Cada operación de escritura maneja su propia
 * transacción RESOURCE_LOCAL.
 * <p>
 * Ámbito @Dependent a propósito: así puede usarse tanto durante una
 * petición HTTP normal como en el arranque de la aplicación
 * (AdminInicializadorListener), momento en el que todavía no existe ningún
 * contexto de petición activo.
 * <p>
 * Implementa Serializable para poder inyectarse sin problemas en beans de
 * ámbito "passivating" como los @ViewScoped (p. ej. ArcadeJuegoBean) — CDI
 * exige que toda la cadena de dependencias de esos beans sea serializable.
 */
@Dependent
public class UsuarioRepositorio implements Serializable {

    @Inject
    private EntityManager em;

    public Usuario guardar(Usuario usuario) {
        EntityTransaction tx = em.getTransaction();
        boolean iniciadaAqui = !tx.isActive();
        if (iniciadaAqui) {
            tx.begin();
        }
        Usuario guardado = em.merge(usuario);
        em.flush();
        if (iniciadaAqui) {
            tx.commit();
        }
        return guardado;
    }

    public Optional<Usuario> buscarPorId(String id) {
        if (id == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(em.find(Usuario.class, id));
    }

    public boolean existeNombreOCorreo(String nombre, String correo) {
        TypedQuery<Long> query = em.createQuery(
                "select count(u) from Usuario u where lower(u.nombre) = lower(:nombre) or lower(u.correo) = lower(:correo)",
                Long.class);
        query.setParameter("nombre", nombre == null ? "" : nombre);
        query.setParameter("correo", correo == null ? "" : correo);
        return query.getSingleResult() > 0;
    }

    public Optional<Usuario> buscarPorIdentificador(String identificador) {
        try {
            TypedQuery<Usuario> query = em.createQuery(
                    "select u from Usuario u where u.nombre = :identificador or u.correo = :identificador",
                    Usuario.class);
            query.setParameter("identificador", identificador);
            return Optional.of(query.getSingleResult());
        } catch (NoResultException e) {
            return Optional.empty();
        }
    }

    public Optional<Usuario> buscarPorTokenRecuperacion(String token) {
        if (token == null || token.isBlank()) {
            return Optional.empty();
        }
        try {
            TypedQuery<Usuario> query = em.createQuery(
                    "select u from Usuario u where u.tokenRecuperacion = :token", Usuario.class);
            query.setParameter("token", token);
            return Optional.of(query.getSingleResult());
        } catch (NoResultException e) {
            return Optional.empty();
        }
    }

    public boolean eliminar(String id) {
        Optional<Usuario> usuario = buscarPorId(id);
        if (usuario.isEmpty()) {
            return false;
        }
        EntityTransaction tx = em.getTransaction();
        boolean iniciadaAqui = !tx.isActive();
        if (iniciadaAqui) {
            tx.begin();
        }
        em.remove(em.contains(usuario.get()) ? usuario.get() : em.merge(usuario.get()));
        if (iniciadaAqui) {
            tx.commit();
        }
        return true;
    }

    public List<Usuario> listarTodos() {
        return em.createQuery("select u from Usuario u", Usuario.class).getResultList();
    }
}
