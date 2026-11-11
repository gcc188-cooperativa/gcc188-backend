package br.ufla.cooperativa.cooperandos.persistence;

import br.ufla.cooperativa.shared.EntidadeAuditavel;
import jakarta.persistence.*;
import java.time.LocalDate;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "cooperando")
@Getter
@Setter
public class Cooperando extends EntidadeAuditavel {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "usuario_id", nullable = false, unique = true)
    private Long usuarioId;

    @Column(nullable = false, length = 20)
    private String matricula;

    @Column(nullable = false, length = 120)
    private String nome;

    @Column(nullable = false, length = 11)
    private String cpf;

    @Column(length = 20)
    private String telefone;

    @Column(name = "data_ingresso", nullable = false)
    private LocalDate dataIngresso;

    @Column(nullable = false)
    private boolean ativo = true;
}
