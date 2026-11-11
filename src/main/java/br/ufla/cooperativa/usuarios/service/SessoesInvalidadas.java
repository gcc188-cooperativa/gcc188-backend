package br.ufla.cooperativa.usuarios.service;

/** Evento: as sessões (refresh tokens) do usuário devem ser revogadas. Tratado pelo módulo auth. */
public record SessoesInvalidadas(Long usuarioId) {}
