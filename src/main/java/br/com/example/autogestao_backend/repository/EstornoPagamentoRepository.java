package br.com.example.autogestao_backend.repository;

import br.com.example.autogestao_backend.model.EstornoPagamento;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface EstornoPagamentoRepository extends JpaRepository<EstornoPagamento, UUID> {
    @Query("select coalesce(sum(e.valor), 0) from EstornoPagamento e where e.pagamento.ordemServico.id = :id")
    BigDecimal somarPorOrdemServico(@Param("id") UUID id);
    Optional<EstornoPagamento> findByRequisicaoId(UUID requisicaoId);
    Page<EstornoPagamento> findByPagamentoId(UUID pagamentoId, Pageable pageable);

    @Query("select coalesce(sum(e.valor), 0) from EstornoPagamento e where e.pagamento.id = :pagamentoId")
    BigDecimal somarPorPagamento(@Param("pagamentoId") UUID pagamentoId);

    @Query("select coalesce(sum(e.valor), 0) from EstornoPagamento e where e.instante >= :inicio and e.instante < :fim")
    BigDecimal somarEstornos(@Param("inicio") Instant inicio, @Param("fim") Instant fim);
}
