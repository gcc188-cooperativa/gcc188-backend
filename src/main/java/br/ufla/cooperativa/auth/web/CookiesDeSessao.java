package br.ufla.cooperativa.auth.web;

import br.ufla.cooperativa.auth.service.AuthService.Sessao;
import br.ufla.cooperativa.shared.AppProperties;
import jakarta.servlet.http.HttpServletResponse;
import java.time.Duration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

/** Cookies HttpOnly; Secure; SameSite=Strict, com caminhos restritos (Design, cookies). */
@Component
public class CookiesDeSessao {
    public static final String ACESSO = "ACCESS_TOKEN";
    public static final String REFRESH = "REFRESH_TOKEN";

    private final AppProperties props;

    public CookiesDeSessao(AppProperties props) {
        this.props = props;
    }

    public void emitir(HttpServletResponse resp, Sessao s) {
        add(resp, ACESSO, s.acesso(), "/api", props.auth().accessTtl());
        add(resp, REFRESH, s.refresh(), "/api/v1/auth", props.auth().refreshTtl());
    }

    public void limpar(HttpServletResponse resp) {
        add(resp, ACESSO, "", "/api", Duration.ZERO);
        add(resp, REFRESH, "", "/api/v1/auth", Duration.ZERO);
    }

    private void add(HttpServletResponse resp, String nome, String valor, String caminho, Duration ttl) {
        ResponseCookie c = ResponseCookie.from(nome, valor)
                .httpOnly(true)
                .secure(props.auth().cookieSecure())
                .sameSite("Strict")
                .path(caminho)
                .maxAge(ttl)
                .build();
        resp.addHeader(HttpHeaders.SET_COOKIE, c.toString());
    }
}
