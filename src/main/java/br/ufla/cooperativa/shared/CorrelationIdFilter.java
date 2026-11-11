package br.ufla.cooperativa.shared;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.HandlerMapping;

/** Identificador de correlação (X-Request-Id) no MDC, na resposta e no log de acesso (RNF-23). */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class CorrelationIdFilter extends OncePerRequestFilter {
    private static final Logger log = LoggerFactory.getLogger("acesso");

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse resp, FilterChain chain)
            throws ServletException, IOException {
        String id = req.getHeader("X-Request-Id");
        if (id == null || id.isBlank() || id.length() > 100 || !id.matches("[A-Za-z0-9._-]+")) {
            id = UUID.randomUUID().toString();
        }
        MDC.put("correlation_id", id);
        resp.setHeader("X-Request-Id", id);
        long inicio = System.nanoTime();
        try {
            chain.doFilter(req, resp);
        } finally {
            Object padrao = req.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE);
            log.atInfo()
                    .addKeyValue("metodo", req.getMethod())
                    .addKeyValue("rota", padrao != null ? padrao.toString() : "-")
                    .addKeyValue("status", resp.getStatus())
                    .addKeyValue("duracao_ms", (System.nanoTime() - inicio) / 1_000_000)
                    .log("requisicao");
            MDC.remove("usuario_id");
            MDC.remove("correlation_id");
        }
    }
}
