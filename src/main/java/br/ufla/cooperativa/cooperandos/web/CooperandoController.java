package br.ufla.cooperativa.cooperandos.web;

import br.ufla.cooperativa.cooperandos.dto.CooperandoDtos.*;
import br.ufla.cooperativa.cooperandos.service.CooperandoService;
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
@RequestMapping("/api/v1/cooperandos")
public class CooperandoController {
    private static final Map<String, String> ORDENAVEIS =
            Map.of("id", "id", "nome", "nome", "matricula", "matricula", "data_ingresso", "dataIngresso");
    private final CooperandoService service;

    public CooperandoController(CooperandoService service) {
        this.service = service;
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<CooperandoResponse> cadastrar(@Valid @RequestBody CadastrarCooperandoRequest r) {
        CooperandoResponse c = service.cadastrar(r);
        return ResponseEntity.created(URI.create("/api/v1/cooperandos/" + c.id())).body(c);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMINISTRADOR','FINANCEIRO')")
    public PaginaResponse<CooperandoResponse> listar(
            @RequestParam(name = "nome", required = false) String nome,
            @RequestParam(name = "matricula", required = false) String matricula,
            @RequestParam(name = "ativo", required = false) Boolean ativo,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int size,
            @RequestParam(name = "sort", required = false) String sort) {
        var pg = Paginacao.de(page, size, sort, ORDENAVEIS, Sort.by("nome").and(Sort.by("id")));
        return PaginaResponse.de(service.listar(nome, matricula, ativo, pg));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR','FINANCEIRO','MOTORISTA')")
    public CooperandoResponse buscar(@PathVariable("id") Long id, @AuthenticationPrincipal Ator ator) {
        return service.buscar(id, ator);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public CooperandoResponse atualizar(@PathVariable("id") Long id, @Valid @RequestBody AtualizarCooperandoRequest r) {
        return service.atualizar(id, r);
    }

    @PatchMapping("/{id}/situacao")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public CooperandoResponse situacao(@PathVariable("id") Long id, @Valid @RequestBody SituacaoRequest r) {
        return service.alterarSituacao(id, r.ativo());
    }
}
