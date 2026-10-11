package br.com.example.autogestao_backend.repository;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;
import br.com.example.autogestao_backend.model.*;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

public interface OrdemServicoRepository extends JpaRepository<OrdemServico, UUID> {
    Optional<OrdemServico> findByNumero(long numero);
    boolean existsByNumero(long numero);
    Page<OrdemServico> findByStatusIn(Collection<StatusOrdemServico> status, Pageable pageable);
    Page<OrdemServico> findByClienteId(UUID clienteId, Pageable pageable);
    Page<OrdemServico> findByVeiculoId(UUID veiculoId, Pageable pageable);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select o from OrdemServico o where o.id = :id")
    Optional<OrdemServico> findParaAtualizacao(@Param("id") UUID id);

    @Query("select o from OrdemServico o where o.finalizadaEm >= :inicio and o.finalizadaEm < :fim")
    Page<OrdemServico> findFinalizadasNoPeriodo(@Param("inicio") Instant inicio, @Param("fim") Instant fim, Pageable pageable);

    @Query("select coalesce(sum(o.receitaFinalizada), 0) from OrdemServico o where o.finalizadaEm >= :inicio and o.finalizadaEm < :fim")
    BigDecimal somarFaturamento(@Param("inicio") Instant inicio, @Param("fim") Instant fim);

    @Query("select coalesce(sum(o.custoDiretoFinalizado), 0) from OrdemServico o where o.finalizadaEm >= :inicio and o.finalizadaEm < :fim")
    BigDecimal somarCustosDiretos(@Param("inicio") Instant inicio, @Param("fim") Instant fim);

    @Query("select count(o) from OrdemServico o where o.finalizadaEm >= :inicio and o.finalizadaEm < :fim")
    long contarFinalizadas(@Param("inicio") Instant inicio, @Param("fim") Instant fim);

    @Query("""
        select count(distinct o.cliente.id) from OrdemServico o
        where o.recebidaEm >= :inicio and o.recebidaEm < :fim
        and o.status <> br.com.example.autogestao_backend.model.StatusOrdemServico.CANCELADO
        """)
    long contarClientesAtendidos(@Param("inicio") Instant inicio, @Param("fim") Instant fim);
}
