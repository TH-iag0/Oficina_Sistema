package br.com.example.autogestao_backend.repository;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;
import br.com.example.autogestao_backend.model.EstornoPagamento;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

public interface EstornoPagamentoRepository extends JpaRepository<EstornoPagamento, UUID> {
    Optional<EstornoPagamento> findByRequisicaoId(UUID requisicaoId);
    Page<EstornoPagamento> findByPagamentoId(UUID pagamentoId, Pageable pageable);

    @Query("select coalesce(sum(e.valor), 0) from EstornoPagamento e where e.pagamento.id = :pagamentoId")
    BigDecimal somarPorPagamento(@Param("pagamentoId") UUID pagamentoId);

    @Query("select coalesce(sum(e.valor), 0) from EstornoPagamento e where e.instante >= :inicio and e.instante < :fim")
    BigDecimal somarEstornos(@Param("inicio") Instant inicio, @Param("fim") Instant fim);
}
