package br.ufla.cooperativa.despesas.web;

import br.ufla.cooperativa.despesas.CategoriaDespesa;
import br.ufla.cooperativa.despesas.OrigemDespesa;
import br.ufla.cooperativa.despesas.dto.DespesaDtos.*;
import br.ufla.cooperativa.despesas.service.DespesaService;
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
@RequestMapping("/api/v1/despesas")
@PreAuthorize("hasAnyRole('ADMINISTRADOR','FINANCEIRO','MOTORISTA')")
public class DespesaController {
    private static final Map<String, String> ORDENAVEIS = Map.of("id", "id", "data_despesa", "dataDespesa",
            "valor", "valor", "categoria", "categoria");
    private final DespesaService service;

    public DespesaController(DespesaService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<DespesaResponse> registrar(@Valid @RequestBody DespesaRequest r, @AuthenticationPrincipal Ator ator) {
        DespesaResponse d = service.registrar(r, ator);
        return ResponseEntity.created(URI.create("/api/v1/despesas/" + d.id())).body(d);
    }

    @GetMapping
    public PaginaResponse<DespesaResponse> listar(
            @RequestParam(name = "origem", required = false) OrigemDespesa origem,
            @RequestParam(name = "categoria", required = false) CategoriaDespesa categoria,
            @RequestParam(name = "cooperando_id", required = false) Long cooperandoId,
            @RequestParam(name = "data_de", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate de,
            @RequestParam(name = "data_ate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate ate,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int size,
            @RequestParam(name = "sort", required = false) String sort,
            @AuthenticationPrincipal Ator ator) {
        var pg = Paginacao.de(page, size, sort, ORDENAVEIS, Sort.by(Sort.Direction.DESC, "dataDespesa").and(Sort.by("id")));
        return PaginaResponse.de(service.listar(origem, categoria, cooperandoId, de, ate, ator, pg));
    }

    @GetMapping("/totais")
    public TotaisDespesaResponse totais(
            @RequestParam(name = "origem", required = false) OrigemDespesa origem,
            @RequestParam(name = "data_de", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate de,
            @RequestParam(name = "data_ate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate ate,
            @AuthenticationPrincipal Ator ator) {
        return service.totais(origem, de, ate, ator);
    }

    @GetMapping("/{id}")
    public DespesaResponse buscar(@PathVariable("id") Long id, @AuthenticationPrincipal Ator ator) {
        return service.buscar(id, ator);
    }

    @PutMapping("/{id}")
    public DespesaResponse atualizar(@PathVariable("id") Long id, @Valid @RequestBody DespesaRequest r,
            @AuthenticationPrincipal Ator ator) {
        return service.atualizar(id, r, ator);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> excluir(@PathVariable("id") Long id, @AuthenticationPrincipal Ator ator) {
        service.excluir(id, ator);
        return ResponseEntity.noContent().build();
    }
}
