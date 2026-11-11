package br.ufla.cooperativa.pagamentos.persistence;

import br.ufla.cooperativa.pagamentos.StatusPagamento;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface PagamentoRepository extends JpaRepository<Pagamento, Long>, JpaSpecificationExecutor<Pagamento> {
    @Override
    @EntityGraph(attributePaths = "contrato")
    Page<Pagamento> findAll(Specification<Pagamento> spec, Pageable pageable);

    @Override
    @EntityGraph(attributePaths = "contrato")
    Optional<Pagamento> findOne(Specification<Pagamento> spec);

    @EntityGraph(attributePaths = "contrato")
    List<Pagamento> findByContratoIdOrderByNumeroParcela(Long contratoId);

    List<Pagamento> findByContratoIdAndStatus(Long contratoId, StatusPagamento status);

    boolean existsByContratoIdAndStatusIn(Long contratoId, Collection<StatusPagamento> status);
}
