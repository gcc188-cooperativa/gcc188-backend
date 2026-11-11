package br.ufla.cooperativa.relatorios.persistence;

import br.ufla.cooperativa.contratos.StatusContrato;
import br.ufla.cooperativa.despesas.OrigemDespesa;
import br.ufla.cooperativa.pagamentos.StatusPagamento;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import org.springframework.stereotype.Repository;

/** Consultas de leitura (JPQL) com projeções; valores ausentes viram sentinelas para evitar parâmetros nulos. */
@Repository
public class RelatorioRepository {
    @PersistenceContext
    private EntityManager em;

    public List<Object[]> pagamentosDoCooperando(Long cooperandoId, LocalDate de, LocalDate ate) {
        return em.createQuery("""
                select p.id, p.contrato.id, p.numeroParcela, p.dataVencimento, p.valorBruto, p.valorTaxa,
                       p.valorLiquido, p.status, p.dataRepasse
                from Pagamento p
                where p.contrato.cooperando.id = :coop and p.dataVencimento between :de and :ate
                order by p.dataVencimento, p.id""", Object[].class)
                .setParameter("coop", cooperandoId).setParameter("de", de).setParameter("ate", ate).getResultList();
    }

    public List<Object[]> despesasDoCooperando(Long cooperandoId, LocalDate de, LocalDate ate) {
        return em.createQuery("""
                select d.id, d.categoria, d.descricao, d.valor, d.dataDespesa
                from Despesa d
                where d.cooperando.id = :coop and d.dataDespesa between :de and :ate
                order by d.dataDespesa, d.id""", Object[].class)
                .setParameter("coop", cooperandoId).setParameter("de", de).setParameter("ate", ate).getResultList();
    }

    public BigDecimal somaPagamentos(String campoValor, String campoData, Collection<StatusPagamento> status,
            LocalDate de, LocalDate ate) {
        // campoValor e campoData vêm de constantes internas do serviço, nunca da requisição.
        BigDecimal v = em.createQuery("select sum(p." + campoValor + ") from Pagamento p where p.status in :st and p."
                + campoData + " between :de and :ate", BigDecimal.class)
                .setParameter("st", status).setParameter("de", de).setParameter("ate", ate).getSingleResult();
        return v == null ? BigDecimal.ZERO : v;
    }

    public BigDecimal somaDespesas(OrigemDespesa origem, LocalDate de, LocalDate ate) {
        BigDecimal v = em.createQuery("select sum(d.valor) from Despesa d where d.origem = :o and d.dataDespesa between :de and :ate",
                BigDecimal.class).setParameter("o", origem).setParameter("de", de).setParameter("ate", ate).getSingleResult();
        return v == null ? BigDecimal.ZERO : v;
    }

    public List<Object[]> inadimplenciaPorCliente(LocalDate hoje) {
        return em.createQuery("""
                select cl.id, cl.nome, count(p), sum(p.valorBruto)
                from Pagamento p join p.contrato c join c.cliente cl
                where p.status = :st and p.dataVencimento < :hoje
                group by cl.id, cl.nome order by sum(p.valorBruto) desc""", Object[].class)
                .setParameter("st", StatusPagamento.PREVISTO).setParameter("hoje", hoje).getResultList();
    }

    public List<Object[]> inadimplenciaPorCooperando(LocalDate hoje) {
        return em.createQuery("""
                select co.id, co.nome, count(p), sum(p.valorBruto)
                from Pagamento p join p.contrato c join c.cooperando co
                where p.status = :st and p.dataVencimento < :hoje
                group by co.id, co.nome order by sum(p.valorBruto) desc""", Object[].class)
                .setParameter("st", StatusPagamento.PREVISTO).setParameter("hoje", hoje).getResultList();
    }

    /** coop e cli: 0 significa "todos" (ids são sempre positivos). */
    public long contarContratos(StatusContrato status, long coop, long cli) {
        return em.createQuery("""
                select count(c) from Contrato c
                where c.status = :st and (:coop = 0L or c.cooperando.id = :coop) and (:cli = 0L or c.cliente.id = :cli)""",
                Long.class).setParameter("st", status).setParameter("coop", coop).setParameter("cli", cli).getSingleResult();
    }

    /** Devolve {quantidade, soma do bruto} dos pagamentos no estado, com vencimento no intervalo, no escopo. */
    public Object[] agregarPagamentos(StatusPagamento status, LocalDate de, LocalDate ate, long coop, long cli) {
        return em.createQuery("""
                select count(p), sum(p.valorBruto) from Pagamento p join p.contrato c
                where p.status = :st and p.dataVencimento between :de and :ate
                  and (:coop = 0L or c.cooperando.id = :coop) and (:cli = 0L or c.cliente.id = :cli)""", Object[].class)
                .setParameter("st", status).setParameter("de", de).setParameter("ate", ate)
                .setParameter("coop", coop).setParameter("cli", cli).getSingleResult();
    }

    public List<Object[]> proximosVencimentos(LocalDate de, LocalDate ate, long coop, long cli, int limite) {
        return em.createQuery("""
                select p.id, c.id, p.dataVencimento, p.valorBruto from Pagamento p join p.contrato c
                where p.status = :st and p.dataVencimento between :de and :ate
                  and (:coop = 0L or c.cooperando.id = :coop) and (:cli = 0L or c.cliente.id = :cli)
                order by p.dataVencimento, p.id""", Object[].class)
                .setParameter("st", StatusPagamento.PREVISTO).setParameter("de", de).setParameter("ate", ate)
                .setParameter("coop", coop).setParameter("cli", cli).setMaxResults(limite).getResultList();
    }
}
