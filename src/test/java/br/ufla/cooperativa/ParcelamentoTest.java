package br.ufla.cooperativa;

import static org.assertj.core.api.Assertions.assertThat;

import br.ufla.cooperativa.contratos.Modalidade;
import br.ufla.cooperativa.contratos.service.Parcelamento;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class ParcelamentoTest {

    @Test
    void divisaoExata() {
        var p = Parcelamento.dividir(new BigDecimal("6000.00"), 3);
        assertThat(p).hasSize(3).allSatisfy(v -> assertThat(v).isEqualByComparingTo("2000.00"));
    }

    @Test
    void residuoVaiParaAUltimaParcelaESomaFecha() {
        var total = new BigDecimal("100.00");
        var p = Parcelamento.dividir(total, 3);
        assertThat(p.get(0)).isEqualByComparingTo("33.33");
        assertThat(p.get(1)).isEqualByComparingTo("33.33");
        assertThat(p.get(2)).isEqualByComparingTo("33.34");
        assertThat(p.stream().reduce(BigDecimal.ZERO, BigDecimal::add)).isEqualByComparingTo(total);
    }

    @Test
    void somaSempreIgualAoTotal() {
        for (int n = 2; n <= 24; n++) {
            var total = new BigDecimal("1234.57");
            assertThat(Parcelamento.dividir(total, n).stream().reduce(BigDecimal.ZERO, BigDecimal::add))
                    .isEqualByComparingTo(total);
        }
    }

    @Test
    void unicoVenceNaDataInicio() {
        var d = Parcelamento.vencimentos(Modalidade.UNICO, LocalDate.of(2026, 11, 10), null, 1);
        assertThat(d).containsExactly(LocalDate.of(2026, 11, 10));
    }

    @Test
    void mensalPrimeiraOcorrenciaDoDiaEmOuApósInicio() {
        var depois = Parcelamento.vencimentos(Modalidade.MENSAL, LocalDate.of(2026, 11, 10), 5, 3);
        assertThat(depois).containsExactly(LocalDate.of(2026, 12, 5), LocalDate.of(2027, 1, 5), LocalDate.of(2027, 2, 5));
        var mesmoMes = Parcelamento.vencimentos(Modalidade.MENSAL, LocalDate.of(2026, 11, 10), 15, 2);
        assertThat(mesmoMes).containsExactly(LocalDate.of(2026, 11, 15), LocalDate.of(2026, 12, 15));
        var mesmoDia = Parcelamento.vencimentos(Modalidade.MENSAL, LocalDate.of(2026, 11, 10), 10, 2);
        assertThat(mesmoDia.get(0)).isEqualTo(LocalDate.of(2026, 11, 10));
    }
}
