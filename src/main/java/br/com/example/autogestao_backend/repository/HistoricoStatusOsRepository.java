package br.com.example.autogestao_backend.repository;

import br.com.example.autogestao_backend.model.HistoricoStatusOs;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HistoricoStatusOsRepository extends JpaRepository<HistoricoStatusOs, UUID> {
    Page<HistoricoStatusOs> findByOrdemServicoIdOrderByInstanteAsc(UUID ordemServicoId, Pageable pageable);
}
