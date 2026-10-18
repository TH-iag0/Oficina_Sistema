package br.com.example.autogestao_backend.repository;

import br.com.example.autogestao_backend.model.Diagnostico;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DiagnosticoRepository extends JpaRepository<Diagnostico, UUID> {
    @Query("select count(d) from Diagnostico d where d.item.ordemServico.recebidaEm >= :inicio and d.item.ordemServico.recebidaEm < :fim and d.item.ordemServico.status <> br.com.example.autogestao_backend.model.StatusOrdemServico.CANCELADO")
    long contarNoPeriodo(@Param("inicio") Instant inicio, @Param("fim") Instant fim);
    @Query("select count(d) from Diagnostico d where d.item.ordemServico.recebidaEm >= :inicio and d.item.ordemServico.recebidaEm < :fim and d.reparoAprovado is not null and d.item.ordemServico.status <> br.com.example.autogestao_backend.model.StatusOrdemServico.CANCELADO")
    long contarConvertidos(@Param("inicio") Instant inicio, @Param("fim") Instant fim);
    @Query("select d.causaIdentificada as nome, count(d) as quantidade from Diagnostico d where d.item.ordemServico.recebidaEm >= :inicio and d.item.ordemServico.recebidaEm < :fim and d.item.ordemServico.status <> br.com.example.autogestao_backend.model.StatusOrdemServico.CANCELADO and d.causaIdentificada is not null group by d.causaIdentificada order by count(d) desc, d.causaIdentificada")
    List<GrupoQuantidade> causas(@Param("inicio") Instant inicio, @Param("fim") Instant fim, Pageable pagina);
    Optional<Diagnostico> findByItemId(UUID itemId);
    Page<Diagnostico> findByCausaIdentificadaContainingIgnoreCase(String causa, Pageable pageable);
}
