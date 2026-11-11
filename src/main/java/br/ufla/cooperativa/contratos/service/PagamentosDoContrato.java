package br.ufla.cooperativa.contratos.service;

import br.ufla.cooperativa.contratos.dto.ContratoDtos.ParcelaResumo;
import java.util.List;

/** Porta implementada pelo módulo de pagamentos, para o detalhe do contrato (RF-CTR-09). */
public interface PagamentosDoContrato {
    List<ParcelaResumo> listar(Long contratoId);
}
