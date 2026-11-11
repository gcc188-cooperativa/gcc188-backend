package br.ufla.cooperativa.contratos.service;

/** Porta implementada pelo módulo de pagamentos (RN06): pagamentos PREVISTO ou PAGO sem repasse. */
public interface PendenciasFinanceiras {
    boolean temPendencias(Long contratoId);
}
