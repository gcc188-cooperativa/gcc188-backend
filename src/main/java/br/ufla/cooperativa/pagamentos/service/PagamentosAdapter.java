package br.ufla.cooperativa.pagamentos.service;

import br.ufla.cooperativa.contratos.dto.ContratoDtos.ParcelaResumo;
import br.ufla.cooperativa.contratos.service.PagamentosDoContrato;
import br.ufla.cooperativa.contratos.service.PendenciasFinanceiras;
import br.ufla.cooperativa.pagamentos.StatusPagamento;
import br.ufla.cooperativa.pagamentos.persistence.PagamentoRepository;
import java.util.List;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Implementa as portas declaradas em contratos, sem criar dependência de contratos para pagamentos. */
@Component
public class PagamentosAdapter implements PendenciasFinanceiras, PagamentosDoContrato {
    private final PagamentoRepository repo;

    public PagamentosAdapter(PagamentoRepository repo) {
        this.repo = repo;
    }

    @Override
    @Transactional(readOnly = true)
    public boolean temPendencias(Long contratoId) {
        return repo.existsByContratoIdAndStatusIn(contratoId, List.of(StatusPagamento.PREVISTO, StatusPagamento.PAGO));
    }

    @Override
    @Transactional(readOnly = true)
    public List<ParcelaResumo> listar(Long contratoId) {
        return repo.findByContratoIdOrderByNumeroParcela(contratoId).stream()
                .map(p -> new ParcelaResumo(p.getId(), p.getNumeroParcela(), p.getDataVencimento(), p.getValorBruto(),
                        p.getValorTaxa(), p.getValorLiquido(), p.getStatus().name(), p.getDataBaixa(), p.getDataRepasse()))
                .toList();
    }
}
