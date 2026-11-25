package br.ufla.cooperativa.cooperados.service;

import br.ufla.cooperativa.cooperados.dto.CooperadoDtos.*;
import br.ufla.cooperativa.cooperados.persistence.Cooperado;
import br.ufla.cooperativa.cooperados.persistence.CooperadoRepository;
import br.ufla.cooperativa.shared.*;
import br.ufla.cooperativa.usuarios.persistence.Usuario;
import br.ufla.cooperativa.usuarios.service.UsuarioService;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CooperadoService {
    private final CooperadoRepository repo;
    private final UsuarioService usuarios;

    public CooperadoService(CooperadoRepository repo, UsuarioService usuarios) {
        this.repo = repo;
        this.usuarios = usuarios;
    }

    public static CooperadoResponse paraResponse(Cooperado c) {
        return new CooperadoResponse(c.getId(), c.getUsuarioId(), c.getMatricula(), c.getNome(), c.getCpf(),
                c.getTelefone(), c.getDataIngresso(), c.isAtivo(), c.getCriadoEm(), c.getAtualizadoEm());
    }

    /** RF-COO-01: cooperado e usuário MOTORISTA em uma única transação (RN14, RNF-05). */
    @Transactional
    public CooperadoResponse cadastrar(CadastrarCooperadoRequest r) {
        validarCpf(r.cpf());
        if (repo.existsByMatricula(r.matricula())) throw new ConflitoException("Matrícula já cadastrada", "matricula");
        if (repo.existsByCpf(r.cpf())) throw new ConflitoException("CPF já cadastrado", "cpf");
        Usuario u = usuarios.criarInterno(r.nome(), r.emailEfetivo(), r.senhaEfetivo(), Papel.MOTORISTA);
        Cooperado c = new Cooperado();
        c.setUsuarioId(u.getId());
        c.setMatricula(r.matricula().trim());
        c.setNome(r.nome().trim());
        c.setCpf(r.cpf());
        c.setTelefone(r.telefone());
        c.setDataIngresso(r.dataIngresso());
        return paraResponse(repo.save(c));
    }

    @Transactional(readOnly = true)
    public Page<CooperadoResponse> listar(String nome, String matricula, Boolean ativo, Pageable pg) {
        List<Specification<Cooperado>> f = new ArrayList<>();
        if (nome != null && !nome.isBlank())
            f.add((root, q, cb) -> cb.like(cb.lower(root.get("nome")), "%" + nome.trim().toLowerCase() + "%"));
        if (matricula != null && !matricula.isBlank()) f.add((root, q, cb) -> cb.equal(root.get("matricula"), matricula.trim()));
        if (ativo != null) f.add((root, q, cb) -> cb.equal(root.get("ativo"), ativo));
        return repo.findAll(Specs.todos(f), pg).map(CooperadoService::paraResponse);
    }

    @Transactional(readOnly = true)
    public CooperadoResponse buscar(Long id, Ator ator) {
        if (ator.is(Papel.MOTORISTA) && !id.equals(ator.vinculo())) throw naoEncontrado();
        return paraResponse(exigir(id));
    }

    @Transactional
    public CooperadoResponse atualizar(Long id, AtualizarCooperadoRequest r) {
        validarCpf(r.cpf());
        Cooperado c = exigir(id);
        if (repo.existsByMatriculaAndIdNot(r.matricula(), id)) throw new ConflitoException("Matrícula já cadastrada", "matricula");
        if (repo.existsByCpfAndIdNot(r.cpf(), id)) throw new ConflitoException("CPF já cadastrado", "cpf");
        c.setMatricula(r.matricula().trim());
        c.setNome(r.nome().trim());
        c.setCpf(r.cpf());
        c.setTelefone(r.telefone());
        c.setDataIngresso(r.dataIngresso());
        return paraResponse(repo.saveAndFlush(c));
    }

    @Transactional
    public CooperadoResponse alterarSituacao(Long id, boolean ativo) {
        Cooperado c = exigir(id);
        c.setAtivo(ativo);
        return paraResponse(repo.saveAndFlush(c));
    }

    public Cooperado exigir(Long id) {
        return repo.findById(id).orElseThrow(CooperadoService::naoEncontrado);
    }

    private static NaoEncontradoException naoEncontrado() {
        return new NaoEncontradoException("Cooperado não encontrado");
    }

    private static void validarCpf(String cpf) {
        if (!Documentos.cpfValido(cpf)) throw new RegraNegocioException("DOCUMENTO_INVALIDO", "cpf", "CPF inválido");
    }
}
