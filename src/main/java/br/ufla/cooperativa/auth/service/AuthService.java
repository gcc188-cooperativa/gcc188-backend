package br.ufla.cooperativa.auth.service;

import br.ufla.cooperativa.auth.dto.AuthDtos.MeResponse;
import br.ufla.cooperativa.auth.persistence.RefreshToken;
import br.ufla.cooperativa.auth.persistence.RefreshTokenRepository;
import br.ufla.cooperativa.shared.*;
import br.ufla.cooperativa.usuarios.persistence.Usuario;
import br.ufla.cooperativa.usuarios.persistence.UsuarioRepository;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.UUID;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {
    public record Sessao(String acesso, String refresh, MeResponse usuario) {}

    private final UsuarioRepository usuarios;
    private final RefreshTokenRepository refreshTokens;
    private final PasswordEncoder encoder;
    private final JwtService jwt;
    private final VinculoResolver vinculos;
    private final TempoNegocio tempo;
    private final AppProperties props;
    private final PoliticaSenha politica;
    private final SecureRandom random = new SecureRandom();
    private final String hashFalso;

    public AuthService(UsuarioRepository usuarios, RefreshTokenRepository refreshTokens, PasswordEncoder encoder,
            JwtService jwt, VinculoResolver vinculos, TempoNegocio tempo, AppProperties props, PoliticaSenha politica) {
        this.usuarios = usuarios;
        this.refreshTokens = refreshTokens;
        this.encoder = encoder;
        this.jwt = jwt;
        this.vinculos = vinculos;
        this.tempo = tempo;
        this.props = props;
        this.politica = politica;
        this.hashFalso = encoder.encode("senha-falsa-para-igualar-tempo");
    }

    private static NaoAutenticadoException falha() {
        return new NaoAutenticadoException("Credenciais inválidas");
    }

    /** Mesma resposta para e-mail inexistente, senha errada, conta inativa ou bloqueada. */
    @Transactional(noRollbackFor = NaoAutenticadoException.class)
    public Sessao login(String email, String senha) {
        Usuario u = usuarios.findByEmailIgnoreCase(email.trim()).orElse(null);
        if (u == null) {
            encoder.matches(senha, hashFalso);
            throw falha();
        }
        Instant agora = tempo.agora();
        if (u.getBloqueadoAte() != null && u.getBloqueadoAte().isAfter(agora)) throw falha();
        boolean senhaOk = encoder.matches(senha, u.getSenhaHash());
        if (!u.isAtivo() || !senhaOk) {
            if (u.isAtivo()) {
                u.setFalhasLogin(u.getFalhasLogin() + 1);
                if (u.getFalhasLogin() >= props.auth().maxFalhas()) {
                    u.setBloqueadoAte(agora.plus(props.auth().bloqueio()));
                    u.setFalhasLogin(0);
                }
                usuarios.save(u);
            }
            throw falha();
        }
        u.setFalhasLogin(0);
        u.setBloqueadoAte(null);
        usuarios.save(u);
        return emitir(u, UUID.randomUUID());
    }

    /** Rotação: o token usado é marcado e um novo da mesma família é emitido; reuso revoga a família (CA-12). */
    @Transactional(noRollbackFor = NaoAutenticadoException.class)
    public Sessao renovar(String refreshCru) {
        if (refreshCru == null || refreshCru.isBlank()) throw falha();
        RefreshToken rt = refreshTokens.findByTokenHash(hash(refreshCru)).orElseThrow(AuthService::falha);
        Instant agora = tempo.agora();
        if (rt.getRevogadoEm() != null || rt.getExpiraEm().isBefore(agora)) throw falha();
        if (rt.getUsadoEm() != null || refreshTokens.marcarUsado(rt.getId(), agora) == 0) {
            refreshTokens.revogarFamilia(rt.getFamilia(), agora);
            throw falha();
        }
        Usuario u = usuarios.findById(rt.getUsuarioId()).orElseThrow(AuthService::falha);
        if (!u.isAtivo()) {
            refreshTokens.revogarDoUsuario(u.getId(), agora);
            throw falha();
        }
        return emitir(u, rt.getFamilia());
    }

    @Transactional
    public void sair(Long usuarioId) {
        refreshTokens.revogarDoUsuario(usuarioId, tempo.agora());
    }

    @Transactional
    public Sessao alterarSenha(Ator ator, String senhaAtual, String novaSenha) {
        Usuario u = usuarios.findById(ator.id()).orElseThrow(AuthService::falha);
        if (!encoder.matches(senhaAtual, u.getSenhaHash()))
            throw new RegraNegocioException("SENHA_ATUAL", "senha_atual", "Senha atual incorreta");
        politica.validar(novaSenha, "nova_senha");
        if (encoder.matches(novaSenha, u.getSenhaHash()))
            throw new RegraNegocioException("SENHA_REPETIDA", "nova_senha", "A nova senha deve ser diferente da atual");
        u.setSenhaHash(encoder.encode(novaSenha));
        u.setDeveTrocarSenha(false);
        usuarios.saveAndFlush(u);
        refreshTokens.revogarDoUsuario(u.getId(), tempo.agora());
        return emitir(u, UUID.randomUUID());
    }

    @Transactional(readOnly = true)
    public MeResponse me(Ator ator) {
        Usuario u = usuarios.findById(ator.id()).orElseThrow(AuthService::falha);
        return paraMe(u, vinculos.de(u));
    }

    private Sessao emitir(Usuario u, UUID familia) {
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        String refresh = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        Instant agora = tempo.agora();
        RefreshToken rt = new RefreshToken();
        rt.setUsuarioId(u.getId());
        rt.setTokenHash(hash(refresh));
        rt.setFamilia(familia);
        rt.setCriadoEm(agora);
        rt.setExpiraEm(agora.plus(props.auth().refreshTtl()));
        refreshTokens.save(rt);
        Long vinculo = vinculos.de(u);
        return new Sessao(jwt.gerar(u, vinculo), refresh, paraMe(u, vinculo));
    }

    private static MeResponse paraMe(Usuario u, Long vinculo) {
        return new MeResponse(u.getId(), u.getNome(), u.getEmail(), u.getPapel(), vinculo, u.isDeveTrocarSenha());
    }

    private static String hash(String valor) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(valor.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
