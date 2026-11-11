package br.ufla.cooperativa.auth.persistence;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {
    Optional<RefreshToken> findByTokenHash(String tokenHash);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update RefreshToken t set t.revogadoEm = :agora where t.usuarioId = :uid and t.revogadoEm is null")
    int revogarDoUsuario(@Param("uid") Long usuarioId, @Param("agora") Instant agora);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update RefreshToken t set t.revogadoEm = :agora where t.familia = :familia and t.revogadoEm is null")
    int revogarFamilia(@Param("familia") UUID familia, @Param("agora") Instant agora);

    /** Marca como usado de forma atômica; devolve 0 se outro pedido já o usou (indício de reuso). */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update RefreshToken t set t.usadoEm = :agora where t.id = :id and t.usadoEm is null")
    int marcarUsado(@Param("id") Long id, @Param("agora") Instant agora);
}
