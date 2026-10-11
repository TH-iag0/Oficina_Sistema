package br.com.example.autogestao_backend.repository;

import java.time.Instant;
import java.util.UUID;
import br.com.example.autogestao_backend.model.ExecucaoServico;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

public interface ExecucaoServicoRepository extends JpaRepository<ExecucaoServico, UUID> {
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
