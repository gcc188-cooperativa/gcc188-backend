package br.ufla.cooperativa.despesas.persistence;

import br.ufla.cooperativa.contratos.persistence.Contrato;
import br.ufla.cooperativa.cooperandos.persistence.Cooperando;
import br.ufla.cooperativa.despesas.CategoriaDespesa;
import br.ufla.cooperativa.despesas.OrigemDespesa;
import br.ufla.cooperativa.shared.EntidadeAuditavel;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "despesa")
@Getter
@Setter
public class Despesa extends EntidadeAuditavel {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 12)
    private OrigemDespesa origem;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15)
    private CategoriaDespesa categoria;

    @Column(nullable = false, length = 300)
    private String descricao;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal valor;

    @Column(name = "data_despesa", nullable = false)
    private LocalDate dataDespesa;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cooperando_id")
    private Cooperando cooperando;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "contrato_id")
    private Contrato contrato;

    @Column(name = "registrado_por_id", nullable = false)
    private Long registradoPorId;

    @Version
    private Long versao;
}
