package br.ufla.cooperativa.contratos.service;

import br.ufla.cooperativa.contratos.Modalidade;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/** RN05: divisão do valor total (resíduo na última parcela) e datas de vencimento. */
public final class Parcelamento {
    private Parcelamento() {}

    public static List<BigDecimal> dividir(BigDecimal total, int n) {
        BigDecimal base = total.divide(BigDecimal.valueOf(n), 2, RoundingMode.HALF_UP);
        List<BigDecimal> r = new ArrayList<>();
        BigDecimal acumulado = BigDecimal.ZERO;
        for (int i = 0; i < n - 1; i++) {
            r.add(base);
            acumulado = acumulado.add(base);
        }
        r.add(total.subtract(acumulado).setScale(2, RoundingMode.HALF_UP));
        return r;
    }

    public static List<LocalDate> vencimentos(Modalidade modalidade, LocalDate inicio, Integer dia, int n) {
        List<LocalDate> r = new ArrayList<>();
        if (modalidade == Modalidade.UNICO) {
            r.add(inicio);
            return r;
        }
        LocalDate primeira = inicio.withDayOfMonth(dia);
        if (primeira.isBefore(inicio)) primeira = primeira.plusMonths(1);
        for (int i = 0; i < n; i++) r.add(primeira.plusMonths(i));
        return r;
    }
}
