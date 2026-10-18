package br.com.example.autogestao_backend.repository;

import br.com.example.autogestao_backend.model.UsuarioAdmin;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UsuarioAdminRepository extends JpaRepository<UsuarioAdmin, UUID> {
    Optional<UsuarioAdmin> findByLogin(String login);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select u from UsuarioAdmin u where u.login = :login")
    Optional<UsuarioAdmin> bloquear(@Param("login") String login);
}
