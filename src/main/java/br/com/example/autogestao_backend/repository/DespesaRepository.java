package br.com.example.autogestao_backend.repository;

import java.math.BigDecimal;
import java.time.*;
import java.util.UUID;
import br.com.example.autogestao_backend.model.Despesa;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

public interface DespesaRepository extends JpaRepository<Despesa, UUID> {
    Page<Despesa> findByPagaEmIsNullAndVencimentoLessThanEqual(LocalDate limite, Pageable pageable);

    @Query("select coalesce(sum(d.valor), 0) from Despesa d where d.competencia >= :inicio and d.competencia < :fim")
    BigDecimal somarPorCompetencia(@Param("inicio") LocalDate inicio, @Param("fim") LocalDate fim);

    @Query("select coalesce(sum(d.valor), 0) from Despesa d where d.pagaEm >= :inicio and d.pagaEm < :fim")
    BigDecimal somarPagas(@Param("inicio") Instant inicio, @Param("fim") Instant fim);
}
