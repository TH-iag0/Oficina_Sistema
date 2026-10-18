package br.com.example.autogestao_backend.repository;

import br.com.example.autogestao_backend.model.MarcacaoPonto;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MarcacaoPontoRepository extends JpaRepository<MarcacaoPonto, UUID> {
    @Query("select m from MarcacaoPonto m where m.funcionario.id = :id and not exists (select a.id from AjustePonto a where a.original = m) order by m.instante desc, m.id desc")
    List<MarcacaoPonto> findUltimasVigentes(@Param("id") UUID id, Pageable pageable);

    @Query("select count(a) from AjustePonto a where a.original.id = :id")
    long contarAjustes(@Param("id") UUID id);

    @Query("select m from MarcacaoPonto m where m.funcionario.id = :id and m.instante >= :inicio and m.instante < :fim and not exists (select a.id from AjustePonto a where a.original = m) order by m.instante asc, m.id asc")
    List<MarcacaoPonto> findVigentes(@Param("id") UUID id, @Param("inicio") Instant inicio, @Param("fim") Instant fim);
    Optional<MarcacaoPonto> findByFuncionario_IdAndRequisicaoId(UUID funcionarioId, UUID requisicaoId);
    Optional<MarcacaoPonto> findFirstByFuncionario_IdOrderByInstanteDesc(UUID funcionarioId);

    @Query("""
        select m from MarcacaoPonto m where m.funcionario.id = :funcionarioId
        and m.instante >= :inicio and m.instante < :fim order by m.instante asc, m.id asc
        """)
    Page<MarcacaoPonto> findNoPeriodo(@Param("funcionarioId") UUID funcionarioId,
        @Param("inicio") Instant inicio, @Param("fim") Instant fim, Pageable pageable);
}
