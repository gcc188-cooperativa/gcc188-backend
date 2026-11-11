package br.ufla.cooperativa.pagamentos.persistence;

import br.ufla.cooperativa.contratos.persistence.Contrato;
import br.ufla.cooperativa.pagamentos.StatusPagamento;
import br.ufla.cooperativa.shared.EntidadeAuditavel;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "pagamento")
@Getter
@Setter
public class Pagamento extends EntidadeAuditavel {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "contrato_id", nullable = false)
    private Contrato contrato;

    @JdbcTypeCode(SqlTypes.SMALLINT)
    @Column(name = "numero_parcela", nullable = false)
    private int numeroParcela;

    @Column(name = "data_vencimento", nullable = false)
    private LocalDate dataVencimento;

    @Column(name = "valor_bruto", nullable = false, precision = 12, scale = 2)
    private BigDecimal valorBruto;

    @Column(name = "valor_taxa", nullable = false, precision = 12, scale = 2)
    private BigDecimal valorTaxa;

    @Column(name = "valor_liquido", nullable = false, precision = 12, scale = 2)
    private BigDecimal valorLiquido;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private StatusPagamento status;

    @Column(name = "data_baixa")
    private LocalDate dataBaixa;

    @Column(name = "baixado_por_id")
    private Long baixadoPorId;

    @Column(name = "data_repasse")
    private LocalDate dataRepasse;

    @Column(name = "repassado_por_id")
    private Long repassadoPorId;

    @Column(length = 300)
    private String observacao;

    @Version
    private Long versao;
}
