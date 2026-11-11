package br.ufla.cooperativa.auth.service;

import br.ufla.cooperativa.auth.persistence.RefreshTokenRepository;
import br.ufla.cooperativa.shared.TempoNegocio;
import br.ufla.cooperativa.usuarios.service.SessoesInvalidadas;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Revoga os refresh tokens quando o módulo de usuários inativa ou redefine a senha de alguém. */
@Component
public class SessoesListener {
    private final RefreshTokenRepository refreshTokens;
    private final TempoNegocio tempo;

    public SessoesListener(RefreshTokenRepository refreshTokens, TempoNegocio tempo) {
        this.refreshTokens = refreshTokens;
        this.tempo = tempo;
    }

    @EventListener
    @Transactional
    public void aoInvalidar(SessoesInvalidadas e) {
        refreshTokens.revogarDoUsuario(e.usuarioId(), tempo.agora());
    }
}
