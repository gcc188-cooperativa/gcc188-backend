package br.ufla.cooperativa;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.ufla.cooperativa.shared.Ator;
import br.ufla.cooperativa.shared.Papel;
import br.ufla.cooperativa.usuarios.persistence.Usuario;
import br.ufla.cooperativa.usuarios.persistence.UsuarioRepository;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

/** Integração com PostgreSQL real (requer Docker): CA-01, CA-05 e CA-11, mais as migrações (CA-14). */
@Testcontainers
@SpringBootTest
@ActiveProfiles("test")
class FluxoContratoIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17-alpine");

    @Autowired WebApplicationContext ctx;
    @Autowired UsuarioRepository usuarios;
    @Autowired PasswordEncoder encoder;

    MockMvc mvc;

    @BeforeEach
    void preparar() {
        mvc = MockMvcBuilders.webAppContextSetup(ctx).apply(springSecurity()).build();
    }

    private static MockHttpServletRequestBuilder como(MockHttpServletRequestBuilder rb, Ator a) {
        return rb.with(authentication(new UsernamePasswordAuthenticationToken(a, null, a.authorities())))
                .with(csrf()).contentType(MediaType.APPLICATION_JSON);
    }

    private long id(String json) {
        return ((Number) JsonPath.read(json, "$.id")).longValue();
    }

    @Test
    void fluxoCompletoDoContratoAoCronograma() throws Exception {
        Usuario adm = new Usuario();
        adm.setNome("Adm");
        adm.setEmail("adm-fluxo@coop.com");
        adm.setSenhaHash(encoder.encode("SenhaForte1234"));
        adm.setPapel(Papel.ADMINISTRADOR);
        adm = usuarios.save(adm);
        Ator admin = new Ator(adm.getId(), Papel.ADMINISTRADOR, null);

        String coop = mvc.perform(como(post("/api/v1/cooperandos"), admin).content("""
                {"matricula":"M001","nome":"João Motorista","cpf":"52998224725","telefone":"31999999999",
                 "data_ingresso":"2026-01-01","email":"joao@coop.com","senha":"SenhaForte1234"}"""))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        long coopId = id(coop);
        long coopUsuarioId = ((Number) JsonPath.read(coop, "$.usuario_id")).longValue();

        String cli = mvc.perform(como(post("/api/v1/clientes"), admin).content("""
                {"nome":"Transportes Ltda","documento":"11222333000181","email":"cli@empresa.com","senha":"SenhaForte1234"}"""))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        long cliId = id(cli);

        Ator motorista = new Ator(coopUsuarioId, Papel.MOTORISTA, coopId);
        String body = """
                {"cooperando_id":%d,"cliente_id":%d,"descricao":"Frete mensal","valor_total":6000.00,
                 "modalidade":"MENSAL","num_parcelas":3,"dia_vencimento":10,"data_inicio":"2026-11-10",
                 "taxa_tipo":"PERCENTUAL","taxa_valor":10}""".formatted(coopId, cliId);
        String contrato = mvc.perform(como(post("/api/v1/contratos"), motorista).content(body))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.status").value("RASCUNHO"))
                .andReturn().getResponse().getContentAsString();
        long contratoId = id(contrato);

        mvc.perform(como(post("/api/v1/contratos/" + contratoId + "/submissao"), motorista))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("PENDENTE_APROVACAO"));

        // CA-05: o motorista não aprova
        mvc.perform(como(post("/api/v1/contratos/" + contratoId + "/aprovacao"), motorista))
                .andExpect(status().isForbidden());

        // CA-01: aprovação gera 3 pagamentos PREVISTO de 2000,00 / 200,00 / 1800,00
        mvc.perform(como(post("/api/v1/contratos/" + contratoId + "/aprovacao"), admin))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("ATIVO"));

        mvc.perform(como(get("/api/v1/contratos/" + contratoId + "/pagamentos"), admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[0].status").value("PREVISTO"))
                .andExpect(jsonPath("$[0].valor_bruto").value(2000.0))
                .andExpect(jsonPath("$[0].valor_taxa").value(200.0))
                .andExpect(jsonPath("$[0].valor_liquido").value(1800.0));

        // CA-04: outro motorista não enxerga o contrato (404)
        Ator outro = new Ator(coopUsuarioId, Papel.MOTORISTA, coopId + 999);
        mvc.perform(como(get("/api/v1/contratos/" + contratoId), outro)).andExpect(status().isNotFound());
    }

    @Test
    void escritaSemTokenCsrfEhRecusada() throws Exception {
        Ator admin = new Ator(1L, Papel.ADMINISTRADOR, null);
        mvc.perform(post("/api/v1/usuarios")
                        .with(authentication(new UsernamePasswordAuthenticationToken(admin, null, admin.authorities())))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void semAutenticacaoResponde401() throws Exception {
        mvc.perform(get("/api/v1/contratos")).andExpect(status().isUnauthorized());
    }
}
