package br.ufla.cooperativa.contratos.service;

import br.ufla.cooperativa.clientes.persistence.Cliente;
import br.ufla.cooperativa.clientes.persistence.ClienteRepository;
import br.ufla.cooperativa.contratos.Modalidade;
import br.ufla.cooperativa.contratos.StatusContrato;
import br.ufla.cooperativa.contratos.TaxaTipo;
import br.ufla.cooperativa.contratos.dto.ContratoDtos.*;
import br.ufla.cooperativa.contratos.persistence.Contrato;
import br.ufla.cooperativa.contratos.persistence.ContratoRepository;
import br.ufla.cooperativa.contratos.service.ContratoEstados.Acao;
import br.ufla.cooperativa.cooperandos.persistence.Cooperando;
import br.ufla.cooperativa.cooperandos.persistence.CooperandoRepository;
import br.ufla.cooperativa.shared.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ContratoService {
    private static final List<String> VIA_COOPERANDO = List.of("cooperando", "id");
    private static final List<String> VIA_CLIENTE = List.of("cliente", "id");

    private final ContratoRepository repo;
    private final CooperandoRepository cooperandos;
    private final ClienteRepository clientes;
    private final ApplicationEventPublisher publisher;
    private final PendenciasFinanceiras pendencias;
    private final PagamentosDoContrato pagamentos;
    private final TempoNegocio tempo;

    public ContratoService(ContratoRepository repo, CooperandoRepository cooperandos, ClienteRepository clientes,
            ApplicationEventPublisher publisher, PendenciasFinanceiras pendencias, PagamentosDoContrato pagamentos,
            TempoNegocio tempo) {
        this.repo = repo;
        this.cooperandos = cooperandos;
        this.clientes = clientes;
        this.publisher = publisher;
        this.pendencias = pendencias;
        this.pagamentos = pagamentos;
        this.tempo = tempo;
    }

    public static ContratoResponse paraResponse(Contrato c, List<ParcelaResumo> parcelas) {
        return new ContratoResponse(c.getId(),
                new ParteResumo(c.getCooperando().getId(), c.getCooperando().getNome()),
                new ParteResumo(c.getCliente().getId(), c.getCliente().getNome()),
                c.getDescricao(), c.getValorTotal(), c.getModalidade(), c.getNumParcelas(), c.getDiaVencimento(),
                c.getDataInicio(), c.getDataFim(), c.getTaxaTipo(), c.getTaxaValor(), c.getStatus(),
                c.getMotivoReprovacao(), c.getCriadoPorId(), c.getAprovadoPorId(), c.getAprovadoEm(), c.getVersao(),
                c.getCriadoEm(), c.getAtualizadoEm(), parcelas);
    }

    // ---------------------------------------------------------------- consultas

    /** Carrega o contrato dentro do escopo do usuário; fora do escopo responde 404 (RN10). */
    @Transactional(readOnly = true)
    public Contrato carregar(Long id, Ator ator) {
        Specification<Contrato> spec = Specs.todos(List.of(
                (Specification<Contrato>) (root, q, cb) -> cb.equal(root.get("id"), id),
                EscopoDoUsuario.<Contrato>de(ator, VIA_COOPERANDO, VIA_CLIENTE)));
        return repo.findOne(spec).orElseThrow(() -> new NaoEncontradoException("Contrato não encontrado"));
    }

    @Transactional(readOnly = true)
    public ContratoResponse detalhar(Long id, Ator ator) {
        Contrato c = carregar(id, ator);
        return paraResponse(c, pagamentos.listar(c.getId()));
    }

    @Transactional(readOnly = true)
    public Page<ContratoResponse> listar(StatusContrato status, Long cooperandoId, Long clienteId, LocalDate de,
            LocalDate ate, Ator ator, Pageable pg) {
        List<Specification<Contrato>> f = new ArrayList<>();
        f.add(EscopoDoUsuario.<Contrato>de(ator, VIA_COOPERANDO, VIA_CLIENTE));
        if (status != null) f.add((root, q, cb) -> cb.equal(root.get("status"), status));
        if (cooperandoId != null) f.add((root, q, cb) -> cb.equal(root.get("cooperando").get("id"), cooperandoId));
        if (clienteId != null) f.add((root, q, cb) -> cb.equal(root.get("cliente").get("id"), clienteId));
        if (de != null) f.add((root, q, cb) -> cb.greaterThanOrEqualTo(root.<LocalDate>get("dataInicio"), de));
        if (ate != null) f.add((root, q, cb) -> cb.lessThanOrEqualTo(root.<LocalDate>get("dataInicio"), ate));
        return repo.findAll(Specs.todos(f), pg).map(c -> paraResponse(c, null));
    }

    // ---------------------------------------------------------------- rascunho

    @Transactional
    public ContratoResponse criar(ContratoRequest r, Ator ator) {
        garantirCooperandoDoMotorista(r.cooperandoId(), ator);
        Contrato c = new Contrato();
        aplicar(c, r);
        c.setStatus(StatusContrato.RASCUNHO);
        c.setCriadoPorId(ator.id());
        repo.save(c);
        return paraResponse(c, null);
    }

    @Transactional
    public ContratoResponse atualizar(Long id, ContratoRequest r, Ator ator) {
        Contrato c = carregar(id, ator);
        if (c.getStatus() != StatusContrato.RASCUNHO)
            throw new ConflitoException("Somente contratos em RASCUNHO são editáveis (RN12)", null);
        if (r.versao() == null) throw new RequisicaoInvalidaException("O campo versao é obrigatório");
        if (!r.versao().equals(c.getVersao())) throw new ConflitoException("Versão desatualizada", "versao");
        garantirCooperandoDoMotorista(r.cooperandoId(), ator);
        aplicar(c, r);
        repo.saveAndFlush(c);
        return paraResponse(c, null);
    }

    @Transactional
    public void excluir(Long id, Ator ator) {
        Contrato c = carregar(id, ator);
        if (c.getStatus() != StatusContrato.RASCUNHO)
            throw new ConflitoException("Somente contratos em RASCUNHO podem ser excluídos (RN11)", null);
        repo.delete(c);
    }

    // ---------------------------------------------------------------- transições (RN12)

    @Transactional
    public ContratoResponse submeter(Long id, Ator ator) {
        Contrato c = carregar(id, ator);
        StatusContrato destino = ContratoEstados.aplicar(c.getStatus(), Acao.SUBMETER);
        validarCompletude(c);
        c.setStatus(destino);
        c.setMotivoReprovacao(null);
        repo.saveAndFlush(c);
        return paraResponse(c, null);
    }

    @Transactional
    public ContratoResponse aprovar(Long id, Ator ator) {
        Contrato c = carregar(id, ator);
        StatusContrato destino = ContratoEstados.aplicar(c.getStatus(), Acao.APROVAR);
        if (ator.id().equals(c.getCriadoPorId()))
            throw new RegraNegocioException("RN12", null, "Quem elabora o contrato não pode aprová-lo");
        validarCompletude(c);
        c.setStatus(destino);
        c.setAprovadoPorId(ator.id());
        c.setAprovadoEm(tempo.agora());
        repo.saveAndFlush(c);
        publisher.publishEvent(new ContratoAprovado(c));
        return paraResponse(c, null);
    }

    @Transactional
    public ContratoResponse reprovar(Long id, String motivo, Ator ator) {
        Contrato c = carregar(id, ator);
        c.setStatus(ContratoEstados.aplicar(c.getStatus(), Acao.REPROVAR));
        c.setMotivoReprovacao(motivo.trim());
        repo.saveAndFlush(c);
        return paraResponse(c, null);
    }

    @Transactional
    public ContratoResponse encerrar(Long id, Ator ator) {
        Contrato c = carregar(id, ator);
        StatusContrato destino = ContratoEstados.aplicar(c.getStatus(), Acao.ENCERRAR);
        if (pendencias.temPendencias(c.getId()))
            throw new ConflitoException("Há pagamentos previstos ou pagos sem repasse (RN06)", null);
        c.setStatus(destino);
        repo.saveAndFlush(c);
        return paraResponse(c, null);
    }

    @Transactional
    public ContratoResponse cancelar(Long id, Ator ator) {
        Contrato c = carregar(id, ator);
        c.setStatus(ContratoEstados.aplicar(c.getStatus(), Acao.CANCELAR));
        repo.saveAndFlush(c);
        publisher.publishEvent(new ContratoCancelado(c.getId()));
        return paraResponse(c, null);
    }

    // ---------------------------------------------------------------- regras

    private void garantirCooperandoDoMotorista(Long cooperandoId, Ator ator) {
        if (ator.is(Papel.MOTORISTA) && !cooperandoId.equals(ator.vinculo()))
            throw new AccessDeniedException("O motorista só pode elaborar contratos próprios");
    }

    /** Validações estruturais (RN01, RN02, RN05, RN13). O limite da taxa (RN03) é verificado ao submeter. */
    private void aplicar(Contrato c, ContratoRequest r) {
        Cooperando coop = cooperandos.findById(r.cooperandoId())
                .orElseThrow(() -> new RegraNegocioException("RN01", "cooperando_id", "Cooperando não encontrado"));
        Cliente cli = clientes.findById(r.clienteId())
                .orElseThrow(() -> new RegraNegocioException("RN01", "cliente_id", "Cliente não encontrado"));
        if (!coop.isAtivo()) throw new RegraNegocioException("RN13", "cooperando_id", "Cooperando inativo");
        if (!cli.isAtivo()) throw new RegraNegocioException("RN13", "cliente_id", "Cliente inativo");
        if (r.dataFim() != null && r.dataFim().isBefore(r.dataInicio()))
            throw new RegraNegocioException("DATAS", "data_fim", "data_fim deve ser igual ou posterior a data_inicio");
        if (r.taxaTipo() == TaxaTipo.PERCENTUAL && r.taxaValor().compareTo(BigDecimal.valueOf(100)) > 0)
            throw new RegraNegocioException("RN02", "taxa_valor", "A taxa percentual deve ser de no máximo 100");

        int n;
        Integer dia;
        if (r.modalidade() == Modalidade.UNICO) {
            if (r.numParcelas() != null && r.numParcelas() != 1)
                throw new RegraNegocioException("RN05", "num_parcelas", "Contrato ÚNICO tem exatamente 1 parcela");
            n = 1;
            dia = null;
        } else {
            n = r.numParcelas() == null ? 0 : r.numParcelas();
            if (n < 2) throw new RegraNegocioException("RN05", "num_parcelas", "Contrato MENSAL exige 2 ou mais parcelas");
            if (r.diaVencimento() == null)
                throw new RegraNegocioException("RN05", "dia_vencimento", "Contrato MENSAL exige o dia de vencimento");
            dia = r.diaVencimento();
        }
        c.setCooperando(coop);
        c.setCliente(cli);
        c.setDescricao(r.descricao().trim());
        c.setValorTotal(r.valorTotal());
        c.setModalidade(r.modalidade());
        c.setNumParcelas(n);
        c.setDiaVencimento(dia);
        c.setDataInicio(r.dataInicio());
        c.setDataFim(r.dataFim());
        c.setTaxaTipo(r.taxaTipo());
        c.setTaxaValor(r.taxaValor());
    }

    /** Completude para submeter e aprovar: parcelas positivas e taxa dentro do valor bruto de cada parcela. */
    private void validarCompletude(Contrato c) {
        if (!c.getCooperando().isAtivo()) throw new RegraNegocioException("RN13", "cooperando_id", "Cooperando inativo");
        if (!c.getCliente().isAtivo()) throw new RegraNegocioException("RN13", "cliente_id", "Cliente inativo");
        for (BigDecimal bruto : Parcelamento.dividir(c.getValorTotal(), c.getNumParcelas())) {
            if (bruto.signum() <= 0)
                throw new RegraNegocioException("RN05", "valor_total", "O valor total é insuficiente para o número de parcelas");
            TaxaCalculadora.calcular(c.getTaxaTipo(), c.getTaxaValor(), bruto);
        }
    }
}
