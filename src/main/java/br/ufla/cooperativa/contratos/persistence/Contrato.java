package br.ufla.cooperativa.contratos.persistence;

import br.ufla.cooperativa.clientes.persistence.Cliente;
import br.ufla.cooperativa.contratos.Modalidade;
import br.ufla.cooperativa.contratos.StatusContrato;
import br.ufla.cooperativa.contratos.TaxaTipo;
import br.ufla.cooperativa.cooperandos.persistence.Cooperando;
import br.ufla.cooperativa.shared.EntidadeAuditavel;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "contrato")
@Getter
@Setter
public class Contrato extends EntidadeAuditavel {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cooperando_id", nullable = false)
    private Cooperando cooperando;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cliente_id", nullable = false)
    private Cliente cliente;

    @Column(nullable = false, length = 500)
    private String descricao;

    @Column(name = "valor_total", nullable = false, precision = 12, scale = 2)
    private BigDecimal valorTotal;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Modalidade modalidade;

    @JdbcTypeCode(SqlTypes.SMALLINT)
    @Column(name = "num_parcelas", nullable = false)
    private int numParcelas;

    @JdbcTypeCode(SqlTypes.SMALLINT)
    @Column(name = "dia_vencimento")
    private Integer diaVencimento;

    @Column(name = "data_inicio", nullable = false)
    private LocalDate dataInicio;

    @Column(name = "data_fim")
    private LocalDate dataFim;

    @Enumerated(EnumType.STRING)
    @Column(name = "taxa_tipo", nullable = false, length = 10)
    private TaxaTipo taxaTipo;

    @Column(name = "taxa_valor", nullable = false, precision = 12, scale = 2)
    private BigDecimal taxaValor;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StatusContrato status;

    @Column(name = "motivo_reprovacao", length = 300)
    private String motivoReprovacao;

    @Column(name = "criado_por_id", nullable = false)
    private Long criadoPorId;

    @Column(name = "aprovado_por_id")
    private Long aprovadoPorId;

    @Column(name = "aprovado_em")
    private Instant aprovadoEm;

    @Version
    private Long versao;
}
