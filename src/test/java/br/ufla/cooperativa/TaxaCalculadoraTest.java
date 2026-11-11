package br.ufla.cooperativa;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.ufla.cooperativa.contratos.TaxaTipo;
import br.ufla.cooperativa.contratos.service.TaxaCalculadora;
import br.ufla.cooperativa.shared.RegraNegocioException;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class TaxaCalculadoraTest {

    @Test
    void percentualCalculaTaxaELiquido() {
        var v = TaxaCalculadora.calcular(TaxaTipo.PERCENTUAL, new BigDecimal("10"), new BigDecimal("2000.00"));
        assertThat(v.taxa()).isEqualByComparingTo("200.00");
        assertThat(v.liquido()).isEqualByComparingTo("1800.00");
    }

    @Test
    void percentualArredondaHalfUp() {
        var v = TaxaCalculadora.calcular(TaxaTipo.PERCENTUAL, new BigDecimal("2.5"), new BigDecimal("0.10"));
        assertThat(v.taxa()).isEqualByComparingTo("0.00");
        var w = TaxaCalculadora.calcular(TaxaTipo.PERCENTUAL, new BigDecimal("5"), new BigDecimal("0.10"));
        assertThat(w.taxa()).isEqualByComparingTo("0.01"); // 0,005 arredonda para cima
    }

    @Test
    void fixoUsaOValorPorPagamento() {
        var v = TaxaCalculadora.calcular(TaxaTipo.FIXO, new BigDecimal("50"), new BigDecimal("400.00"));
        assertThat(v.taxa()).isEqualByComparingTo("50.00");
        assertThat(v.liquido()).isEqualByComparingTo("350.00");
    }

    @Test
    void taxaMaiorQueOBrutoViolaRN03() {
        assertThatThrownBy(() -> TaxaCalculadora.calcular(TaxaTipo.FIXO, new BigDecimal("500"), new BigDecimal("400.00")))
                .isInstanceOf(RegraNegocioException.class)
                .satisfies(e -> {
                    var ex = (RegraNegocioException) e;
                    assertThat(ex.getCodigoRegra()).isEqualTo("RN03");
                    assertThat(ex.getCampo()).isEqualTo("taxa_valor");
                });
    }
}
