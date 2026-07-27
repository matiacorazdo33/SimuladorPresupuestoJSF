package edu.unl.cc.web;

import edu.unl.cc.dominio.Usuario;
import jakarta.inject.Inject;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

/**
 * Equivalente a AutenticacionInterceptor + AdminInterceptor de la version
 * Spring Boot: protege las paginas privadas (exige sesion iniciada) y las de
 * /admin/* (exige ademas rol ADMIN).
 */
@WebFilter(urlPatterns = {
        "/menu.xhtml", "/saldo.xhtml", "/presupuesto.xhtml",
        "/movimientoForm.xhtml", "/arcade.xhtml", "/arcadeJuego.xhtml",
        "/cuenta/*", "/admin/*"
})
public class SeguridadFilter implements Filter {

    @Inject
    private AuthBean authBean;

    @Override
    public void init(FilterConfig filterConfig) {
        // sin configuración adicional
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse res = (HttpServletResponse) response;

        if (!authBean.isAutenticado()) {
            res.sendRedirect(req.getContextPath() + "/acceso.xhtml?modo=login");
            return;
        }

        String path = req.getRequestURI();
        if (path.contains("/admin/")) {
            Usuario usuario = authBean.getUsuarioActual();
            if (usuario == null || !usuario.esAdmin()) {
                res.sendError(HttpServletResponse.SC_FORBIDDEN, "Acceso restringido a administradores");
                return;
            }
        }

        chain.doFilter(request, response);
    }

    @Override
    public void destroy() {
        // sin recursos que liberar
    }
}
