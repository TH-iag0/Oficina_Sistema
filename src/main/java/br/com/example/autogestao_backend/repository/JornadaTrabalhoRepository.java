package br.com.example.autogestao_backend.repository;

import br.com.example.autogestao_backend.model.JornadaTrabalho;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface JornadaTrabalhoRepository extends JpaRepository<JornadaTrabalho, UUID> {
    @Query("""
        select j from JornadaTrabalho j where j.funcionario.id = :funcionarioId
        and j.vigenciaInicio < :fim and (j.vigenciaFim is null or j.vigenciaFim >= :inicio)
        order by j.vigenciaInicio desc, j.id
        """)
    List<JornadaTrabalho> findSobrepostas(@Param("funcionarioId") UUID funcionarioId,
        @Param("inicio") LocalDate inicio, @Param("fim") LocalDate fim);
    List<JornadaTrabalho> findByFuncionarioIdAndDiaSemana(UUID funcionarioId, DayOfWeek dia);
    List<JornadaTrabalho> findByFuncionarioId(UUID funcionarioId);
    @Query("""
        select j from JornadaTrabalho j where j.funcionario.id = :funcionarioId
        and j.diaSemana = :dia and j.vigenciaInicio <= :data
        and (j.vigenciaFim is null or j.vigenciaFim >= :data)
        order by j.vigenciaInicio desc
        """)
    Slice<JornadaTrabalho> findVigentes(@Param("funcionarioId") UUID funcionarioId,
        @Param("dia") DayOfWeek dia, @Param("data") LocalDate data, Pageable pageable);
}
