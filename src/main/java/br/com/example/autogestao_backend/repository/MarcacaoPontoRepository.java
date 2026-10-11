package br.com.example.autogestao_backend.repository;

import java.time.Instant;
import java.util.*;
import br.com.example.autogestao_backend.model.MarcacaoPonto;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

public interface MarcacaoPontoRepository extends JpaRepository<MarcacaoPonto, UUID> {
    Optional<MarcacaoPonto> findByFuncionario_IdAndRequisicaoId(UUID funcionarioId, UUID requisicaoId);
    Optional<MarcacaoPonto> findFirstByFuncionario_IdOrderByInstanteDesc(UUID funcionarioId);

    @Query("""
        select m from MarcacaoPonto m where m.funcionario.id = :funcionarioId
        and m.instante >= :inicio and m.instante < :fim order by m.instante asc, m.id asc
        """)
    Page<MarcacaoPonto> findNoPeriodo(@Param("funcionarioId") UUID funcionarioId,
        @Param("inicio") Instant inicio, @Param("fim") Instant fim, Pageable pageable);
}
