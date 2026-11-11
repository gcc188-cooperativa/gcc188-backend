package br.ufla.cooperativa.pagamentos.service;

import br.ufla.cooperativa.contratos.service.ContratoAprovado;
import br.ufla.cooperativa.contratos.service.ContratoCancelado;
import br.ufla.cooperativa.pagamentos.StatusPagamento;
import br.ufla.cooperativa.pagamentos.persistence.Pagamento;
import br.ufla.cooperativa.pagamentos.persistence.PagamentoRepository;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/** Reage, na mesma transação de quem publica, à aprovação e ao cancelamento do contrato (D41). */
@Component
public class PagamentoEventos {
    private final PagamentoRepository repo;
    private final CronogramaPagamentos cronograma;

    public PagamentoEventos(PagamentoRepository repo, CronogramaPagamentos cronograma) {
        this.repo = repo;
        this.cronograma = cronograma;
    }

    @EventListener
    public void aoAprovar(ContratoAprovado e) {
        repo.saveAll(cronograma.gerar(e.contrato()));
    }

    @EventListener
    public void aoCancelar(ContratoCancelado e) {
        for (Pagamento p : repo.findByContratoIdAndStatus(e.contratoId(), StatusPagamento.PREVISTO)) {
            p.setStatus(StatusPagamento.CANCELADO);
        }
    }
}
