package br.ufla.cooperativa.clientes.web;

import br.ufla.cooperativa.clientes.dto.ClienteDtos.*;
import br.ufla.cooperativa.clientes.service.ClienteService;
import br.ufla.cooperativa.shared.*;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.Map;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/clientes")
public class ClienteController {
    private static final Map<String, String> ORDENAVEIS = Map.of("id", "id", "nome", "nome", "documento", "documento");
    private final ClienteService service;

    public ClienteController(ClienteService service) {
        this.service = service;
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<ClienteResponse> cadastrar(@Valid @RequestBody CadastrarClienteRequest r) {
        ClienteResponse c = service.cadastrar(r);
        return ResponseEntity.created(URI.create("/api/v1/clientes/" + c.id())).body(c);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMINISTRADOR','FINANCEIRO','MOTORISTA')")
    public PaginaResponse<ClienteResponse> listar(
            @RequestParam(name = "nome", required = false) String nome,
            @RequestParam(name = "documento", required = false) String documento,
            @RequestParam(name = "ativo", required = false) Boolean ativo,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int size,
            @RequestParam(name = "sort", required = false) String sort,
            @AuthenticationPrincipal Ator ator) {
        var pg = Paginacao.de(page, size, sort, ORDENAVEIS, Sort.by("nome").and(Sort.by("id")));
        return PaginaResponse.de(service.listar(nome, documento, ativo, ator, pg));
    }

    @GetMapping("/{id}")
    public ClienteResponse buscar(@PathVariable("id") Long id, @AuthenticationPrincipal Ator ator) {
        return service.buscar(id, ator);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ClienteResponse atualizar(@PathVariable("id") Long id, @Valid @RequestBody AtualizarClienteRequest r) {
        return service.atualizar(id, r);
    }

    @PatchMapping("/{id}/situacao")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ClienteResponse situacao(@PathVariable("id") Long id, @Valid @RequestBody SituacaoRequest r) {
        return service.alterarSituacao(id, r.ativo());
    }
}
