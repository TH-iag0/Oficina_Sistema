package br.com.example.autogestao_backend.repository;

import br.com.example.autogestao_backend.model.Funcionario;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface FuncionarioRepository extends JpaRepository<Funcionario, UUID> {
    Optional<Funcionario> findByMatricula(String matricula);
    @Query("select f.id from Funcionario f where f.matricula = :matricula")
    Optional<UUID> findIdByMatricula(@Param("matricula") String matricula);
    boolean existsByMatricula(String matricula);
    Page<Funcionario> findByAtivoTrue(Pageable pageable);

    /** Exige transação no serviço para serializar marcações do mesmo funcionário. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select f from Funcionario f where f.id = :id")
    Optional<Funcionario> findParaAtualizacao(@Param("id") UUID id);
}
