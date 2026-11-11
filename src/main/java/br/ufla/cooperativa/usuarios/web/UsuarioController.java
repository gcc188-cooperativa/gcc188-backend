package br.ufla.cooperativa.usuarios.web;

import br.ufla.cooperativa.shared.*;
import br.ufla.cooperativa.usuarios.dto.UsuarioDtos.*;
import br.ufla.cooperativa.usuarios.service.UsuarioService;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.Map;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/usuarios")
@PreAuthorize("hasRole('ADMINISTRADOR')")
public class UsuarioController {
    private static final Map<String, String> ORDENAVEIS =
            Map.of("id", "id", "nome", "nome", "email", "email", "papel", "papel", "criado_em", "criadoEm");
    private final UsuarioService service;

    public UsuarioController(UsuarioService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<UsuarioResponse> criar(@Valid @RequestBody CriarUsuarioRequest r) {
        UsuarioResponse u = service.criar(r);
        return ResponseEntity.created(URI.create("/api/v1/usuarios/" + u.id())).body(u);
    }

    @GetMapping
    public PaginaResponse<UsuarioResponse> listar(
            @RequestParam(name = "papel", required = false) Papel papel,
            @RequestParam(name = "ativo", required = false) Boolean ativo,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int size,
            @RequestParam(name = "sort", required = false) String sort) {
        var pg = Paginacao.de(page, size, sort, ORDENAVEIS, Sort.by("nome").and(Sort.by("id")));
        return PaginaResponse.de(service.listar(papel, ativo, pg));
    }

    @GetMapping("/{id}")
    public UsuarioResponse buscar(@PathVariable("id") Long id) {
        return service.buscar(id);
    }

    @PutMapping("/{id}")
    public UsuarioResponse atualizar(@PathVariable("id") Long id, @Valid @RequestBody AtualizarUsuarioRequest r) {
        return service.atualizar(id, r);
    }

    @PatchMapping("/{id}/situacao")
    public UsuarioResponse situacao(@PathVariable("id") Long id, @Valid @RequestBody SituacaoRequest r,
            @AuthenticationPrincipal Ator ator) {
        return service.alterarSituacao(id, r.ativo(), ator);
    }

    @PostMapping("/{id}/redefinicao-senha")
    public ResponseEntity<Void> redefinir(@PathVariable("id") Long id, @Valid @RequestBody RedefinirSenhaRequest r) {
        service.redefinirSenha(id, r.senha());
        return ResponseEntity.noContent().build();
    }
}
