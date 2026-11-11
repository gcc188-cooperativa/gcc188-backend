package br.ufla.cooperativa.cooperandos.service;

import br.ufla.cooperativa.cooperandos.dto.CooperandoDtos.*;
import br.ufla.cooperativa.cooperandos.persistence.Cooperando;
import br.ufla.cooperativa.cooperandos.persistence.CooperandoRepository;
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
public class CooperandoService {
    private final CooperandoRepository repo;
    private final UsuarioService usuarios;

    public CooperandoService(CooperandoRepository repo, UsuarioService usuarios) {
        this.repo = repo;
        this.usuarios = usuarios;
    }

    public static CooperandoResponse paraResponse(Cooperando c) {
        return new CooperandoResponse(c.getId(), c.getUsuarioId(), c.getMatricula(), c.getNome(), c.getCpf(),
                c.getTelefone(), c.getDataIngresso(), c.isAtivo(), c.getCriadoEm(), c.getAtualizadoEm());
    }

    /** RF-COO-01: cooperando e usuário MOTORISTA em uma única transação (RN14, RNF-05). */
    @Transactional
    public CooperandoResponse cadastrar(CadastrarCooperandoRequest r) {
        validarCpf(r.cpf());
        if (repo.existsByMatricula(r.matricula())) throw new ConflitoException("Matrícula já cadastrada", "matricula");
        if (repo.existsByCpf(r.cpf())) throw new ConflitoException("CPF já cadastrado", "cpf");
        Usuario u = usuarios.criarInterno(r.nome(), r.email(), r.senha(), Papel.MOTORISTA);
        Cooperando c = new Cooperando();
        c.setUsuarioId(u.getId());
        c.setMatricula(r.matricula().trim());
        c.setNome(r.nome().trim());
        c.setCpf(r.cpf());
        c.setTelefone(r.telefone());
        c.setDataIngresso(r.dataIngresso());
        return paraResponse(repo.save(c));
    }

    @Transactional(readOnly = true)
    public Page<CooperandoResponse> listar(String nome, String matricula, Boolean ativo, Pageable pg) {
        List<Specification<Cooperando>> f = new ArrayList<>();
        if (nome != null && !nome.isBlank())
            f.add((root, q, cb) -> cb.like(cb.lower(root.get("nome")), "%" + nome.trim().toLowerCase() + "%"));
        if (matricula != null && !matricula.isBlank()) f.add((root, q, cb) -> cb.equal(root.get("matricula"), matricula.trim()));
        if (ativo != null) f.add((root, q, cb) -> cb.equal(root.get("ativo"), ativo));
        return repo.findAll(Specs.todos(f), pg).map(CooperandoService::paraResponse);
    }

    @Transactional(readOnly = true)
    public CooperandoResponse buscar(Long id, Ator ator) {
        if (ator.is(Papel.MOTORISTA) && !id.equals(ator.vinculo())) throw naoEncontrado();
        return paraResponse(exigir(id));
    }

    @Transactional
    public CooperandoResponse atualizar(Long id, AtualizarCooperandoRequest r) {
        validarCpf(r.cpf());
        Cooperando c = exigir(id);
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
    public CooperandoResponse alterarSituacao(Long id, boolean ativo) {
        Cooperando c = exigir(id);
        c.setAtivo(ativo);
        return paraResponse(repo.saveAndFlush(c));
    }

    public Cooperando exigir(Long id) {
        return repo.findById(id).orElseThrow(CooperandoService::naoEncontrado);
    }

    private static NaoEncontradoException naoEncontrado() {
        return new NaoEncontradoException("Cooperando não encontrado");
    }

    private static void validarCpf(String cpf) {
        if (!Documentos.cpfValido(cpf)) throw new RegraNegocioException("DOCUMENTO_INVALIDO", "cpf", "CPF inválido");
    }
}
