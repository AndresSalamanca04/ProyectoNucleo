package co.edu.unbosque.controller;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;

// Vigilamos toda la carpeta sistema
@WebFilter(filterName = "AuthFilter", urlPatterns = {"/sistema/*"})
public class AuthFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        
        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse res = (HttpServletResponse) response;
        HttpSession session = req.getSession(false);
        
        String path = req.getRequestURI().substring(req.getContextPath().length());

        // 1. REGLAS DE EXCEPCIÓN (Cosas que SIEMPRE pueden pasar sin login)
        // Permitimos el login, los recursos (imágenes/css) y las peticiones internas de JSF
        if (path.contains("/login.xhtml") || 
            path.contains("/jakarta.faces.resource") || 
            path.contains("/javax.faces.resource")) {
            chain.doFilter(request, response);
            return;
        }

        // 2. VERIFICACIÓN DE SESIÓN
        boolean logueado = (session != null && session.getAttribute("usuarioLogueado") != null);

        if (logueado) {
            // Si está logueado, lo dejamos pasar a donde quiera
            chain.doFilter(request, response);
        } else {
            // Si no está logueado y trata de entrar a otra página, al login
            res.sendRedirect(req.getContextPath() + "/sistema/login.xhtml");
        }
    }
}