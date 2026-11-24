package br.ufla.cooperativa.despesas.service;

import br.ufla.cooperativa.contratos.persistence.Contrato;
import br.ufla.cooperativa.contratos.persistence.ContratoRepository;
import br.ufla.cooperativa.cooperados.persistence.Cooperado;
import br.ufla.cooperativa.cooperados.persistence.CooperadoRepository;
import br.ufla.cooperativa.despesas.CategoriaDespesa;
import br.ufla.cooperativa.despesas.OrigemDespesa;
import br.ufla.cooperativa.despesas.dto.DespesaDtos.*;
import br.ufla.cooperativa.despesas.persistence.Despesa;
import br.ufla.cooperativa.despesas.persistence.DespesaRepository;
import br.ufla.cooperativa.shared.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DespesaService {
    private static final List<String> VIA_COOPERADO = List.of("cooperado", "id");

    private final DespesaRepository repo;
    private final CooperadoRepository cooperados;
    private final ContratoRepository contratos;

    public DespesaService(DespesaRepository repo, CooperadoRepository cooperados, ContratoRepository contratos) {
        this.repo = repo;
        this.cooperados = cooperados;
        this.contratos = contratos;
    }

    public static DespesaResponse paraResponse(Despesa d) {
        return new DespesaResponse(d.getId(), d.getOrigem(), d.getCategoria(), d.getDescricao(), d.getValor(),
                d.getDataDespesa(), d.getCooperado() == null ? null : d.getCooperado().getId(),
                d.getContrato() == null ? null : d.getContrato().getId(), d.getRegistradoPorId(), d.getVersao(),
                d.getCriadoEm(), d.getAtualizadoEm());
    }

    @Transactional
    public DespesaResponse registrar(DespesaRequest r, Ator ator) {
        Despesa d = new Despesa();
        aplicar(d, r, ator);
        d.setRegistradoPorId(ator.id());
        repo.save(d);
        return paraResponse(d);
    }

    @Transactional
    public DespesaResponse atualizar(Long id, DespesaRequest r, Ator ator) {
        Despesa d = carregar(id, ator);
        if (r.versao() == null) throw new RequisicaoInvalidaException("O campo versao é obrigatório");
        if (!r.versao().equals(d.getVersao())) throw new ConflitoException("Versão desatualizada", "versao");
        aplicar(d, r, ator);
        repo.saveAndFlush(d);
        return paraResponse(d);
    }

    @Transactional
    public void excluir(Long id, Ator ator) {
        repo.delete(carregar(id, ator));
    }

    @Transactional(readOnly = true)
    public DespesaResponse buscar(Long id, Ator ator) {
        return paraResponse(carregar(id, ator));
    }

    @Transactional(readOnly = true)
    public Page<DespesaResponse> listar(OrigemDespesa origem, CategoriaDespesa categoria, Long cooperadoId,
            LocalDate de, LocalDate ate, Ator ator, Pageable pg) {
        return repo.findAll(filtros(origem, categoria, cooperadoId, de, ate, ator), pg).map(DespesaService::paraResponse);
    }

    /** RF-DSP-05: totais por categoria e período (agregação em memória sobre o conjunto filtrado). */
    @Transactional(readOnly = true)
    public TotaisDespesaResponse totais(OrigemDespesa origem, LocalDate de, LocalDate ate, Ator ator) {
        Map<CategoriaDespesa, BigDecimal> mapa = new EnumMap<>(CategoriaDespesa.class);
        BigDecimal total = BigDecimal.ZERO;
        for (Despesa d : repo.findAll(filtros(origem, null, null, de, ate, ator))) {
            mapa.merge(d.getCategoria(), d.getValor(), BigDecimal::add);
            total = total.add(d.getValor());
        }
        List<TotalCategoria> lista = mapa.entrySet().stream().map(e -> new TotalCategoria(e.getKey(), e.getValue())).toList();
        return new TotaisDespesaResponse(lista, total);
    }

    private Specification<Despesa> filtros(OrigemDespesa origem, CategoriaDespesa categoria, Long cooperadoId,
            LocalDate de, LocalDate ate, Ator ator) {
        List<Specification<Despesa>> f = new ArrayList<>();
        f.add(EscopoDoUsuario.<Despesa>de(ator, VIA_COOPERADO, null));
        if (origem != null) f.add((root, q, cb) -> cb.equal(root.get("origem"), origem));
        if (categoria != null) f.add((root, q, cb) -> cb.equal(root.get("categoria"), categoria));
        if (cooperadoId != null) f.add((root, q, cb) -> cb.equal(root.get("cooperado").get("id"), cooperadoId));
        if (de != null) f.add((root, q, cb) -> cb.greaterThanOrEqualTo(root.<LocalDate>get("dataDespesa"), de));
        if (ate != null) f.add((root, q, cb) -> cb.lessThanOrEqualTo(root.<LocalDate>get("dataDespesa"), ate));
        return Specs.todos(f);
    }

    private Despesa carregar(Long id, Ator ator) {
        Specification<Despesa> spec = Specs.todos(List.of(
                (Specification<Despesa>) (root, q, cb) -> cb.equal(root.get("id"), id),
                EscopoDoUsuario.<Despesa>de(ator, VIA_COOPERADO, null)));
        return repo.findOne(spec).orElseThrow(() -> new NaoEncontradoException("Despesa não encontrada"));
    }

    /** RN08: coerência entre origem, cooperado e contrato; o motorista só lança despesas próprias (RN10). */
    private void aplicar(Despesa d, DespesaRequest r, Ator ator) {
        Long cooperadoId = r.cooperadoId();
        if (ator.is(Papel.MOTORISTA)) {
            if (r.origem() != OrigemDespesa.COOPERADO)
                throw new AccessDeniedException("O motorista só lança despesas de origem COOPERADO");
            if (cooperadoId != null && !cooperadoId.equals(ator.vinculo()))
                throw new AccessDeniedException("O motorista só lança despesas próprias");
            cooperadoId = ator.vinculo();
        }
        Cooperado coop = null;
        Contrato contrato = null;
        if (r.origem() == OrigemDespesa.COOPERATIVA) {
            if (cooperadoId != null)
                throw new RegraNegocioException("RN08", "cooperado_id", "Despesa da cooperativa não tem cooperado");
            if (r.contratoId() != null)
                throw new RegraNegocioException("RN08", "contrato_id", "Despesa da cooperativa não tem contrato");
        } else {
            if (cooperadoId == null)
                throw new RegraNegocioException("RN08", "cooperado_id", "Despesa de cooperado exige o cooperado");
            coop = cooperados.findById(cooperadoId)
                    .orElseThrow(() -> new RegraNegocioException("RN08", "cooperado_id", "Cooperado não encontrado"));
            if (r.contratoId() != null) {
                contrato = contratos.findById(r.contratoId())
                        .orElseThrow(() -> new RegraNegocioException("RN08", "contrato_id", "Contrato não encontrado"));
                if (!contrato.getCooperado().getId().equals(coop.getId()))
                    throw new RegraNegocioException("RN08", "contrato_id", "O contrato deve pertencer ao cooperado");
            }
        }
        d.setOrigem(r.origem());
        d.setCategoria(r.categoria());
        d.setDescricao(r.descricao().trim());
        d.setValor(r.valor());
        d.setDataDespesa(r.dataDespesa());
        d.setCooperado(coop);
        d.setContrato(contrato);
    }
}
