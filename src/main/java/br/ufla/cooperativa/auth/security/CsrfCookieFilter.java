package br.ufla.cooperativa.auth.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.filter.OncePerRequestFilter;

/** Força a emissão do cookie XSRF-TOKEN (o token é carregado de forma preguiçosa). */
public class CsrfCookieFilter extends OncePerRequestFilter {
    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse resp, FilterChain chain)
            throws ServletException, IOException {
        Object attr = req.getAttribute(CsrfToken.class.getName());
        if (attr instanceof CsrfToken token) token.getToken();
        chain.doFilter(req, resp);
    }
}
