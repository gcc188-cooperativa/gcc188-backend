package br.ufla.cooperativa.usuarios.service;

import br.ufla.cooperativa.shared.*;
import br.ufla.cooperativa.usuarios.dto.UsuarioDtos.*;
import br.ufla.cooperativa.usuarios.persistence.Usuario;
import br.ufla.cooperativa.usuarios.persistence.UsuarioRepository;
import java.util.ArrayList;
import java.util.List;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UsuarioService {
    private final UsuarioRepository repo;
    private final PasswordEncoder encoder;
    private final PoliticaSenha politica;
    private final ApplicationEventPublisher publisher;

    public UsuarioService(UsuarioRepository repo, PasswordEncoder encoder, PoliticaSenha politica,
            ApplicationEventPublisher publisher) {
        this.repo = repo;
        this.encoder = encoder;
        this.politica = politica;
        this.publisher = publisher;
    }

    public static UsuarioResponse paraResponse(Usuario u) {
        return new UsuarioResponse(u.getId(), u.getNome(), u.getEmail(), u.getPapel(), u.isAtivo(),
                u.isDeveTrocarSenha(), u.getVersao(), u.getCriadoEm(), u.getAtualizadoEm());
    }

    @Transactional
    public UsuarioResponse criar(CriarUsuarioRequest r) {
        if (r.papel() == Papel.MOTORISTA || r.papel() == Papel.CLIENTE) {
            throw new RegraNegocioException("RN14", "papel",
                    "Usuários MOTORISTA e CLIENTE são criados pelo cadastro de cooperando ou de cliente");
        }
        return paraResponse(criarInterno(r.nome(), r.email(), r.senha(), r.papel()));
    }

    /** Criação usada também pelos cadastros de cooperando e cliente (mesma transação, RN14). */
    @Transactional
    public Usuario criarInterno(String nome, String email, String senha, Papel papel) {
        politica.validar(senha, "senha");
        String e = email.trim();
        if (repo.existsByEmailIgnoreCase(e)) throw new ConflitoException("E-mail já cadastrado", "email");
        Usuario u = new Usuario();
        u.setNome(nome.trim());
        u.setEmail(e);
        u.setSenhaHash(encoder.encode(senha));
        u.setPapel(papel);
        u.setAtivo(true);
        u.setDeveTrocarSenha(true);
        return repo.save(u);
    }

    @Transactional(readOnly = true)
    public Page<UsuarioResponse> listar(Papel papel, Boolean ativo, Pageable pg) {
        List<Specification<Usuario>> f = new ArrayList<>();
        if (papel != null) f.add((root, q, cb) -> cb.equal(root.get("papel"), papel));
        if (ativo != null) f.add((root, q, cb) -> cb.equal(root.get("ativo"), ativo));
        return repo.findAll(Specs.todos(f), pg).map(UsuarioService::paraResponse);
    }

    @Transactional(readOnly = true)
    public UsuarioResponse buscar(Long id) {
        return paraResponse(exigir(id));
    }

    @Transactional
    public UsuarioResponse atualizar(Long id, AtualizarUsuarioRequest r) {
        Usuario u = exigir(id);
        if (!r.versao().equals(u.getVersao())) throw new ConflitoException("Versão desatualizada", "versao");
        boolean vinculado = u.getPapel() == Papel.MOTORISTA || u.getPapel() == Papel.CLIENTE;
        boolean paraVinculado = r.papel() == Papel.MOTORISTA || r.papel() == Papel.CLIENTE;
        if ((vinculado || paraVinculado) && r.papel() != u.getPapel()) {
            throw new RegraNegocioException("RN14", "papel", "O papel de usuários vinculados a cooperando ou cliente não pode ser alterado");
        }
        String e = r.email().trim();
        if (repo.existsByEmailIgnoreCaseAndIdNot(e, id)) throw new ConflitoException("E-mail já cadastrado", "email");
        u.setNome(r.nome().trim());
        u.setEmail(e);
        u.setPapel(r.papel());
        repo.saveAndFlush(u);
        return paraResponse(u);
    }

    @Transactional
    public UsuarioResponse alterarSituacao(Long id, boolean ativo, Ator ator) {
        Usuario u = exigir(id);
        if (!ativo && id.equals(ator.id()))
            throw new RegraNegocioException("AUTO_INATIVACAO", "ativo", "Não é possível inativar o próprio usuário");
        u.setAtivo(ativo);
        repo.saveAndFlush(u);
        if (!ativo) publisher.publishEvent(new SessoesInvalidadas(id));
        return paraResponse(u);
    }

    /** Redefinição pelo administrador: senha temporária e troca obrigatória no próximo acesso (D22). */
    @Transactional
    public void redefinirSenha(Long id, String senhaTemporaria) {
        politica.validar(senhaTemporaria, "senha");
        Usuario u = exigir(id);
        u.setSenhaHash(encoder.encode(senhaTemporaria));
        u.setDeveTrocarSenha(true);
        u.setFalhasLogin(0);
        u.setBloqueadoAte(null);
        repo.saveAndFlush(u);
        publisher.publishEvent(new SessoesInvalidadas(id));
    }

    private Usuario exigir(Long id) {
        return repo.findById(id).orElseThrow(() -> new NaoEncontradoException("Usuário não encontrado"));
    }
}
