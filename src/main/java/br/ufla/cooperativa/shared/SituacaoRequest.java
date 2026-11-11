package br.ufla.cooperativa.shared;

import jakarta.validation.constraints.NotNull;

public record SituacaoRequest(@NotNull Boolean ativo) {}
