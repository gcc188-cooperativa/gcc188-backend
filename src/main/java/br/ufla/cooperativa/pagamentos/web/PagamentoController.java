package br.ufla.cooperativa.pagamentos.web;

import br.ufla.cooperativa.pagamentos.StatusPagamento;
import br.ufla.cooperativa.pagamentos.dto.PagamentoDtos.*;
import br.ufla.cooperativa.pagamentos.service.PagamentoService;
import br.ufla.cooperativa.shared.*;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1")
public class PagamentoController {
    private static final Map<String, String> ORDENAVEIS = Map.of("id", "id", "data_vencimento", "dataVencimento",
            "status", "status", "valor_bruto", "valorBruto", "numero_parcela", "numeroParcela");
    private final PagamentoService service;

    public PagamentoController(PagamentoService service) {
        this.service = service;
    }

    @GetMapping("/pagamentos")
    public PaginaResponse<PagamentoResponse> listar(
            @RequestParam(name = "contrato_id", required = false) Long contratoId,
            @RequestParam(name = "status", required = false) StatusPagamento status,
            @RequestParam(name = "vencimento_de", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate de,
            @RequestParam(name = "vencimento_ate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate ate,
            @RequestParam(name = "em_atraso", required = false) Boolean emAtraso,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int size,
            @RequestParam(name = "sort", required = false) String sort,
            @AuthenticationPrincipal Ator ator) {
        var pg = Paginacao.de(page, size, sort, ORDENAVEIS, Sort.by("dataVencimento").and(Sort.by("id")));
        return PaginaResponse.de(service.listar(contratoId, status, de, ate, emAtraso, ator, pg));
    }

    @GetMapping("/contratos/{id}/pagamentos")
    public List<PagamentoResponse> doContrato(@PathVariable("id") Long id, @AuthenticationPrincipal Ator ator) {
        return service.listarDoContrato(id, ator);
    }

    @GetMapping("/pagamentos/{id}")
    public PagamentoResponse buscar(@PathVariable("id") Long id, @AuthenticationPrincipal Ator ator) {
        return service.buscar(id, ator);
    }

    @PostMapping("/pagamentos/{id}/baixa")
    @PreAuthorize("hasRole('FINANCEIRO')")
    public PagamentoResponse baixar(@PathVariable("id") Long id, @Valid @RequestBody BaixaRequest r,
            @AuthenticationPrincipal Ator ator) {
        return service.baixar(id, r, ator);
    }

    @PostMapping("/pagamentos/{id}/repasse")
    @PreAuthorize("hasRole('FINANCEIRO')")
    public PagamentoResponse repassar(@PathVariable("id") Long id, @Valid @RequestBody RepasseRequest r,
            @AuthenticationPrincipal Ator ator) {
        return service.repassar(id, r, ator);
    }

    @PostMapping("/pagamentos/{id}/estorno")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR','FINANCEIRO')")
    public PagamentoResponse estornar(@PathVariable("id") Long id, @Valid @RequestBody EstornoRequest r,
            @AuthenticationPrincipal Ator ator) {
        return service.estornar(id, r, ator);
    }
}
