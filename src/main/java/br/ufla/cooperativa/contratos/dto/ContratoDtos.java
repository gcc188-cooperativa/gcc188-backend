package br.ufla.cooperativa.contratos.dto;

import br.ufla.cooperativa.contratos.Modalidade;
import br.ufla.cooperativa.contratos.StatusContrato;
import br.ufla.cooperativa.contratos.TaxaTipo;
import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public final class ContratoDtos {
    private ContratoDtos() {}

    public record ParteResumo(Long id, String nome) {}

    /** Parcela do cronograma. O status é texto para o módulo de contratos não depender de pagamentos. */
    public record ParcelaResumo(Long id, int numeroParcela, LocalDate dataVencimento, BigDecimal valorBruto,
            BigDecimal valorTaxa, BigDecimal valorLiquido, String status, LocalDate dataBaixa, LocalDate dataRepasse) {}

    public record ContratoResponse(
            Long id, ParteResumo cooperando, ParteResumo cliente, String descricao, BigDecimal valorTotal,
            Modalidade modalidade, int numParcelas, Integer diaVencimento, LocalDate dataInicio, LocalDate dataFim,
            TaxaTipo taxaTipo, BigDecimal taxaValor, StatusContrato status, String motivoReprovacao,
            Long criadoPorId, Long aprovadoPorId, Instant aprovadoEm, Long versao, Instant criadoEm,
            Instant atualizadoEm,
            @JsonInclude(JsonInclude.Include.NON_NULL) List<ParcelaResumo> pagamentos) {}

    /** versao é exigida somente no PUT (concorrência otimista). */
    public record ContratoRequest(
            @NotNull Long cooperandoId,
            @NotNull Long clienteId,
            @NotBlank @Size(max = 500) String descricao,
            @NotNull @DecimalMin("0.01") @Digits(integer = 10, fraction = 2) BigDecimal valorTotal,
            @NotNull Modalidade modalidade,
            Integer numParcelas,
            @Min(1) @Max(28) Integer diaVencimento,
            @NotNull LocalDate dataInicio,
            LocalDate dataFim,
            @NotNull TaxaTipo taxaTipo,
            @NotNull @DecimalMin("0.01") @Digits(integer = 10, fraction = 2) BigDecimal taxaValor,
            Long versao) {}

    public record ReprovarRequest(@NotBlank @Size(max = 300) String motivo) {}
}
