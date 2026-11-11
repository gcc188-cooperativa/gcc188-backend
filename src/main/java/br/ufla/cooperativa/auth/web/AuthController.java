package br.ufla.cooperativa.auth.web;

import br.ufla.cooperativa.auth.dto.AuthDtos.*;
import br.ufla.cooperativa.auth.service.AuthService;
import br.ufla.cooperativa.auth.service.AuthService.Sessao;
import br.ufla.cooperativa.shared.Ator;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {
    private final AuthService service;
    private final CookiesDeSessao cookies;

    public AuthController(AuthService service, CookiesDeSessao cookies) {
        this.service = service;
        this.cookies = cookies;
    }

    @PostMapping("/login")
    public MeResponse login(@Valid @RequestBody LoginRequest r, HttpServletResponse resp) {
        Sessao s = service.login(r.email(), r.senha());
        cookies.emitir(resp, s);
        return s.usuario();
    }

    @PostMapping("/refresh")
    public MeResponse refresh(@CookieValue(name = CookiesDeSessao.REFRESH, required = false) String refresh,
            HttpServletResponse resp) {
        try {
            Sessao s = service.renovar(refresh);
            cookies.emitir(resp, s);
            return s.usuario();
        } catch (RuntimeException e) {
            cookies.limpar(resp);
            throw e;
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@AuthenticationPrincipal Ator ator, HttpServletResponse resp) {
        service.sair(ator.id());
        cookies.limpar(resp);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/me")
    public MeResponse me(@AuthenticationPrincipal Ator ator) {
        return service.me(ator);
    }

    @PostMapping("/alterar-senha")
    public ResponseEntity<Void> alterarSenha(@Valid @RequestBody AlterarSenhaRequest r,
            @AuthenticationPrincipal Ator ator, HttpServletResponse resp) {
        Sessao s = service.alterarSenha(ator, r.senhaAtual(), r.novaSenha());
        cookies.emitir(resp, s);
        return ResponseEntity.noContent().build();
    }
}
