package br.ufla.cooperativa.contratos.persistence;

import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface ContratoRepository extends JpaRepository<Contrato, Long>, JpaSpecificationExecutor<Contrato> {
    @Override
    @EntityGraph(attributePaths = {"cooperando", "cliente"})
    Page<Contrato> findAll(Specification<Contrato> spec, Pageable pageable);

    @Override
    @EntityGraph(attributePaths = {"cooperando", "cliente"})
    Optional<Contrato> findOne(Specification<Contrato> spec);
}
