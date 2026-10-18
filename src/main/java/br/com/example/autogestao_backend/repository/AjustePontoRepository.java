package br.com.example.autogestao_backend.repository;

import br.com.example.autogestao_backend.model.AjustePonto;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AjustePontoRepository extends JpaRepository<AjustePonto, UUID> {
    Optional<AjustePonto> findByRequisicaoId(UUID requisicaoId);
    boolean existsByOriginalId(UUID originalId);
}
