package br.com.example.autogestao_backend.repository;

import br.com.example.autogestao_backend.model.Despesa;
import jakarta.persistence.LockModeType;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DespesaRepository extends JpaRepository<Despesa, UUID> {
    @org.springframework.data.jpa.repository.Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select d from Despesa d where d.id = :id")
    Optional<Despesa> findParaAtualizacao(@Param("id") UUID id);
    Page<Despesa> findByPagaEmIsNullAndVencimentoLessThanEqual(LocalDate limite, Pageable pageable);

    @Query("select coalesce(sum(d.valor), 0) from Despesa d where d.competencia >= :inicio and d.competencia < :fim")
    BigDecimal somarPorCompetencia(@Param("inicio") LocalDate inicio, @Param("fim") LocalDate fim);

    @Query("select coalesce(sum(d.valor), 0) from Despesa d where d.pagaEm >= :inicio and d.pagaEm < :fim")
    BigDecimal somarPagas(@Param("inicio") Instant inicio, @Param("fim") Instant fim);

    interface CategoriaTotal {
        String getNome();
        BigDecimal getValor();
    }

    @Query("""
        select d.categoria as nome, sum(d.valor) as valor from Despesa d
        where d.competencia >= :inicio and d.competencia < :fim
        group by d.categoria order by sum(d.valor) desc, d.categoria
        """)
    List<CategoriaTotal> somarPorCategoria(@Param("inicio") LocalDate inicio, @Param("fim") LocalDate fim);
}
