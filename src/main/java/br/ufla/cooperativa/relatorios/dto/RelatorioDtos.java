package br.ufla.cooperativa.relatorios.dto;

import br.ufla.cooperativa.despesas.CategoriaDespesa;
import br.ufla.cooperativa.pagamentos.StatusPagamento;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public final class RelatorioDtos {
    private RelatorioDtos() {}

    public record Parte(Long id, String nome) {}

    public record LinhaPagamento(Long id, Long contratoId, int numeroParcela, LocalDate dataVencimento,
            BigDecimal valorBruto, BigDecimal valorTaxa, BigDecimal valorLiquido, StatusPagamento status,
            LocalDate dataRepasse) {}

    public record LinhaDespesa(Long id, CategoriaDespesa categoria, String descricao, BigDecimal valor, LocalDate dataDespesa) {}

    /** RF-REL-01. Totais desconsideram pagamentos CANCELADO; despesas são informativas (RN09). */
    public record ExtratoResponse(Parte cooperando, LocalDate de, LocalDate ate, List<LinhaPagamento> pagamentos,
            List<LinhaDespesa> despesas, BigDecimal totalBruto, BigDecimal totalTaxa, BigDecimal totalLiquido,
            BigDecimal totalARepassar, BigDecimal totalRepassado, BigDecimal totalDespesas) {}

    /** RF-REL-02. */
    public record ResumoFinanceiroResponse(LocalDate de, LocalDate ate, BigDecimal taxasArrecadadas,
            BigDecimal valoresARepassar, BigDecimal valoresRepassados, BigDecimal despesasCooperativa, BigDecimal saldo) {}

    public record LinhaInadimplencia(Long id, String nome, long quantidade, BigDecimal valorBruto) {}

    /** RF-REL-03. */
    public record InadimplenciaResponse(LocalDate referencia, long quantidadeTotal, BigDecimal valorTotal,
            List<LinhaInadimplencia> porCliente, List<LinhaInadimplencia> porCooperando) {}

    public record Indicador(long quantidade, BigDecimal valor) {}

    public record Vencimento(Long pagamentoId, Long contratoId, LocalDate dataVencimento, BigDecimal valorBruto) {}

    /** RF-REL-04, já restrito ao escopo do papel (RN10). */
    public record PainelResponse(long contratosPendentesAprovacao, long contratosAtivos, Indicador pagamentosAReceber,
            Indicador pagamentosEmAtraso, Indicador pagamentosARepassar, long proximosVencimentos,
            List<Vencimento> proximosVencimentosLista) {}
}
