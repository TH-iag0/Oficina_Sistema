package br.com.example.autogestao_backend.repository;

import br.com.example.autogestao_backend.model.ExecucaoServico;
import java.time.Instant;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ExecucaoServicoRepository extends JpaRepository<ExecucaoServico, UUID> {
    boolean existsByItemOrdemServicoIdAndFimIsNull(UUID ordemServicoId);
    boolean existsByFuncionarioIdAndFimIsNull(UUID funcionarioId);
    boolean existsByItemIdAndFimIsNull(UUID itemId);
    Page<ExecucaoServico> findByItemId(UUID itemId, Pageable pageable);

    // Inclui períodos que começaram antes do filtro, mas ainda se sobrepõem a ele.
    @Query("""
        select e from ExecucaoServico e where e.funcionario.id = :funcionarioId
        and e.inicio < :fim and (e.fim is null or e.fim > :inicio)
        order by e.inicio asc, e.id asc
        """)
    Page<ExecucaoServico> findSobrepostas(@Param("funcionarioId") UUID funcionarioId,
        @Param("inicio") Instant inicio, @Param("fim") Instant fim, Pageable pageable);
}
