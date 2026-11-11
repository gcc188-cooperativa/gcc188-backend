package br.ufla.cooperativa.despesas.service;

import br.ufla.cooperativa.contratos.persistence.Contrato;
import br.ufla.cooperativa.contratos.persistence.ContratoRepository;
import br.ufla.cooperativa.cooperandos.persistence.Cooperando;
import br.ufla.cooperativa.cooperandos.persistence.CooperandoRepository;
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
    private static final List<String> VIA_COOPERANDO = List.of("cooperando", "id");

    private final DespesaRepository repo;
    private final CooperandoRepository cooperandos;
    private final ContratoRepository contratos;

    public DespesaService(DespesaRepository repo, CooperandoRepository cooperandos, ContratoRepository contratos) {
        this.repo = repo;
        this.cooperandos = cooperandos;
        this.contratos = contratos;
    }

    public static DespesaResponse paraResponse(Despesa d) {
        return new DespesaResponse(d.getId(), d.getOrigem(), d.getCategoria(), d.getDescricao(), d.getValor(),
                d.getDataDespesa(), d.getCooperando() == null ? null : d.getCooperando().getId(),
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
    public Page<DespesaResponse> listar(OrigemDespesa origem, CategoriaDespesa categoria, Long cooperandoId,
            LocalDate de, LocalDate ate, Ator ator, Pageable pg) {
        return repo.findAll(filtros(origem, categoria, cooperandoId, de, ate, ator), pg).map(DespesaService::paraResponse);
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

    private Specification<Despesa> filtros(OrigemDespesa origem, CategoriaDespesa categoria, Long cooperandoId,
            LocalDate de, LocalDate ate, Ator ator) {
        List<Specification<Despesa>> f = new ArrayList<>();
        f.add(EscopoDoUsuario.<Despesa>de(ator, VIA_COOPERANDO, null));
        if (origem != null) f.add((root, q, cb) -> cb.equal(root.get("origem"), origem));
        if (categoria != null) f.add((root, q, cb) -> cb.equal(root.get("categoria"), categoria));
        if (cooperandoId != null) f.add((root, q, cb) -> cb.equal(root.get("cooperando").get("id"), cooperandoId));
        if (de != null) f.add((root, q, cb) -> cb.greaterThanOrEqualTo(root.<LocalDate>get("dataDespesa"), de));
        if (ate != null) f.add((root, q, cb) -> cb.lessThanOrEqualTo(root.<LocalDate>get("dataDespesa"), ate));
        return Specs.todos(f);
    }

    private Despesa carregar(Long id, Ator ator) {
        Specification<Despesa> spec = Specs.todos(List.of(
                (Specification<Despesa>) (root, q, cb) -> cb.equal(root.get("id"), id),
                EscopoDoUsuario.<Despesa>de(ator, VIA_COOPERANDO, null)));
        return repo.findOne(spec).orElseThrow(() -> new NaoEncontradoException("Despesa não encontrada"));
    }

    /** RN08: coerência entre origem, cooperando e contrato; o motorista só lança despesas próprias (RN10). */
    private void aplicar(Despesa d, DespesaRequest r, Ator ator) {
        Long cooperandoId = r.cooperandoId();
        if (ator.is(Papel.MOTORISTA)) {
            if (r.origem() != OrigemDespesa.COOPERANDO)
                throw new AccessDeniedException("O motorista só lança despesas de origem COOPERANDO");
            if (cooperandoId != null && !cooperandoId.equals(ator.vinculo()))
                throw new AccessDeniedException("O motorista só lança despesas próprias");
            cooperandoId = ator.vinculo();
        }
        Cooperando coop = null;
        Contrato contrato = null;
        if (r.origem() == OrigemDespesa.COOPERATIVA) {
            if (cooperandoId != null)
                throw new RegraNegocioException("RN08", "cooperando_id", "Despesa da cooperativa não tem cooperando");
            if (r.contratoId() != null)
                throw new RegraNegocioException("RN08", "contrato_id", "Despesa da cooperativa não tem contrato");
        } else {
            if (cooperandoId == null)
                throw new RegraNegocioException("RN08", "cooperando_id", "Despesa de cooperando exige o cooperando");
            coop = cooperandos.findById(cooperandoId)
                    .orElseThrow(() -> new RegraNegocioException("RN08", "cooperando_id", "Cooperando não encontrado"));
            if (r.contratoId() != null) {
                contrato = contratos.findById(r.contratoId())
                        .orElseThrow(() -> new RegraNegocioException("RN08", "contrato_id", "Contrato não encontrado"));
                if (!contrato.getCooperando().getId().equals(coop.getId()))
                    throw new RegraNegocioException("RN08", "contrato_id", "O contrato deve pertencer ao cooperando");
            }
        }
        d.setOrigem(r.origem());
        d.setCategoria(r.categoria());
        d.setDescricao(r.descricao().trim());
        d.setValor(r.valor());
        d.setDataDespesa(r.dataDespesa());
        d.setCooperando(coop);
        d.setContrato(contrato);
    }
}
