package br.com.example.autogestao_backend.repository;

import java.util.UUID;
import br.com.example.autogestao_backend.model.HistoricoStatusOs;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HistoricoStatusOsRepository extends JpaRepository<HistoricoStatusOs, UUID> {
    Page<HistoricoStatusOs> findByOrdemServicoIdOrderByInstanteAsc(UUID ordemServicoId, Pageable pageable);
}
