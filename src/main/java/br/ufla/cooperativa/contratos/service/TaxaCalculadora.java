package br.ufla.cooperativa.contratos.service;

import br.ufla.cooperativa.contratos.TaxaTipo;
import br.ufla.cooperativa.shared.RegraNegocioException;
import java.math.BigDecimal;
import java.math.RoundingMode;

/** RN02 e RN03: taxa por pagamento, arredondamento HALF_UP a 2 casas (D40). */
public final class TaxaCalculadora {
    private TaxaCalculadora() {}

    public record Valores(BigDecimal taxa, BigDecimal liquido) {}

    public static Valores calcular(TaxaTipo tipo, BigDecimal taxaValor, BigDecimal bruto) {
        BigDecimal taxa = tipo == TaxaTipo.PERCENTUAL
                ? bruto.multiply(taxaValor).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP)
                : taxaValor.setScale(2, RoundingMode.HALF_UP);
        if (taxa.compareTo(bruto) > 0) {
            throw new RegraNegocioException("RN03", "taxa_valor", "A taxa não pode exceder o valor bruto do pagamento");
        }
        return new Valores(taxa, bruto.subtract(taxa));
    }
}
