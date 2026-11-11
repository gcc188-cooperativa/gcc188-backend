package br.ufla.cooperativa.contratos.web;

import br.ufla.cooperativa.contratos.StatusContrato;
import br.ufla.cooperativa.contratos.dto.ContratoDtos.*;
import br.ufla.cooperativa.contratos.service.ContratoService;
import br.ufla.cooperativa.shared.*;
import jakarta.validation.Valid;
import java.net.URI;
import java.time.LocalDate;
import java.util.Map;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/contratos")
public class ContratoController {
    private static final Map<String, String> ORDENAVEIS = Map.of("id", "id", "data_inicio", "dataInicio",
            "valor_total", "valorTotal", "status", "status", "criado_em", "criadoEm");
    private final ContratoService service;

    public ContratoController(ContratoService service) {
        this.service = service;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('MOTORISTA','ADMINISTRADOR')")
    public ResponseEntity<ContratoResponse> criar(@Valid @RequestBody ContratoRequest r, @AuthenticationPrincipal Ator ator) {
        ContratoResponse c = service.criar(r, ator);
        return ResponseEntity.created(URI.create("/api/v1/contratos/" + c.id())).body(c);
    }

    @GetMapping
    public PaginaResponse<ContratoResponse> listar(
            @RequestParam(name = "status", required = false) StatusContrato status,
            @RequestParam(name = "cooperando_id", required = false) Long cooperandoId,
            @RequestParam(name = "cliente_id", required = false) Long clienteId,
            @RequestParam(name = "data_inicio_de", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate de,
            @RequestParam(name = "data_inicio_ate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate ate,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int size,
            @RequestParam(name = "sort", required = false) String sort,
            @AuthenticationPrincipal Ator ator) {
        var pg = Paginacao.de(page, size, sort, ORDENAVEIS, Sort.by(Sort.Direction.DESC, "id"));
        return PaginaResponse.de(service.listar(status, cooperandoId, clienteId, de, ate, ator, pg));
    }

    @GetMapping("/{id}")
    public ContratoResponse detalhar(@PathVariable("id") Long id, @AuthenticationPrincipal Ator ator) {
        return service.detalhar(id, ator);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('MOTORISTA','ADMINISTRADOR')")
    public ContratoResponse atualizar(@PathVariable("id") Long id, @Valid @RequestBody ContratoRequest r,
            @AuthenticationPrincipal Ator ator) {
        return service.atualizar(id, r, ator);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('MOTORISTA','ADMINISTRADOR')")
    public ResponseEntity<Void> excluir(@PathVariable("id") Long id, @AuthenticationPrincipal Ator ator) {
        service.excluir(id, ator);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/submissao")
    @PreAuthorize("hasAnyRole('MOTORISTA','ADMINISTRADOR')")
    public ContratoResponse submeter(@PathVariable("id") Long id, @AuthenticationPrincipal Ator ator) {
        return service.submeter(id, ator);
    }

    @PostMapping("/{id}/aprovacao")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ContratoResponse aprovar(@PathVariable("id") Long id, @AuthenticationPrincipal Ator ator) {
        return service.aprovar(id, ator);
    }

    @PostMapping("/{id}/reprovacao")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ContratoResponse reprovar(@PathVariable("id") Long id, @Valid @RequestBody ReprovarRequest r,
            @AuthenticationPrincipal Ator ator) {
        return service.reprovar(id, r.motivo(), ator);
    }

    @PostMapping("/{id}/encerramento")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ContratoResponse encerrar(@PathVariable("id") Long id, @AuthenticationPrincipal Ator ator) {
        return service.encerrar(id, ator);
    }

    @PostMapping("/{id}/cancelamento")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ContratoResponse cancelar(@PathVariable("id") Long id, @AuthenticationPrincipal Ator ator) {
        return service.cancelar(id, ator);
    }
}
