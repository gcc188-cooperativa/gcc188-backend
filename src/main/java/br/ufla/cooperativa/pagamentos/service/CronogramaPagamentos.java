package br.ufla.cooperativa.pagamentos.service;

import br.ufla.cooperativa.contratos.persistence.Contrato;
import br.ufla.cooperativa.contratos.service.Parcelamento;
import br.ufla.cooperativa.contratos.service.TaxaCalculadora;
import br.ufla.cooperativa.contratos.service.TaxaCalculadora.Valores;
import br.ufla.cooperativa.pagamentos.StatusPagamento;
import br.ufla.cooperativa.pagamentos.persistence.Pagamento;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;

/** RN03 e RN05: gera as parcelas com bruto, taxa e líquido congelados; soma dos brutos = valor total. */
@Component
public class CronogramaPagamentos {

    public List<Pagamento> gerar(Contrato c) {
        List<BigDecimal> brutos = Parcelamento.dividir(c.getValorTotal(), c.getNumParcelas());
        List<LocalDate> datas = Parcelamento.vencimentos(c.getModalidade(), c.getDataInicio(), c.getDiaVencimento(), c.getNumParcelas());
        List<Pagamento> r = new ArrayList<>();
        for (int i = 0; i < brutos.size(); i++) {
            Valores v = TaxaCalculadora.calcular(c.getTaxaTipo(), c.getTaxaValor(), brutos.get(i));
            Pagamento p = new Pagamento();
            p.setContrato(c);
            p.setNumeroParcela(i + 1);
            p.setDataVencimento(datas.get(i));
            p.setValorBruto(brutos.get(i));
            p.setValorTaxa(v.taxa());
            p.setValorLiquido(v.liquido());
            p.setStatus(StatusPagamento.PREVISTO);
            r.add(p);
        }
        return r;
    }
}
