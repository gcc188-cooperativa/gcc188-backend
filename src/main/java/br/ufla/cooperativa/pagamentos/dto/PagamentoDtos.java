package br.ufla.cooperativa.pagamentos.dto;

import br.ufla.cooperativa.pagamentos.StatusPagamento;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

public final class PagamentoDtos {
    private PagamentoDtos() {}

    /** emAtraso é derivado (P5): status PREVISTO com vencimento anterior a hoje. */
    public record PagamentoResponse(Long id, Long contratoId, int numeroParcela, LocalDate dataVencimento,
            BigDecimal valorBruto, BigDecimal valorTaxa, BigDecimal valorLiquido, StatusPagamento status,
            LocalDate dataBaixa, Long baixadoPorId, LocalDate dataRepasse, Long repassadoPorId, String observacao,
            boolean emAtraso, Long versao, Instant criadoEm, Instant atualizadoEm) {}

    public record BaixaRequest(@NotNull LocalDate dataBaixa, @Size(max = 300) String observacao) {}

    public record RepasseRequest(@NotNull LocalDate dataRepasse) {}

    public record EstornoRequest(@NotBlank @Size(max = 300) String justificativa) {}
}
