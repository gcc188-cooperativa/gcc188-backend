package br.ufla.cooperativa.clientes.persistence;

import br.ufla.cooperativa.shared.EntidadeAuditavel;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "cliente")
@Getter
@Setter
public class Cliente extends EntidadeAuditavel {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "usuario_id", nullable = false, unique = true)
    private Long usuarioId;

    @Column(nullable = false, length = 120)
    private String nome;

    @Column(nullable = false, length = 14)
    private String documento;

    @Column(length = 20)
    private String telefone;

    @Column(name = "email_contato", length = 160)
    private String emailContato;

    @Column(nullable = false)
    private boolean ativo = true;
}
