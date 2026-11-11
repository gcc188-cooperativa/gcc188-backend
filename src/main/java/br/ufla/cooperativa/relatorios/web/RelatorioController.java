package br.ufla.cooperativa.relatorios.web;

import br.ufla.cooperativa.relatorios.dto.RelatorioDtos.*;
import br.ufla.cooperativa.relatorios.service.RelatorioService;
import br.ufla.cooperativa.shared.Ator;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1")
public class RelatorioController {
    private final RelatorioService service;

    public RelatorioController(RelatorioService service) {
        this.service = service;
    }

    @GetMapping("/cooperandos/{id}/extrato")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR','FINANCEIRO','MOTORISTA')")
    public ExtratoResponse extrato(@PathVariable("id") Long id,
            @RequestParam(name = "data_de", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate de,
            @RequestParam(name = "data_ate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate ate,
            @AuthenticationPrincipal Ator ator) {
        return service.extrato(id, de, ate, ator);
    }

    @GetMapping("/relatorios/resumo-financeiro")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR','FINANCEIRO')")
    public ResumoFinanceiroResponse resumo(
            @RequestParam(name = "data_de", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate de,
            @RequestParam(name = "data_ate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate ate) {
        return service.resumo(de, ate);
    }

    @GetMapping("/relatorios/inadimplencia")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR','FINANCEIRO')")
    public InadimplenciaResponse inadimplencia() {
        return service.inadimplencia();
    }

    @GetMapping("/painel/indicadores")
    public PainelResponse painel(@AuthenticationPrincipal Ator ator) {
        return service.painel(ator);
    }
}
