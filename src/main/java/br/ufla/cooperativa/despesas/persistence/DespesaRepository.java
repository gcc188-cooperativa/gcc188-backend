package br.ufla.cooperativa.despesas.persistence;

import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface DespesaRepository extends JpaRepository<Despesa, Long>, JpaSpecificationExecutor<Despesa> {
    @Override
    @EntityGraph(attributePaths = {"cooperando", "contrato"})
    Page<Despesa> findAll(Specification<Despesa> spec, Pageable pageable);

    @Override
    @EntityGraph(attributePaths = {"cooperando", "contrato"})
    Optional<Despesa> findOne(Specification<Despesa> spec);
}
