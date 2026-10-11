package br.com.example.autogestao_backend.repository;

import java.util.*;
import br.com.example.autogestao_backend.model.Funcionario;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

public interface FuncionarioRepository extends JpaRepository<Funcionario, UUID> {
    Optional<Funcionario> findByMatricula(String matricula);
    boolean existsByMatricula(String matricula);
    Page<Funcionario> findByAtivoTrue(Pageable pageable);

    /** Exige transação no serviço para serializar marcações do mesmo funcionário. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select f from Funcionario f where f.id = :id")
    Optional<Funcionario> findParaAtualizacao(@Param("id") UUID id);
}
