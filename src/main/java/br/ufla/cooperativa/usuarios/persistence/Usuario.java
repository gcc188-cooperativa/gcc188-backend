package br.ufla.cooperativa.usuarios.persistence;

import br.ufla.cooperativa.shared.EntidadeAuditavel;
import br.ufla.cooperativa.shared.Papel;
import jakarta.persistence.*;
import java.time.Instant;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "usuario")
@Getter
@Setter
public class Usuario extends EntidadeAuditavel {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 120)
    private String nome;

    @Column(nullable = false, length = 160)
    private String email;

    @Column(name = "senha_hash", nullable = false)
    private String senhaHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15)
    private Papel papel;

    @Column(nullable = false)
    private boolean ativo = true;

    @Column(name = "deve_trocar_senha", nullable = false)
    private boolean deveTrocarSenha;

    @JdbcTypeCode(SqlTypes.SMALLINT)
    @Column(name = "falhas_login", nullable = false)
    private int falhasLogin;

    @Column(name = "bloqueado_ate")
    private Instant bloqueadoAte;

    @Version
    private Long versao;
}
