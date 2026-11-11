package br.ufla.cooperativa.auth.security;

import br.ufla.cooperativa.auth.service.JwtService;
import br.ufla.cooperativa.shared.Ator;
import br.ufla.cooperativa.shared.ProblemaHttp;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Optional;
import java.util.Set;
import org.slf4j.MDC;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

/** Autentica pelo cookie ACCESS_TOKEN. Com troca de senha pendente, só libera a própria troca, o logout e /me (CA-15). */
public class JwtAuthFilter extends OncePerRequestFilter {
    private static final Set<String> PERMITIDOS_NA_TROCA =
            Set.of("/api/v1/auth/alterar-senha", "/api/v1/auth/logout", "/api/v1/auth/me");

    private final JwtService jwt;

    public JwtAuthFilter(JwtService jwt) {
        this.jwt = jwt;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse resp, FilterChain chain)
            throws ServletException, IOException {
        String token = lerCookie(req, "ACCESS_TOKEN");
        if (token != null) {
            Optional<JwtService.Claims> claims = jwt.validar(token);
            if (claims.isPresent()) {
                Ator ator = claims.get().ator();
                if (claims.get().trocarSenha() && !PERMITIDOS_NA_TROCA.contains(req.getRequestURI())) {
                    ProblemaHttp.escrever(resp, 403, "Troca de senha obrigatória",
                            "É necessário trocar a senha antes de continuar", "SENHA_TEMPORARIA");
                    return;
                }
                SecurityContext ctx = SecurityContextHolder.createEmptyContext();
                ctx.setAuthentication(UsernamePasswordAuthenticationToken.authenticated(ator, null, ator.authorities()));
                SecurityContextHolder.setContext(ctx);
                MDC.put("usuario_id", String.valueOf(ator.id()));
            }
        }
        chain.doFilter(req, resp);
    }

    private static String lerCookie(HttpServletRequest req, String nome) {
        Cookie[] cs = req.getCookies();
        if (cs == null) return null;
        for (Cookie c : cs) if (nome.equals(c.getName()) && !c.getValue().isBlank()) return c.getValue();
        return null;
    }
}
