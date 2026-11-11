package br.ufla.cooperativa.auth.service;

import br.ufla.cooperativa.shared.AppProperties;
import br.ufla.cooperativa.shared.Ator;
import br.ufla.cooperativa.shared.Papel;
import br.ufla.cooperativa.usuarios.persistence.Usuario;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jose.crypto.MACVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;

/** JWT de acesso HS256 (Nimbus JOSE): claims sub, papel, vinculo, trocar_senha, iat, exp, jti. */
@Service
public class JwtService {
    public record Claims(Ator ator, boolean trocarSenha) {}

    private final byte[] chave;
    private final Duration ttl;
    private final Clock clock;

    public JwtService(AppProperties props, Clock clock) {
        String s = props.auth().jwtSecret();
        if (s == null || s.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalStateException("app.auth.jwt-secret deve ter ao menos 32 bytes (256 bits)");
        }
        this.chave = s.getBytes(StandardCharsets.UTF_8);
        this.ttl = props.auth().accessTtl();
        this.clock = clock;
    }

    public String gerar(Usuario u, Long vinculo) {
        try {
            Instant agora = clock.instant();
            JWTClaimsSet.Builder b = new JWTClaimsSet.Builder()
                    .subject(String.valueOf(u.getId()))
                    .claim("papel", u.getPapel().name())
                    .claim("trocar_senha", u.isDeveTrocarSenha())
                    .issueTime(Date.from(agora))
                    .expirationTime(Date.from(agora.plus(ttl)))
                    .jwtID(UUID.randomUUID().toString());
            if (vinculo != null) b.claim("vinculo", vinculo);
            SignedJWT jwt = new SignedJWT(new JWSHeader(JWSAlgorithm.HS256), b.build());
            jwt.sign(new MACSigner(chave));
            return jwt.serialize();
        } catch (Exception e) {
            throw new IllegalStateException("Falha ao assinar o token", e);
        }
    }

    public Optional<Claims> validar(String token) {
        try {
            SignedJWT jwt = SignedJWT.parse(token);
            if (!JWSAlgorithm.HS256.equals(jwt.getHeader().getAlgorithm())) return Optional.empty();
            if (!jwt.verify(new MACVerifier(chave))) return Optional.empty();
            JWTClaimsSet c = jwt.getJWTClaimsSet();
            Date exp = c.getExpirationTime();
            if (exp == null || !exp.toInstant().isAfter(clock.instant())) return Optional.empty();
            Long id = Long.valueOf(c.getSubject());
            Papel papel = Papel.valueOf(c.getStringClaim("papel"));
            Long vinculo = c.getClaim("vinculo") == null ? null : c.getLongClaim("vinculo");
            boolean trocar = Boolean.TRUE.equals(c.getBooleanClaim("trocar_senha"));
            return Optional.of(new Claims(new Ator(id, papel, vinculo), trocar));
        } catch (Exception e) {
            return Optional.empty();
        }
    }
}
