package br.ufla.cooperativa.contratos.service;

import br.ufla.cooperativa.contratos.persistence.Contrato;

/** Evento síncrono, na mesma transação: pagamentos gera o cronograma (RF-CTR-04, RNF-05). */
public record ContratoAprovado(Contrato contrato) {}
