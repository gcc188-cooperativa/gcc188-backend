package br.ufla.cooperativa.crud;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.ufla.cooperativa.shared.Ator;
import br.ufla.cooperativa.shared.Papel;
import br.ufla.cooperativa.usuarios.persistence.Usuario;
import br.ufla.cooperativa.usuarios.persistence.UsuarioRepository;
import com.jayway.jsonpath.JsonPath;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.testcontainers.postgresql.PostgreSQLContainer;

/**
 * Base dos testes de CRUD: PostgreSQL real (Testcontainers, requer Docker) compartilhado por todas as
 * classes, MockMvc com Spring Security e fábricas de dados únicos (os testes não limpam o banco).
 */
@SpringBootTest
@ActiveProfiles("test")
public abstract class IntegrationTestBase {

    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:17-alpine");

    static {
        POSTGRES.start();
    }

    @DynamicPropertySource
    static void banco(DynamicPropertyRegistry r) {
        r.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        r.add("spring.datasource.username", POSTGRES::getUsername);
        r.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @Autowired WebApplicationContext ctx;
    @Autowired UsuarioRepository usuarios;
    @Autowired PasswordEncoder encoder;

    protected MockMvc mvc;
    private static final Random RND = new Random();

    @BeforeEach
    void montarMvc() {
        mvc = MockMvcBuilders.webAppContextSetup(ctx).apply(springSecurity()).build();
    }

    // ------------------------------------------------------------------ requisições

    protected static MockHttpServletRequestBuilder como(MockHttpServletRequestBuilder rb, Ator a) {
        return rb.with(authentication(new UsernamePasswordAuthenticationToken(a, null, a.authorities())))
                .with(csrf()).contentType(MediaType.APPLICATION_JSON);
    }

    protected static String corpo(ResultActions r) throws Exception {
        return r.andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
    }

    protected static long idDe(String json) {
        return ((Number) JsonPath.read(json, "$.id")).longValue();
    }

    protected static long numero(String json, String caminho) {
        return ((Number) JsonPath.read(json, caminho)).longValue();
    }

    /** Serializa um mapa simples (strings, números, booleanos, nulos) sem depender de ObjectMapper. */
    protected static String json(Map<String, Object> m) {
        StringBuilder sb = new StringBuilder("{");
        boolean primeiro = true;
        for (var e : m.entrySet()) {
            if (!primeiro) sb.append(',');
            primeiro = false;
            sb.append('"').append(e.getKey()).append("\":");
            Object v = e.getValue();
            if (v == null) sb.append("null");
            else if (v instanceof String s) sb.append('"').append(s).append('"');
            else sb.append(v);
        }
        return sb.append('}').toString();
    }

    // ------------------------------------------------------------------ fábricas

    protected static String emailUnico() {
        return "t" + UUID.randomUUID().toString().replace("-", "") + "@teste.com";
    }

    /** Cria direto no banco um usuário do papel (sem vínculo) e devolve o ator correspondente. */
    protected Ator novoAtor(Papel papel) {
        Usuario u = new Usuario();
        u.setNome("Ator " + papel);
        u.setEmail(emailUnico());
        u.setSenhaHash(encoder.encode("SenhaForte1234"));
        u.setPapel(papel);
        u = usuarios.save(u);
        return new Ator(u.getId(), papel, null);
    }

    public record Coop(long id, long usuarioId) {
        public Ator ator() {
            return new Ator(usuarioId, Papel.MOTORISTA, id);
        }
    }

    public record Cli(long id, long usuarioId) {
        public Ator ator() {
            return new Ator(usuarioId, Papel.CLIENTE, id);
        }
    }

    protected Coop novoCooperando(Ator admin) throws Exception {
        String corpo = corpo(mvc.perform(como(post("/api/v1/cooperados"), admin).content(json(Map.of(
                "matricula", "M" + UUID.randomUUID().toString().substring(0, 8),
                "nome", "Motorista Teste",
                "cpf", cpfValido(),
                "data_ingresso", "2026-01-01",
                "email", emailUnico(),
                "senha", "SenhaForte1234")))).andExpect(status().isCreated()));
        return new Coop(idDe(corpo), numero(corpo, "$.usuario_id"));
    }

    protected Cli novoCliente(Ator admin) throws Exception {
        String corpo = corpo(mvc.perform(como(post("/api/v1/clientes"), admin).content(json(Map.of(
                "nome", "Cliente Teste Ltda",
                "documento", cnpjValido(),
                "email", emailUnico(),
                "senha", "SenhaForte1234")))).andExpect(status().isCreated()));
        return new Cli(idDe(corpo), numero(corpo, "$.usuario_id"));
    }

    // ------------------------------------------------------------------ documentos válidos

    protected static String cpfValido() {
        int[] d = new int[11];
        do {
            for (int i = 0; i < 9; i++) d[i] = RND.nextInt(10);
        } while (java.util.Arrays.stream(d, 0, 9).distinct().count() == 1);
        d[9] = dvCpf(d, 9, 10);
        d[10] = dvCpf(d, 10, 11);
        StringBuilder sb = new StringBuilder();
        for (int x : d) sb.append(x);
        return sb.toString();
    }

    private static int dvCpf(int[] d, int n, int peso) {
        int s = 0;
        for (int i = 0; i < n; i++) s += d[i] * (peso - i);
        int r = s % 11;
        return r < 2 ? 0 : 11 - r;
    }

    protected static String cnpjValido() {
        int[] d = new int[14];
        for (int i = 0; i < 8; i++) d[i] = RND.nextInt(10);
        d[8] = 0; d[9] = 0; d[10] = 0; d[11] = 1;
        d[12] = dvCnpj(d, 12);
        d[13] = dvCnpj(d, 13);
        StringBuilder sb = new StringBuilder();
        for (int x : d) sb.append(x);
        return sb.toString();
    }

    private static int dvCnpj(int[] d, int n) {
        int[] w = n == 12 ? new int[] {5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2}
                          : new int[] {6, 5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2};
        int s = 0;
        for (int i = 0; i < n; i++) s += d[i] * w[i];
        int r = s % 11;
        return r < 2 ? 0 : 11 - r;
    }
}
