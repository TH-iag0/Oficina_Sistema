package br.com.example.autogestao_backend.repository;

import br.com.example.autogestao_backend.model.OrdemServico;
import br.com.example.autogestao_backend.model.StatusOrdemServico;
import jakarta.persistence.LockModeType;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OrdemServicoRepository extends JpaRepository<OrdemServico, UUID> {
    @Query("""
        select o from OrdemServico o where (:status is null or o.status = :status) and (
        cast(o.numero as string) like concat('%', :busca, '%')
        or lower(o.cliente.nome) like lower(concat('%', :busca, '%'))
        or lower(o.veiculo.placa) like lower(concat('%', replace(:busca, '-', ''), '%'))
        or lower(o.veiculo.modelo) like lower(concat('%', :busca, '%'))
        or exists (select i.id from ItemOrdemServico i where i.ordemServico = o
        and lower(i.descricaoServico) like lower(concat('%', :busca, '%'))))
        """)
    Page<OrdemServico> pesquisar(@Param("busca") String busca, @Param("status") StatusOrdemServico status, Pageable pagina);
    @Query("select count(distinct o.cliente.id) from OrdemServico o where o.finalizadaEm >= :inicio and o.finalizadaEm < :fim")
    long contarClientesFinalizados(@Param("inicio") Instant inicio, @Param("fim") Instant fim);
    @Query("select count(distinct o.cliente.id) from OrdemServico o where o.recebidaEm >= :inicio and o.recebidaEm < :fim and o.status <> br.com.example.autogestao_backend.model.StatusOrdemServico.CANCELADO and exists (select a.id from OrdemServico a where a.cliente = o.cliente and a.recebidaEm < :inicio and a.status <> br.com.example.autogestao_backend.model.StatusOrdemServico.CANCELADO)")
    long contarRecorrentes(@Param("inicio") Instant inicio, @Param("fim") Instant fim);
    @Query("select o.veiculo.marca as nome, count(o) as quantidade from OrdemServico o where o.recebidaEm >= :inicio and o.recebidaEm < :fim and o.status <> br.com.example.autogestao_backend.model.StatusOrdemServico.CANCELADO group by o.veiculo.marca order by count(o) desc, o.veiculo.marca")
    List<GrupoQuantidade> marcas(@Param("inicio") Instant inicio, @Param("fim") Instant fim, Pageable pagina);
    @Query("select concat(o.veiculo.marca, ' ', o.veiculo.modelo) as nome, count(o) as quantidade from OrdemServico o where o.recebidaEm >= :inicio and o.recebidaEm < :fim and o.status <> br.com.example.autogestao_backend.model.StatusOrdemServico.CANCELADO group by o.veiculo.marca, o.veiculo.modelo order by count(o) desc, o.veiculo.marca, o.veiculo.modelo")
    List<GrupoQuantidade> modelos(@Param("inicio") Instant inicio, @Param("fim") Instant fim, Pageable pagina);
    interface IntervaloAtendimento {
        Instant getInicio();
        Instant getFim();
    }

    @Query("""
        select (select max(a.recebidaEm) from OrdemServico a
            where a.cliente = o.cliente and a.recebidaEm < o.recebidaEm
            and a.status <> br.com.example.autogestao_backend.model.StatusOrdemServico.CANCELADO) as inicio,
            o.recebidaEm as fim
        from OrdemServico o where o.recebidaEm >= :inicio and o.recebidaEm < :fim
        and o.status <> br.com.example.autogestao_backend.model.StatusOrdemServico.CANCELADO
        order by o.recebidaEm, o.id
        """)
    Slice<IntervaloAtendimento> findIntervalosVisitas(@Param("inicio") Instant inicio,
        @Param("fim") Instant fim, Pageable pagina);

    @Query("""
        select o.recebidaEm as inicio, o.finalizadaEm as fim from OrdemServico o
        where o.finalizadaEm >= :inicio and o.finalizadaEm < :fim
        order by o.finalizadaEm, o.id
        """)
    Slice<IntervaloAtendimento> findTemposConclusao(@Param("inicio") Instant inicio,
        @Param("fim") Instant fim, Pageable pagina);
    long countByStatus(StatusOrdemServico status);
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
