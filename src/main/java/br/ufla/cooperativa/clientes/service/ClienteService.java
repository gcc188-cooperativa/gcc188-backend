package br.ufla.cooperativa.clientes.service;

import br.ufla.cooperativa.clientes.dto.ClienteDtos.*;
import br.ufla.cooperativa.clientes.persistence.Cliente;
import br.ufla.cooperativa.clientes.persistence.ClienteRepository;
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
public class ClienteService {
    private final ClienteRepository repo;
    private final UsuarioService usuarios;

    public ClienteService(ClienteRepository repo, UsuarioService usuarios) {
        this.repo = repo;
        this.usuarios = usuarios;
    }

    public static ClienteResponse paraResponse(Cliente c) {
        return new ClienteResponse(c.getId(), c.getUsuarioId(), c.getNome(), c.getDocumento(), c.getTelefone(),
                c.getEmailContato(), c.isAtivo(), c.getCriadoEm(), c.getAtualizadoEm());
    }

    private static ClienteResponse identificacao(Cliente c) {
        return new ClienteResponse(c.getId(), null, c.getNome(), c.getDocumento(), null, null, c.isAtivo(), null, null);
    }

    private static ClienteResponse conforme(Cliente c, Ator ator) {
        return ator.is(Papel.MOTORISTA) ? identificacao(c) : paraResponse(c);
    }

    @Transactional
    public ClienteResponse cadastrar(CadastrarClienteRequest r) {
        validarDocumento(r.documento());
        if (repo.existsByDocumento(r.documento())) throw new ConflitoException("Documento já cadastrado", "documento");
        Usuario u = usuarios.criarInterno(r.nome(), r.email(), r.senha(), Papel.CLIENTE);
        Cliente c = new Cliente();
        c.setUsuarioId(u.getId());
        c.setNome(r.nome().trim());
        c.setDocumento(r.documento());
        c.setTelefone(r.telefone());
        c.setEmailContato(r.emailContato());
        return paraResponse(repo.save(c));
    }

    @Transactional(readOnly = true)
    public Page<ClienteResponse> listar(String nome, String documento, Boolean ativo, Ator ator, Pageable pg) {
        List<Specification<Cliente>> f = new ArrayList<>();
        if (nome != null && !nome.isBlank())
            f.add((root, q, cb) -> cb.like(cb.lower(root.get("nome")), "%" + nome.trim().toLowerCase() + "%"));
        if (documento != null && !documento.isBlank()) f.add((root, q, cb) -> cb.equal(root.get("documento"), documento.trim()));
        if (ativo != null) f.add((root, q, cb) -> cb.equal(root.get("ativo"), ativo));
        return repo.findAll(Specs.todos(f), pg).map(c -> conforme(c, ator));
    }

    @Transactional(readOnly = true)
    public ClienteResponse buscar(Long id, Ator ator) {
        if (ator.is(Papel.CLIENTE) && !id.equals(ator.vinculo())) throw naoEncontrado();
        return conforme(exigir(id), ator);
    }

    @Transactional
    public ClienteResponse atualizar(Long id, AtualizarClienteRequest r) {
        validarDocumento(r.documento());
        Cliente c = exigir(id);
        if (repo.existsByDocumentoAndIdNot(r.documento(), id)) throw new ConflitoException("Documento já cadastrado", "documento");
        c.setNome(r.nome().trim());
        c.setDocumento(r.documento());
        c.setTelefone(r.telefone());
        c.setEmailContato(r.emailContato());
        return paraResponse(repo.saveAndFlush(c));
    }

    @Transactional
    public ClienteResponse alterarSituacao(Long id, boolean ativo) {
        Cliente c = exigir(id);
        c.setAtivo(ativo);
        return paraResponse(repo.saveAndFlush(c));
    }

    public Cliente exigir(Long id) {
        return repo.findById(id).orElseThrow(ClienteService::naoEncontrado);
    }

    private static NaoEncontradoException naoEncontrado() {
        return new NaoEncontradoException("Cliente não encontrado");
    }

    private static void validarDocumento(String d) {
        if (!Documentos.cpfOuCnpjValido(d))
            throw new RegraNegocioException("DOCUMENTO_INVALIDO", "documento", "CPF ou CNPJ inválido");
    }
}
