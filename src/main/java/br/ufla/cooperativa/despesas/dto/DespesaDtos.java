package br.ufla.cooperativa.despesas.dto;

import br.ufla.cooperativa.despesas.CategoriaDespesa;
import br.ufla.cooperativa.despesas.OrigemDespesa;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public final class DespesaDtos {
    private DespesaDtos() {}

    public record DespesaResponse(Long id, OrigemDespesa origem, CategoriaDespesa categoria, String descricao,
            BigDecimal valor, LocalDate dataDespesa, Long cooperandoId, Long contratoId, Long registradoPorId,
            Long versao, Instant criadoEm, Instant atualizadoEm) {}

    /** versao é exigida somente no PUT. */
    public record DespesaRequest(
            @NotNull OrigemDespesa origem,
            @NotNull CategoriaDespesa categoria,
            @NotBlank @Size(max = 300) String descricao,
            @NotNull @DecimalMin("0.01") @Digits(integer = 10, fraction = 2) BigDecimal valor,
            @NotNull LocalDate dataDespesa,
            Long cooperandoId,
            Long contratoId,
            Long versao) {}

    public record TotalCategoria(CategoriaDespesa categoria, BigDecimal total) {}

    public record TotaisDespesaResponse(List<TotalCategoria> porCategoria, BigDecimal total) {}
}
