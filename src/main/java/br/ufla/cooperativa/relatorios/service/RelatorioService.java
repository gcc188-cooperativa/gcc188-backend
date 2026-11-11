package br.ufla.cooperativa.relatorios.service;

import br.ufla.cooperativa.contratos.StatusContrato;
import br.ufla.cooperativa.cooperandos.persistence.Cooperando;
import br.ufla.cooperativa.cooperandos.persistence.CooperandoRepository;
import br.ufla.cooperativa.despesas.CategoriaDespesa;
import br.ufla.cooperativa.despesas.OrigemDespesa;
import br.ufla.cooperativa.pagamentos.StatusPagamento;
import br.ufla.cooperativa.relatorios.dto.RelatorioDtos.*;
import br.ufla.cooperativa.relatorios.persistence.RelatorioRepository;
import br.ufla.cooperativa.shared.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class RelatorioService {
    private static final LocalDate MIN = LocalDate.of(1900, 1, 1);
    private static final LocalDate MAX = LocalDate.of(9999, 12, 31);

    private final RelatorioRepository repo;
    private final CooperandoRepository cooperandos;
    private final TempoNegocio tempo;

    public RelatorioService(RelatorioRepository repo, CooperandoRepository cooperandos, TempoNegocio tempo) {
        this.repo = repo;
        this.cooperandos = cooperandos;
        this.tempo = tempo;
    }

    private static LocalDate ini(LocalDate d) {
        return d == null ? MIN : d;
    }

    private static LocalDate fim(LocalDate d) {
        return d == null ? MAX : d;
    }

    private static BigDecimal bd(Object o) {
        return o == null ? BigDecimal.ZERO : (BigDecimal) o;
    }

    /** RF-REL-01: o motorista só vê o próprio extrato; fora do escopo responde 404 (RN10). */
    public ExtratoResponse extrato(Long cooperandoId, LocalDate de, LocalDate ate, Ator ator) {
        if (ator.is(Papel.MOTORISTA) && !cooperandoId.equals(ator.vinculo()))
            throw new NaoEncontradoException("Cooperando não encontrado");
        Cooperando coop = cooperandos.findById(cooperandoId)
                .orElseThrow(() -> new NaoEncontradoException("Cooperando não encontrado"));
        if (de != null && ate != null && ate.isBefore(de))
            throw new RequisicaoInvalidaException("data_ate deve ser igual ou posterior a data_de");

        List<LinhaPagamento> pagamentos = repo.pagamentosDoCooperando(cooperandoId, ini(de), fim(ate)).stream()
                .map(r -> new LinhaPagamento((Long) r[0], (Long) r[1], ((Number) r[2]).intValue(), (LocalDate) r[3],
                        (BigDecimal) r[4], (BigDecimal) r[5], (BigDecimal) r[6], (StatusPagamento) r[7], (LocalDate) r[8]))
                .toList();
        List<LinhaDespesa> despesas = repo.despesasDoCooperando(cooperandoId, ini(de), fim(ate)).stream()
                .map(r -> new LinhaDespesa((Long) r[0], (CategoriaDespesa) r[1], (String) r[2], (BigDecimal) r[3], (LocalDate) r[4]))
                .toList();

        BigDecimal bruto = BigDecimal.ZERO, taxa = BigDecimal.ZERO, liquido = BigDecimal.ZERO;
        BigDecimal aRepassar = BigDecimal.ZERO, repassado = BigDecimal.ZERO;
        for (LinhaPagamento p : pagamentos) {
            if (p.status() == StatusPagamento.CANCELADO) continue;
            bruto = bruto.add(p.valorBruto());
            taxa = taxa.add(p.valorTaxa());
            liquido = liquido.add(p.valorLiquido());
            if (p.status() == StatusPagamento.PAGO) aRepassar = aRepassar.add(p.valorLiquido());
            if (p.status() == StatusPagamento.REPASSADO) repassado = repassado.add(p.valorLiquido());
        }
        BigDecimal totalDespesas = despesas.stream().map(LinhaDespesa::valor).reduce(BigDecimal.ZERO, BigDecimal::add);
        return new ExtratoResponse(new Parte(coop.getId(), coop.getNome()), de, ate, pagamentos, despesas, bruto, taxa,
                liquido, aRepassar, repassado, totalDespesas);
    }

    /** RF-REL-02: taxas e a repassar por data de baixa; repassados por data de repasse; saldo = taxas − despesas da cooperativa. */
    public ResumoFinanceiroResponse resumo(LocalDate de, LocalDate ate) {
        if (de != null && ate != null && ate.isBefore(de))
            throw new RequisicaoInvalidaException("data_ate deve ser igual ou posterior a data_de");
        BigDecimal taxas = repo.somaPagamentos("valorTaxa", "dataBaixa",
                List.of(StatusPagamento.PAGO, StatusPagamento.REPASSADO), ini(de), fim(ate));
        BigDecimal aRepassar = repo.somaPagamentos("valorLiquido", "dataBaixa", List.of(StatusPagamento.PAGO), ini(de), fim(ate));
        BigDecimal repassados = repo.somaPagamentos("valorLiquido", "dataRepasse", List.of(StatusPagamento.REPASSADO), ini(de), fim(ate));
        BigDecimal despesas = repo.somaDespesas(OrigemDespesa.COOPERATIVA, ini(de), fim(ate));
        return new ResumoFinanceiroResponse(de, ate, taxas, aRepassar, repassados, despesas, taxas.subtract(despesas));
    }

    public InadimplenciaResponse inadimplencia() {
        LocalDate hoje = tempo.hoje();
        List<LinhaInadimplencia> porCliente = repo.inadimplenciaPorCliente(hoje).stream().map(RelatorioService::linha).toList();
        List<LinhaInadimplencia> porCooperando = repo.inadimplenciaPorCooperando(hoje).stream().map(RelatorioService::linha).toList();
        long qtd = porCliente.stream().mapToLong(LinhaInadimplencia::quantidade).sum();
        BigDecimal total = porCliente.stream().map(LinhaInadimplencia::valorBruto).reduce(BigDecimal.ZERO, BigDecimal::add);
        return new InadimplenciaResponse(hoje, qtd, total, porCliente, porCooperando);
    }

    private static LinhaInadimplencia linha(Object[] r) {
        return new LinhaInadimplencia((Long) r[0], (String) r[1], ((Number) r[2]).longValue(), bd(r[3]));
    }

    /** RF-REL-04: indicadores restritos ao escopo do papel (motorista: seu cooperando; cliente: seu cliente). */
    public PainelResponse painel(Ator ator) {
        long coop = ator.is(Papel.MOTORISTA) && ator.vinculo() != null ? ator.vinculo() : 0L;
        long cli = ator.is(Papel.CLIENTE) && ator.vinculo() != null ? ator.vinculo() : 0L;
        if ((ator.is(Papel.MOTORISTA) || ator.is(Papel.CLIENTE)) && ator.vinculo() == null) {
            return new PainelResponse(0, 0, new Indicador(0, BigDecimal.ZERO), new Indicador(0, BigDecimal.ZERO),
                    new Indicador(0, BigDecimal.ZERO), 0, List.of());
        }
        LocalDate hoje = tempo.hoje();
        Indicador aReceber = indicador(StatusPagamento.PREVISTO, hoje, MAX, coop, cli);
        Indicador emAtraso = indicador(StatusPagamento.PREVISTO, MIN, hoje.minusDays(1), coop, cli);
        Indicador aRepassar = indicador(StatusPagamento.PAGO, MIN, MAX, coop, cli);
        List<Vencimento> proximos = repo.proximosVencimentos(hoje, hoje.plusDays(7), coop, cli, 5).stream()
                .map(r -> new Vencimento((Long) r[0], (Long) r[1], (LocalDate) r[2], (BigDecimal) r[3])).toList();
        Indicador semana = indicador(StatusPagamento.PREVISTO, hoje, hoje.plusDays(7), coop, cli);
        return new PainelResponse(repo.contarContratos(StatusContrato.PENDENTE_APROVACAO, coop, cli),
                repo.contarContratos(StatusContrato.ATIVO, coop, cli), aReceber, emAtraso, aRepassar,
                semana.quantidade(), proximos);
    }

    private Indicador indicador(StatusPagamento status, LocalDate de, LocalDate ate, long coop, long cli) {
        Object[] r = repo.agregarPagamentos(status, de, ate, coop, cli);
        return new Indicador(((Number) r[0]).longValue(), bd(r[1]));
    }
}
