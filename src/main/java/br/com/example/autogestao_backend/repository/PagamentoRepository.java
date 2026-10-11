package br.com.example.autogestao_backend.repository;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;
import br.com.example.autogestao_backend.model.Pagamento;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

public interface PagamentoRepository extends JpaRepository<Pagamento, UUID> {
    Optional<Pagamento> findByRequisicaoId(UUID requisicaoId);
    Page<Pagamento> findByOrdemServicoId(UUID ordemServicoId, Pageable pageable);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Pagamento p where p.id = :id")
    Optional<Pagamento> findParaAtualizacao(@Param("id") UUID id);

    @Query("select coalesce(sum(p.valor), 0) from Pagamento p where p.recebidoEm >= :inicio and p.recebidoEm < :fim")
    BigDecimal somarRecebimentos(@Param("inicio") Instant inicio, @Param("fim") Instant fim);

    @Query("select coalesce(sum(p.valor), 0) from Pagamento p where p.ordemServico.id = :ordemServicoId")
    BigDecimal somarPorOrdemServico(@Param("ordemServicoId") UUID ordemServicoId);
}
