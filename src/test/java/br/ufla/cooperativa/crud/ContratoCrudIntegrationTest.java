package br.ufla.cooperativa.crud;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.matchesPattern;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.ufla.cooperativa.shared.Ator;
import br.ufla.cooperativa.shared.Papel;
import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * CRUD que relaciona quatro tabelas (contrato, cooperado, cliente e pagamento; RF-CTR-01, 02, 09 e 10):
 * diagramas de sequência 5 a 8 do Design, com os fluxos alternativos do quadro de cobertura.
 */
class ContratoCrudIntegrationTest extends IntegrationTestBase {

    Ator admin;
    Ator outroAdmin;
    Coop coop;
    Cli cli;
    Ator mot;

    @BeforeEach
    void fixtures() throws Exception {
        admin = novoAtor(Papel.ADMINISTRADOR);
        outroAdmin = novoAtor(Papel.ADMINISTRADOR);
        coop = novoCooperando(admin);
        cli = novoCliente(admin);
        mot = coop.ator();
    }

    /** Contrato válido: MENSAL, 3 parcelas de 2000,00, taxa de 10% (CA-01). */
    private Map<String, Object> corpoContrato(Coop c, Cli cl) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("cooperado_id", c.id());
        m.put("cliente_id", cl.id());
        m.put("descricao", "Frete mensal de insumos");
        m.put("valor_total", new BigDecimal("6000.00"));
        m.put("modalidade", "MENSAL");
        m.put("num_parcelas", 3);
        m.put("dia_vencimento", 10);
        m.put("data_inicio", "2026-11-10");
        m.put("taxa_tipo", "PERCENTUAL");
        m.put("taxa_valor", new BigDecimal("10.00"));
        return m;
    }

    private String criarRascunho(Ator quem) throws Exception {
        return corpo(mvc.perform(como(post("/api/v1/contratos"), quem).content(json(corpoContrato(coop, cli))))
                .andExpect(status().isCreated()));
    }

    private void submeter(long id, Ator quem) throws Exception {
        mvc.perform(como(post("/api/v1/contratos/" + id + "/submissao"), quem)).andExpect(status().isOk());
    }

    // =================================================================== CREATE (RF-CTR-01)

    @Test
    void criar_comoMotorista_retorna201EmRascunhoComAsPartes() throws Exception {
        mvc.perform(como(post("/api/v1/contratos"), mot).content(json(corpoContrato(coop, cli))))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", matchesPattern("/api/v1/contratos/\\d+")))
                .andExpect(jsonPath("$.status").value("RASCUNHO"))
                .andExpect(jsonPath("$.cooperado.id").value(coop.id()))
                .andExpect(jsonPath("$.cooperado.nome").value("Motorista Teste"))
                .andExpect(jsonPath("$.cliente.id").value(cli.id()))
                .andExpect(jsonPath("$.valor_total").value(6000.0))
                .andExpect(jsonPath("$.criado_por_id").value(mot.id()))
                .andExpect(jsonPath("$.aprovado_por_id").value(org.hamcrest.Matchers.nullValue()))
                .andExpect(jsonPath("$.versao").value(0));
    }

    @Test
    void criar_comoAdministrador_retorna201() throws Exception {
        mvc.perform(como(post("/api/v1/contratos"), admin).content(json(corpoContrato(coop, cli))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.criado_por_id").value(admin.id()));
    }

    @Test
    void criar_contratoUnico_retorna201ComUmaParcela() throws Exception {
        Map<String, Object> m = corpoContrato(coop, cli);
        m.put("modalidade", "UNICO");
        m.remove("num_parcelas");
        m.remove("dia_vencimento");
        mvc.perform(como(post("/api/v1/contratos"), mot).content(json(m)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.modalidade").value("UNICO"))
                .andExpect(jsonPath("$.num_parcelas").value(1));
    }

    @Test
    void criar_taxaMaiorQueAParcela_eAceitoEmRascunhoERecusadoNaSubmissao() throws Exception {
        Map<String, Object> m = corpoContrato(coop, cli);
        m.put("valor_total", new BigDecimal("1200.00")); // parcela de 400,00
        m.put("taxa_tipo", "FIXO");
        m.put("taxa_valor", new BigDecimal("500.00"));
        long id = idDe(corpo(mvc.perform(como(post("/api/v1/contratos"), mot).content(json(m)))
                .andExpect(status().isCreated())));
        // CA-02
        mvc.perform(como(post("/api/v1/contratos/" + id + "/submissao"), mot))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.codigo_regra").value("RN03"))
                .andExpect(jsonPath("$.campos_invalidos[*].campo", hasItem("taxa_valor")));
    }

    @Test
    void criar_motoristaParaOutroCooperando_retorna403() throws Exception {
        Coop outro = novoCooperando(admin);
        mvc.perform(como(post("/api/v1/contratos"), mot).content(json(corpoContrato(outro, cli))))
                .andExpect(status().isForbidden());
    }

    @Test
    void criar_papelSemPermissao_retorna403() throws Exception {
        for (Ator a : new Ator[] {novoAtor(Papel.FINANCEIRO), cli.ator()}) {
            mvc.perform(como(post("/api/v1/contratos"), a).content(json(corpoContrato(coop, cli))))
                    .andExpect(status().isForbidden());
        }
    }

    @Test
    void criar_cooperadoInativo_retorna422RN13() throws Exception {
        mvc.perform(como(patch("/api/v1/cooperados/" + coop.id() + "/situacao"), admin).content("{\"ativo\":false}"))
                .andExpect(status().isOk());
        mvc.perform(como(post("/api/v1/contratos"), admin).content(json(corpoContrato(coop, cli))))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.codigo_regra").value("RN13"))
                .andExpect(jsonPath("$.campos_invalidos[*].campo", hasItem("cooperado_id")));
    }

    @Test
    void criar_clienteInativo_retorna422RN13() throws Exception {
        mvc.perform(como(patch("/api/v1/clientes/" + cli.id() + "/situacao"), admin).content("{\"ativo\":false}"))
                .andExpect(status().isOk());
        mvc.perform(como(post("/api/v1/contratos"), mot).content(json(corpoContrato(coop, cli))))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.codigo_regra").value("RN13"))
                .andExpect(jsonPath("$.campos_invalidos[*].campo", hasItem("cliente_id")));
    }

    @Test
    void criar_clienteInexistente_retorna422RN01() throws Exception {
        Map<String, Object> m = corpoContrato(coop, cli);
        m.put("cliente_id", 999999999L);
        mvc.perform(como(post("/api/v1/contratos"), mot).content(json(m)))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.codigo_regra").value("RN01"));
    }

    @Test
    void criar_mensalSemDiaDeVencimento_retorna422RN05() throws Exception {
        Map<String, Object> m = corpoContrato(coop, cli);
        m.remove("dia_vencimento");
        mvc.perform(como(post("/api/v1/contratos"), mot).content(json(m)))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.codigo_regra").value("RN05"))
                .andExpect(jsonPath("$.campos_invalidos[*].campo", hasItem("dia_vencimento")));
    }

    @Test
    void criar_mensalComUmaParcela_retorna422RN05() throws Exception {
        Map<String, Object> m = corpoContrato(coop, cli);
        m.put("num_parcelas", 1);
        mvc.perform(como(post("/api/v1/contratos"), mot).content(json(m)))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.campos_invalidos[*].campo", hasItem("num_parcelas")));
    }

    @Test
    void criar_unicoComVariasParcelas_retorna422RN05() throws Exception {
        Map<String, Object> m = corpoContrato(coop, cli);
        m.put("modalidade", "UNICO");
        mvc.perform(como(post("/api/v1/contratos"), mot).content(json(m)))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.codigo_regra").value("RN05"));
    }

    @Test
    void criar_taxaPercentualAcimaDeCem_retorna422RN02() throws Exception {
        Map<String, Object> m = corpoContrato(coop, cli);
        m.put("taxa_valor", new BigDecimal("150.00"));
        mvc.perform(como(post("/api/v1/contratos"), mot).content(json(m)))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.codigo_regra").value("RN02"));
    }

    @Test
    void criar_dataFimAnteriorAoInicio_retorna422() throws Exception {
        Map<String, Object> m = corpoContrato(coop, cli);
        m.put("data_fim", "2026-01-01");
        mvc.perform(como(post("/api/v1/contratos"), mot).content(json(m)))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.campos_invalidos[*].campo", hasItem("data_fim")));
    }

    @Test
    void criar_camposInvalidos_retorna400() throws Exception {
        Map<String, Object> m = corpoContrato(coop, cli);
        m.put("valor_total", new BigDecimal("0.00"));
        m.put("descricao", "");
        m.put("dia_vencimento", 31);
        mvc.perform(como(post("/api/v1/contratos"), mot).content(json(m)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos_invalidos[*].campo", hasItem("valor_total")))
                .andExpect(jsonPath("$.campos_invalidos[*].campo", hasItem("descricao")))
                .andExpect(jsonPath("$.campos_invalidos[*].campo", hasItem("dia_vencimento")));
    }

    // =================================================================== READ (RF-CTR-08 e RF-CTR-09)

    @Test
    void detalhar_rascunho_trazPartesESemPagamentos() throws Exception {
        long id = idDe(criarRascunho(mot));
        mvc.perform(como(get("/api/v1/contratos/" + id), mot))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.cooperado.nome").value("Motorista Teste"))
                .andExpect(jsonPath("$.cliente.nome").value("Cliente Teste Ltda"))
                .andExpect(jsonPath("$.pagamentos", hasSize(0)));
    }

    @Test
    void detalhar_aposAprovacao_trazOsTresPagamentosDoCronograma() throws Exception {
        long id = idDe(criarRascunho(mot));
        submeter(id, mot);
        mvc.perform(como(post("/api/v1/contratos/" + id + "/aprovacao"), outroAdmin)).andExpect(status().isOk());
        mvc.perform(como(get("/api/v1/contratos/" + id), admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ATIVO"))
                .andExpect(jsonPath("$.pagamentos", hasSize(3)))
                .andExpect(jsonPath("$.pagamentos[0].numero_parcela").value(1))
                .andExpect(jsonPath("$.pagamentos[0].status").value("PREVISTO"))
                .andExpect(jsonPath("$.pagamentos[0].valor_bruto").value(2000.0))
                .andExpect(jsonPath("$.pagamentos[0].valor_taxa").value(200.0))
                .andExpect(jsonPath("$.pagamentos[0].valor_liquido").value(1800.0))
                .andExpect(jsonPath("$.pagamentos[2].numero_parcela").value(3));
    }

    @Test
    void detalhar_clienteDoContrato_enxerga_outroClienteNao() throws Exception {
        long id = idDe(criarRascunho(mot));
        mvc.perform(como(get("/api/v1/contratos/" + id), cli.ator())).andExpect(status().isOk());
        mvc.perform(como(get("/api/v1/contratos/" + id), novoCliente(admin).ator())).andExpect(status().isNotFound());
    }

    @Test
    void detalhar_outroMotorista_retorna404() throws Exception {
        long id = idDe(criarRascunho(mot));
        mvc.perform(como(get("/api/v1/contratos/" + id), novoCooperando(admin).ator()))
                .andExpect(status().isNotFound());
    }

    @Test
    void detalhar_financeiroEAdministrador_enxergamTudo() throws Exception {
        long id = idDe(criarRascunho(mot));
        mvc.perform(como(get("/api/v1/contratos/" + id), novoAtor(Papel.FINANCEIRO))).andExpect(status().isOk());
        mvc.perform(como(get("/api/v1/contratos/" + id), admin)).andExpect(status().isOk());
    }

    @Test
    void detalhar_inexistente_retorna404() throws Exception {
        mvc.perform(como(get("/api/v1/contratos/999999999"), admin)).andExpect(status().isNotFound());
    }

    @Test
    void listar_motoristaVeSomenteOsProprios() throws Exception {
        criarRascunho(mot);
        Coop outro = novoCooperando(admin);
        mvc.perform(como(post("/api/v1/contratos"), outro.ator()).content(json(corpoContrato(outro, cli))))
                .andExpect(status().isCreated());
        mvc.perform(como(get("/api/v1/contratos"), mot))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total_elements").value(1))
                .andExpect(jsonPath("$.content[*].cooperando.id", everyItem(equalTo((int) coop.id()))));
    }

    @Test
    void listar_clienteVeSomenteOsSeus() throws Exception {
        criarRascunho(mot);
        mvc.perform(como(get("/api/v1/contratos"), cli.ator()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total_elements").value(1))
                .andExpect(jsonPath("$.content[0].cliente.id").value(cli.id()));
        mvc.perform(como(get("/api/v1/contratos"), novoCliente(admin).ator()))
                .andExpect(jsonPath("$.total_elements").value(0));
    }

    @Test
    void listar_filtraPorStatusEPagina() throws Exception {
        long a = idDe(criarRascunho(mot));
        criarRascunho(mot);
        criarRascunho(mot);
        submeter(a, mot);
        mvc.perform(como(get("/api/v1/contratos?cooperado_id=" + coop.id() + "&status=RASCUNHO&size=1&page=0"), admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.total_elements").value(2))
                .andExpect(jsonPath("$.total_pages").value(2))
                .andExpect(jsonPath("$.content[0].status").value("RASCUNHO"));
        mvc.perform(como(get("/api/v1/contratos?cooperado_id=" + coop.id() + "&status=PENDENTE_APROVACAO"), admin))
                .andExpect(jsonPath("$.total_elements").value(1));
    }

    @Test
    void listar_filtraPorPeriodoDeInicio() throws Exception {
        criarRascunho(mot); // início em 2026-11-10
        mvc.perform(como(get("/api/v1/contratos?cooperado_id=" + coop.id() + "&data_inicio_de=2026-11-01&data_inicio_ate=2026-11-30"), admin))
                .andExpect(jsonPath("$.total_elements").value(1));
        mvc.perform(como(get("/api/v1/contratos?cooperado_id=" + coop.id() + "&data_inicio_de=2027-01-01"), admin))
                .andExpect(jsonPath("$.total_elements").value(0));
    }

    @Test
    void listar_ordenacaoInvalida_retorna400() throws Exception {
        mvc.perform(como(get("/api/v1/contratos?sort=descricao"), admin)).andExpect(status().isBadRequest());
    }

    // =================================================================== UPDATE (RF-CTR-02)

    private Map<String, Object> edicao(long versao) {
        Map<String, Object> m = corpoContrato(coop, cli);
        m.put("descricao", "Descrição editada");
        m.put("valor_total", new BigDecimal("9000.00"));
        m.put("versao", versao);
        return m;
    }

    @Test
    void editar_rascunho_retorna200EIncrementaAVersao() throws Exception {
        long id = idDe(criarRascunho(mot));
        mvc.perform(como(put("/api/v1/contratos/" + id), mot).content(json(edicao(0))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.descricao").value("Descrição editada"))
                .andExpect(jsonPath("$.valor_total").value(9000.0))
                .andExpect(jsonPath("$.versao").value(1));
        mvc.perform(como(get("/api/v1/contratos/" + id), mot)).andExpect(jsonPath("$.valor_total").value(9000.0));
    }

    @Test
    void editar_trocaClienteEmRascunho_atualizaAParte() throws Exception {
        long id = idDe(criarRascunho(mot));
        Cli outroCli = novoCliente(admin);
        Map<String, Object> m = edicao(0);
        m.put("cliente_id", outroCli.id());
        mvc.perform(como(put("/api/v1/contratos/" + id), mot).content(json(m)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cliente.id").value(outroCli.id()));
    }

    @Test
    void editar_contratoForaDeRascunho_retorna409() throws Exception {
        long id = idDe(criarRascunho(mot));
        submeter(id, mot);
        mvc.perform(como(put("/api/v1/contratos/" + id), mot).content(json(edicao(1))))
                .andExpect(status().isConflict());
    }

    @Test
    void editar_versaoDesatualizada_retorna409() throws Exception {
        long id = idDe(criarRascunho(mot));
        mvc.perform(como(put("/api/v1/contratos/" + id), mot).content(json(edicao(99))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.campos_invalidos[*].campo", hasItem("versao")));
    }

    @Test
    void editar_semVersao_retorna400() throws Exception {
        long id = idDe(criarRascunho(mot));
        Map<String, Object> m = edicao(0);
        m.remove("versao");
        mvc.perform(como(put("/api/v1/contratos/" + id), mot).content(json(m))).andExpect(status().isBadRequest());
    }

    @Test
    void editar_taxaPercentualAcimaDeCem_retorna422RN02() throws Exception {
        long id = idDe(criarRascunho(mot));
        Map<String, Object> m = edicao(0);
        m.put("taxa_valor", new BigDecimal("101.00"));
        mvc.perform(como(put("/api/v1/contratos/" + id), mot).content(json(m)))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.codigo_regra").value("RN02"));
    }

    @Test
    void editar_contratoDeOutroMotorista_retorna404() throws Exception {
        long id = idDe(criarRascunho(mot));
        Coop outro = novoCooperando(admin);
        Map<String, Object> m = corpoContrato(outro, cli);
        m.put("versao", 0);
        mvc.perform(como(put("/api/v1/contratos/" + id), outro.ator()).content(json(m)))
                .andExpect(status().isNotFound());
    }

    @Test
    void editar_inexistente_retorna404() throws Exception {
        mvc.perform(como(put("/api/v1/contratos/999999999"), admin).content(json(edicao(0))))
                .andExpect(status().isNotFound());
    }

    // =================================================================== DELETE (RF-CTR-10)

    @Test
    void excluir_rascunho_retorna204ENaoEncontraDepois() throws Exception {
        long id = idDe(criarRascunho(mot));
        mvc.perform(como(delete("/api/v1/contratos/" + id), mot)).andExpect(status().isNoContent());
        mvc.perform(como(get("/api/v1/contratos/" + id), mot)).andExpect(status().isNotFound());
    }

    @Test
    void excluir_contratoPendente_retorna409() throws Exception {
        long id = idDe(criarRascunho(mot));
        submeter(id, mot);
        mvc.perform(como(delete("/api/v1/contratos/" + id), mot)).andExpect(status().isConflict());
        mvc.perform(como(get("/api/v1/contratos/" + id), mot)).andExpect(status().isOk());
    }

    @Test
    void excluir_contratoAtivoComPagamentos_retorna409() throws Exception {
        long id = idDe(criarRascunho(mot));
        submeter(id, mot);
        mvc.perform(como(post("/api/v1/contratos/" + id + "/aprovacao"), outroAdmin)).andExpect(status().isOk());
        mvc.perform(como(delete("/api/v1/contratos/" + id), admin)).andExpect(status().isConflict());
    }

    @Test
    void excluir_contratoDeOutroMotorista_retorna404() throws Exception {
        long id = idDe(criarRascunho(mot));
        mvc.perform(como(delete("/api/v1/contratos/" + id), novoCooperando(admin).ator()))
                .andExpect(status().isNotFound());
    }

    @Test
    void excluir_papelSemPermissao_retorna403() throws Exception {
        long id = idDe(criarRascunho(mot));
        mvc.perform(como(delete("/api/v1/contratos/" + id), cli.ator())).andExpect(status().isForbidden());
        mvc.perform(como(delete("/api/v1/contratos/" + id), novoAtor(Papel.FINANCEIRO))).andExpect(status().isForbidden());
    }

    @Test
    void excluir_inexistente_retorna404() throws Exception {
        mvc.perform(como(delete("/api/v1/contratos/999999999"), admin)).andExpect(status().isNotFound());
    }
}
