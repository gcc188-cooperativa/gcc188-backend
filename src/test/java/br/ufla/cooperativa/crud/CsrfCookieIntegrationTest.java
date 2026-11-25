package br.ufla.cooperativa.crud;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.ufla.cooperativa.shared.AppProperties;
import br.ufla.cooperativa.shared.Papel;
import jakarta.servlet.http.Cookie;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.web.FilterChainProxy;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfFilter;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MvcResult;

/** Regressão: o cookie XSRF-TOKEN deve sobreviver a requisições autenticadas pelo JWT (CA-11). */
class CsrfCookieIntegrationTest extends IntegrationTestBase {

    @Autowired AppProperties props;

    /**
     * O postprocessor csrf() do spring-security-test substitui, por reflexão, o repositório do
     * CsrfFilter compartilhado pelo contexto. Sem restaurá-lo, a ordem dos testes decide se este
     * teste encontra o repositório real (cookie) ou o de teste (sessão, X-CSRF-TOKEN).
     */
    @BeforeEach
    void restaurarCsrfReal() {
        CookieCsrfTokenRepository repo = CookieCsrfTokenRepository.withHttpOnlyFalse();
        repo.setCookieCustomizer(c -> c.secure(props.auth().cookieSecure()).sameSite("Strict").path("/"));
        FilterChainProxy filtros = ctx.getBean("springSecurityFilterChain", FilterChainProxy.class);
        for (var cadeia : filtros.getFilterChains()) {
            for (var filtro : cadeia.getFilters()) {
                if (filtro instanceof CsrfFilter csrf) {
                    ReflectionTestUtils.setField(csrf, "tokenRepository", repo);
                }
            }
        }
    }

    @Test
    void cookieXsrfPermaneceValidoEntreRequisicoesAutenticadas() throws Exception {
        var admin = novoAtor(Papel.ADMINISTRADOR);
        String emailAdmin = usuarios.findById(admin.id()).orElseThrow().getEmail();

        // Browser: o cookie XSRF-TOKEN é semeado diretamente para não depender da emissão
        // em uma requisição recusada pelo CsrfFilter (o CsrfCookieFilter roda depois dele).
        String xsrfInicial = UUID.randomUUID().toString();
        Cookie cookieInicial = new Cookie("XSRF-TOKEN", xsrfInicial);
        cookieInicial.setPath("/");

        // O login é isento de CSRF; o ponto testado é se as respostas autenticadas preservam o cookie.
        MvcResult login = mvc.perform(post("/api/v1/auth/login")
                        .cookie(cookieInicial)
                        .header("X-XSRF-TOKEN", xsrfInicial)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("email", emailAdmin, "senha", "SenhaForte1234"))))
                .andExpect(status().isOk())
                .andReturn();

        String xsrfLogin = xsrf(login.getResponse());
        String xsrf = xsrfLogin == null ? xsrfInicial : xsrfLogin;
        assertThat(xsrf).isNotBlank();

        MvcResult primeira = criarUsuario(cookies(cookieInicial, login.getResponse()), xsrf);
        String xsrfDepois = xsrf(primeira.getResponse());
        if (xsrfDepois != null) assertThat(xsrfDepois).isEqualTo(xsrf);

        // Se alguma resposta tiver apagado o cookie, este POST responde 403 (CSRF inválido).
        criarUsuario(cookies(cookieInicial, login.getResponse(), primeira.getResponse()),
                xsrfDepois == null ? xsrf : xsrfDepois);
    }

    private MvcResult criarUsuario(Cookie[] cookies, String xsrf) throws Exception {
        return mvc.perform(post("/api/v1/usuarios")
                        .cookie(cookies)
                        .header("X-XSRF-TOKEN", xsrf)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "nome", "Usuario CSRF",
                                "email", "csrf-" + UUID.randomUUID() + "@teste.com",
                                "senha", "SenhaForte1234",
                                "papel", "FINANCEIRO"))))
                .andExpect(status().isCreated())
                .andReturn();
    }

    private static String xsrf(MockHttpServletResponse resp) {
        return Optional.ofNullable(Arrays.stream(resp.getCookies())
                        .filter(c -> "XSRF-TOKEN".equals(c.getName()))
                        .findFirst()
                        .orElse(null))
                .map(Cookie::getValue)
                .orElse(null);
    }

    /** Mescla os cookies das respostas; os mais recentes sobrescrevem os anteriores. */
    private static Cookie[] cookies(Cookie inicial, MockHttpServletResponse... respostas) {
        Map<String, Cookie> todas = new LinkedHashMap<>();
        todas.put(inicial.getName(), inicial);
        for (MockHttpServletResponse r : respostas) {
            for (Cookie c : r.getCookies()) todas.put(c.getName(), c);
        }
        return todas.values().toArray(Cookie[]::new);
    }
}
