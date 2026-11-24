package br.ufla.cooperativa.crud;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.matchesPattern;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.ufla.cooperativa.shared.Ator;
import br.ufla.cooperativa.shared.Papel;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

/** CRUD de uma tabela (RF-USR-01 a 04): diagramas de sequência 1 a 4 do Design. */
class UsuarioCrudIntegrationTest extends IntegrationTestBase {

    Ator admin;

    @BeforeEach
    void ator() {
        admin = novoAtor(Papel.ADMINISTRADOR);
    }

    private Map<String, Object> novo(String email) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("nome", "Maria Souza");
        m.put("email", email);
        m.put("senha", "SenhaForte1234");
        m.put("papel", "FINANCEIRO");
        return m;
    }

    private String criar(String email) throws Exception {
        return corpo(mvc.perform(como(post("/api/v1/usuarios"), admin).content(json(novo(email))))
                .andExpect(status().isCreated()));
    }

    private Map<String, Object> atualizacao(String nome, String email, String papel, long versao) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("nome", nome);
        m.put("email", email);
        m.put("papel", papel);
        m.put("versao", versao);
        return m;
    }

    // =================================================================== CREATE (RF-USR-01)

    @Test
    void criar_retorna201ComLocationEPrimeiroAcessoPendente() throws Exception {
        String email = emailUnico();
        mvc.perform(como(post("/api/v1/usuarios"), admin).content(json(novo(email))))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", matchesPattern("/api/v1/usuarios/\\d+")))
                .andExpect(jsonPath("$.email").value(email))
                .andExpect(jsonPath("$.papel").value("FINANCEIRO"))
                .andExpect(jsonPath("$.ativo").value(true))
                .andExpect(jsonPath("$.deve_trocar_senha").value(true))
                .andExpect(jsonPath("$.versao").value(0))
                .andExpect(jsonPath("$.criado_em").exists())
                .andExpect(jsonPath("$.senha").doesNotExist())
                .andExpect(jsonPath("$.senha_hash").doesNotExist());
    }

    @Test
    void criar_emailDuplicadoSemDistinguirMaiusculas_retorna409() throws Exception {
        String email = emailUnico();
        criar(email);
        mvc.perform(como(post("/api/v1/usuarios"), admin).content(json(novo(email.toUpperCase()))))
                .andExpect(status().isConflict())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.campos_invalidos[*].campo", hasItem("email")));
    }

    @Test
    void criar_senhaForaDaPolitica_retorna422() throws Exception {
        Map<String, Object> m = novo(emailUnico());
        m.put("senha", "curta");
        mvc.perform(como(post("/api/v1/usuarios"), admin).content(json(m)))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.codigo_regra").value("SENHA_FRACA"));
    }

    @Test
    void criar_papelMotoristaOuCliente_retorna422RN14() throws Exception {
        for (String papel : new String[] {"MOTORISTA", "CLIENTE"}) {
            Map<String, Object> m = novo(emailUnico());
            m.put("papel", papel);
            mvc.perform(como(post("/api/v1/usuarios"), admin).content(json(m)))
                    .andExpect(status().isUnprocessableContent())
                    .andExpect(jsonPath("$.codigo_regra").value("RN14"));
        }
    }

    @Test
    void criar_camposInvalidos_retorna400ComListaDeCampos() throws Exception {
        Map<String, Object> m = novo("nao-e-email");
        m.put("nome", "");
        mvc.perform(como(post("/api/v1/usuarios"), admin).content(json(m)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos_invalidos[*].campo", hasItem("email")))
                .andExpect(jsonPath("$.campos_invalidos[*].campo", hasItem("nome")));
    }

    @Test
    void criar_papelSemPermissao_retorna403() throws Exception {
        Ator financeiro = novoAtor(Papel.FINANCEIRO);
        mvc.perform(como(post("/api/v1/usuarios"), financeiro).content(json(novo(emailUnico()))))
                .andExpect(status().isForbidden());
    }

    @Test
    void criar_semAutenticacao_retorna401() throws Exception {
        mvc.perform(get("/api/v1/usuarios")).andExpect(status().isUnauthorized());
    }

    // =================================================================== READ (RF-USR-02)

    @Test
    void consultar_retorna200ComOsDadosDoUsuario() throws Exception {
        String email = emailUnico();
        long id = idDe(criar(email));
        mvc.perform(como(get("/api/v1/usuarios/" + id), admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.email").value(email));
    }

    @Test
    void consultar_inexistente_retorna404() throws Exception {
        mvc.perform(como(get("/api/v1/usuarios/999999999"), admin))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
    }

    @Test
    void listar_filtraPorPapelEPagina() throws Exception {
        for (int i = 0; i < 3; i++) criar(emailUnico());
        mvc.perform(como(get("/api/v1/usuarios?papel=FINANCEIRO&size=2&page=0&sort=nome,desc"), admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(2)))
                .andExpect(jsonPath("$.content[*].papel", org.hamcrest.Matchers.everyItem(org.hamcrest.Matchers.is("FINANCEIRO"))))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(2))
                .andExpect(jsonPath("$.total_elements", greaterThanOrEqualTo(3)))
                .andExpect(jsonPath("$.total_pages", greaterThanOrEqualTo(2)));
    }

    @Test
    void listar_filtraPorSituacao() throws Exception {
        long id = idDe(criar(emailUnico()));
        mvc.perform(como(patch("/api/v1/usuarios/" + id + "/situacao"), admin).content("{\"ativo\":false}"))
                .andExpect(status().isOk());
        mvc.perform(como(get("/api/v1/usuarios?ativo=false&size=100"), admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[*].id", hasItem((int) id)))
                .andExpect(jsonPath("$.content[*].ativo", org.hamcrest.Matchers.everyItem(org.hamcrest.Matchers.is(false))));
    }

    @Test
    void listar_ordenacaoPorCampoNaoPermitido_retorna400() throws Exception {
        mvc.perform(como(get("/api/v1/usuarios?sort=senha_hash"), admin)).andExpect(status().isBadRequest());
    }

    @Test
    void listar_tamanhoDePaginaAcimaDoMaximo_retorna400() throws Exception {
        mvc.perform(como(get("/api/v1/usuarios?size=101"), admin)).andExpect(status().isBadRequest());
    }

    @Test
    void listar_papelSemPermissao_retorna403() throws Exception {
        mvc.perform(como(get("/api/v1/usuarios"), novoAtor(Papel.FINANCEIRO))).andExpect(status().isForbidden());
    }

    // =================================================================== UPDATE (RF-USR-03)

    @Test
    void atualizar_retorna200EIncrementaAVersao() throws Exception {
        String email = emailUnico();
        long id = idDe(criar(email));
        String novoEmail = emailUnico();
        mvc.perform(como(put("/api/v1/usuarios/" + id), admin)
                        .content(json(atualizacao("Maria Atualizada", novoEmail, "ADMINISTRADOR", 0))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nome").value("Maria Atualizada"))
                .andExpect(jsonPath("$.email").value(novoEmail))
                .andExpect(jsonPath("$.papel").value("ADMINISTRADOR"))
                .andExpect(jsonPath("$.versao").value(1));
        mvc.perform(como(get("/api/v1/usuarios/" + id), admin))
                .andExpect(jsonPath("$.nome").value("Maria Atualizada"));
    }

    @Test
    void atualizar_versaoDesatualizada_retorna409() throws Exception {
        String email = emailUnico();
        long id = idDe(criar(email));
        mvc.perform(como(put("/api/v1/usuarios/" + id), admin).content(json(atualizacao("A", email, "FINANCEIRO", 99))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.campos_invalidos[*].campo", hasItem("versao")));
    }

    @Test
    void atualizar_segundaEscritaComAVersaoAntiga_retorna409() throws Exception {
        String email = emailUnico();
        long id = idDe(criar(email));
        mvc.perform(como(put("/api/v1/usuarios/" + id), admin).content(json(atualizacao("Um", email, "FINANCEIRO", 0))))
                .andExpect(status().isOk());
        mvc.perform(como(put("/api/v1/usuarios/" + id), admin).content(json(atualizacao("Dois", email, "FINANCEIRO", 0))))
                .andExpect(status().isConflict());
    }

    @Test
    void atualizar_emailJaUsadoPorOutro_retorna409() throws Exception {
        String emailB = emailUnico();
        criar(emailB);
        String emailA = emailUnico();
        long idA = idDe(criar(emailA));
        mvc.perform(como(put("/api/v1/usuarios/" + idA), admin).content(json(atualizacao("A", emailB, "FINANCEIRO", 0))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.campos_invalidos[*].campo", hasItem("email")));
    }

    @Test
    void atualizar_papelDeUsuarioVinculadoAUmCooperando_retorna422RN14() throws Exception {
        Coop coop = novoCooperando(admin);
        String atual = corpo(mvc.perform(como(get("/api/v1/usuarios/" + coop.usuarioId()), admin)).andExpect(status().isOk()));
        String email = com.jayway.jsonpath.JsonPath.read(atual, "$.email");
        long versao = numero(atual, "$.versao");
        mvc.perform(como(put("/api/v1/usuarios/" + coop.usuarioId()), admin)
                        .content(json(atualizacao("Motorista", email, "FINANCEIRO", versao))))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.codigo_regra").value("RN14"));
    }

    @Test
    void atualizar_inexistente_retorna404() throws Exception {
        mvc.perform(como(put("/api/v1/usuarios/999999999"), admin)
                        .content(json(atualizacao("A", emailUnico(), "FINANCEIRO", 0))))
                .andExpect(status().isNotFound());
    }

    @Test
    void atualizar_semVersao_retorna400() throws Exception {
        long id = idDe(criar(emailUnico()));
        Map<String, Object> m = atualizacao("A", emailUnico(), "FINANCEIRO", 0);
        m.remove("versao");
        mvc.perform(como(put("/api/v1/usuarios/" + id), admin).content(json(m)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos_invalidos[*].campo", hasItem("versao")));
    }

    // =================================================================== DELETE lógico: inativar (RF-USR-04)

    @Test
    void inativarEReativar_retorna200() throws Exception {
        long id = idDe(criar(emailUnico()));
        mvc.perform(como(patch("/api/v1/usuarios/" + id + "/situacao"), admin).content("{\"ativo\":false}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ativo").value(false));
        mvc.perform(como(patch("/api/v1/usuarios/" + id + "/situacao"), admin).content("{\"ativo\":true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ativo").value(true));
    }

    @Test
    void inativar_oProprioUsuario_retorna422() throws Exception {
        mvc.perform(como(patch("/api/v1/usuarios/" + admin.id() + "/situacao"), admin).content("{\"ativo\":false}"))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.codigo_regra").value("AUTO_INATIVACAO"));
    }

    @Test
    void inativar_inexistente_retorna404() throws Exception {
        mvc.perform(como(patch("/api/v1/usuarios/999999999/situacao"), admin).content("{\"ativo\":false}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void inativar_semCampoAtivo_retorna400() throws Exception {
        long id = idDe(criar(emailUnico()));
        mvc.perform(como(patch("/api/v1/usuarios/" + id + "/situacao"), admin).content("{}"))
                .andExpect(status().isBadRequest());
    }

    // =================================================================== redefinição de senha (D22)

    @Test
    void redefinirSenha_retorna204EExigeTrocaNoProximoAcesso() throws Exception {
        long id = idDe(criar(emailUnico()));
        mvc.perform(como(post("/api/v1/usuarios/" + id + "/redefinicao-senha"), admin)
                        .content("{\"senha\":\"NovaSenhaForte99\"}"))
                .andExpect(status().isNoContent());
        mvc.perform(como(get("/api/v1/usuarios/" + id), admin))
                .andExpect(jsonPath("$.deve_trocar_senha").value(true));
    }

    @Test
    void redefinirSenha_senhaFraca_retorna422() throws Exception {
        long id = idDe(criar(emailUnico()));
        mvc.perform(como(post("/api/v1/usuarios/" + id + "/redefinicao-senha"), admin).content("{\"senha\":\"curta\"}"))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.codigo_regra").value("SENHA_FRACA"));
    }
}
