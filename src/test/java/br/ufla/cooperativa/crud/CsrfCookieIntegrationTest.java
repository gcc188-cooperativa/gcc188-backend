package br.ufla.cooperativa.crud;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.ufla.cooperativa.shared.Papel;
import jakarta.servlet.http.Cookie;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MvcResult;

/** Regressão: o cookie XSRF-TOKEN deve sobreviver a requisições autenticadas pelo JWT (CA-11). */
class CsrfCookieIntegrationTest extends IntegrationTestBase {

    @Test
    void cookieXsrfPermaneceValidoEntreRequisicoesAutenticadas() throws Exception {
        var admin = novoAtor(Papel.ADMINISTRADOR);
        String emailAdmin = usuarios.findById(admin.id()).orElseThrow().getEmail();

        // Browser: primeiro GET emite o cookie XSRF-TOKEN; o login não é protegido por CSRF.
        MvcResult bootstrap = mvc.perform(get("/api/v1/auth/me"))
                .andExpect(status().isUnauthorized())
                .andReturn();
        String xsrfInicial = xsrf(bootstrap.getResponse());
        assertThat(xsrfInicial).isNotBlank();

        MvcResult login = mvc.perform(post("/api/v1/auth/login")
                        .cookie(bootstrap.getResponse().getCookies())
                        .header("X-XSRF-TOKEN", xsrfInicial)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("email", emailAdmin, "senha", "SenhaForte1234"))))
                .andExpect(status().isOk())
                .andReturn();

        String xsrfLogin = xsrf(login.getResponse());
        String xsrf = xsrfLogin == null ? xsrfInicial : xsrfLogin;
        assertThat(xsrf).isNotBlank();

        MvcResult primeira = criarUsuario(cookies(bootstrap.getResponse(), login.getResponse()), xsrf);
        String xsrfDepois = xsrf(primeira.getResponse());
        if (xsrfDepois != null) assertThat(xsrfDepois).isEqualTo(xsrf);

        // Se a resposta anterior tiver apagado o cookie, este POST responde 403 (CSRF inválido).
        criarUsuario(cookies(bootstrap.getResponse(), login.getResponse(), primeira.getResponse()),
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

    private static Cookie[] cookies(MockHttpServletResponse... respostas) {
        Map<String, Cookie> todas = new LinkedHashMap<>();
        for (MockHttpServletResponse r : respostas) {
            for (Cookie c : r.getCookies()) todas.put(c.getName(), c);
        }
        return todas.values().toArray(Cookie[]::new);
    }
}
