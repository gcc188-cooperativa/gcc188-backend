package br.ufla.cooperativa.cooperados.persistence;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface CooperadoRepository extends JpaRepository<Cooperado, Long>, JpaSpecificationExecutor<Cooperado> {
    boolean existsByMatricula(String matricula);

    boolean existsByCpf(String cpf);

    boolean existsByMatriculaAndIdNot(String matricula, Long id);

    boolean existsByCpfAndIdNot(String cpf, Long id);

    Optional<Cooperado> findByUsuarioId(Long usuarioId);
}
