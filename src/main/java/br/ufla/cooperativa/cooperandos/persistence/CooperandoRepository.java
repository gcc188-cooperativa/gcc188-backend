package br.ufla.cooperativa.cooperandos.persistence;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface CooperandoRepository extends JpaRepository<Cooperando, Long>, JpaSpecificationExecutor<Cooperando> {
    boolean existsByMatricula(String matricula);

    boolean existsByCpf(String cpf);

    boolean existsByMatriculaAndIdNot(String matricula, Long id);

    boolean existsByCpfAndIdNot(String cpf, Long id);

    Optional<Cooperando> findByUsuarioId(Long usuarioId);
}
