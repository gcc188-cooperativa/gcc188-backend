package br.ufla.cooperativa.pagamentos.service;

import br.ufla.cooperativa.contratos.StatusContrato;
import br.ufla.cooperativa.contratos.service.ContratoService;
import br.ufla.cooperativa.pagamentos.StatusPagamento;
import br.ufla.cooperativa.pagamentos.dto.PagamentoDtos.*;
import br.ufla.cooperativa.pagamentos.persistence.Pagamento;
import br.ufla.cooperativa.pagamentos.persistence.PagamentoRepository;
import br.ufla.cooperativa.shared.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PagamentoService {
    private static final List<String> VIA_COOPERANDO = List.of("contrato", "cooperando", "id");
    private static final List<String> VIA_CLIENTE = List.of("contrato", "cliente", "id");

    private final PagamentoRepository repo;
    private final ContratoService contratos;
    private final TempoNegocio tempo;

    public PagamentoService(PagamentoRepository repo, ContratoService contratos, TempoNegocio tempo) {
        this.repo = repo;
        this.contratos = contratos;
        this.tempo = tempo;
    }

    private PagamentoResponse paraResponse(Pagamento p) {
        boolean atraso = p.getStatus() == StatusPagamento.PREVISTO && p.getDataVencimento().isBefore(tempo.hoje());
        return new PagamentoResponse(p.getId(), p.getContrato().getId(), p.getNumeroParcela(), p.getDataVencimento(),
                p.getValorBruto(), p.getValorTaxa(), p.getValorLiquido(), p.getStatus(), p.getDataBaixa(),
                p.getBaixadoPorId(), p.getDataRepasse(), p.getRepassadoPorId(), p.getObservacao(), atraso,
                p.getVersao(), p.getCriadoEm(), p.getAtualizadoEm());
    }

    @Transactional(readOnly = true)
    public Page<PagamentoResponse> listar(Long contratoId, StatusPagamento status, LocalDate de, LocalDate ate,
            Boolean emAtraso, Ator ator, Pageable pg) {
        LocalDate hoje = tempo.hoje();
        List<Specification<Pagamento>> f = new ArrayList<>();
        f.add(EscopoDoUsuario.<Pagamento>de(ator, VIA_COOPERANDO, VIA_CLIENTE));
        if (contratoId != null) f.add((root, q, cb) -> cb.equal(root.get("contrato").get("id"), contratoId));
        if (status != null) f.add((root, q, cb) -> cb.equal(root.get("status"), status));
        if (de != null) f.add((root, q, cb) -> cb.greaterThanOrEqualTo(root.<LocalDate>get("dataVencimento"), de));
        if (ate != null) f.add((root, q, cb) -> cb.lessThanOrEqualTo(root.<LocalDate>get("dataVencimento"), ate));
        if (emAtraso != null) {
            f.add((root, q, cb) -> {
                var atrasado = cb.and(cb.equal(root.get("status"), StatusPagamento.PREVISTO),
                        cb.lessThan(root.<LocalDate>get("dataVencimento"), hoje));
                return emAtraso ? atrasado : cb.not(atrasado);
            });
        }
        return repo.findAll(Specs.todos(f), pg).map(this::paraResponse);
    }

    @Transactional(readOnly = true)
    public List<PagamentoResponse> listarDoContrato(Long contratoId, Ator ator) {
        contratos.carregar(contratoId, ator); // 404 se o contrato estiver fora do escopo
        return repo.findByContratoIdOrderByNumeroParcela(contratoId).stream().map(this::paraResponse).toList();
    }

    @Transactional(readOnly = true)
    public PagamentoResponse buscar(Long id, Ator ator) {
        return paraResponse(carregar(id, ator));
    }

    /** RF-PAG-02: PREVISTO para PAGO; só contratos ATIVOS aceitam baixa (RN06). */
    @Transactional
    public PagamentoResponse baixar(Long id, BaixaRequest r, Ator ator) {
        Pagamento p = carregar(id, ator);
        if (p.getContrato().getStatus() != StatusContrato.ATIVO)
            throw new ConflitoException("Somente contratos ATIVOS aceitam baixa (RN06)", null);
        if (p.getStatus() != StatusPagamento.PREVISTO)
            throw new ConflitoException("Somente pagamentos PREVISTO aceitam baixa", null);
        p.setStatus(StatusPagamento.PAGO);
        p.setDataBaixa(r.dataBaixa());
        p.setBaixadoPorId(ator.id());
        p.setObservacao(r.observacao());
        repo.saveAndFlush(p);
        return paraResponse(p);
    }

    /** RF-PAG-03: PAGO para REPASSADO; o valor repassado é o líquido congelado (RN07). */
    @Transactional
    public PagamentoResponse repassar(Long id, RepasseRequest r, Ator ator) {
        Pagamento p = carregar(id, ator);
        if (p.getStatus() != StatusPagamento.PAGO)
            throw new ConflitoException("O repasse só é permitido sobre pagamento PAGO (RN07)", null);
        if (r.dataRepasse().isBefore(p.getDataBaixa()))
            throw new RegraNegocioException("RN07", "data_repasse", "O repasse não pode ser anterior à baixa");
        p.setStatus(StatusPagamento.REPASSADO);
        p.setDataRepasse(r.dataRepasse());
        p.setRepassadoPorId(ator.id());
        repo.saveAndFlush(p);
        return paraResponse(p);
    }

    /** RF-PAG-04: estorno de baixa ainda não repassada (PAGO para PREVISTO). */
    @Transactional
    public PagamentoResponse estornar(Long id, EstornoRequest r, Ator ator) {
        Pagamento p = carregar(id, ator);
        if (p.getStatus() != StatusPagamento.PAGO)
            throw new ConflitoException("Somente baixas ainda não repassadas podem ser estornadas", null);
        p.setStatus(StatusPagamento.PREVISTO);
        p.setDataBaixa(null);
        p.setBaixadoPorId(null);
        p.setObservacao(r.justificativa().trim());
        repo.saveAndFlush(p);
        return paraResponse(p);
    }

    private Pagamento carregar(Long id, Ator ator) {
        Specification<Pagamento> spec = Specs.todos(List.of(
                (Specification<Pagamento>) (root, q, cb) -> cb.equal(root.get("id"), id),
                EscopoDoUsuario.<Pagamento>de(ator, VIA_COOPERANDO, VIA_CLIENTE)));
        return repo.findOne(spec).orElseThrow(() -> new NaoEncontradoException("Pagamento não encontrado"));
    }
}
