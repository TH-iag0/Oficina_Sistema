package br.com.example.autogestao_backend.repository;

import java.time.*;
import java.util.UUID;
import br.com.example.autogestao_backend.model.JornadaTrabalho;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

public interface JornadaTrabalhoRepository extends JpaRepository<JornadaTrabalho, UUID> {
    @Query("""
        select j from JornadaTrabalho j where j.funcionario.id = :funcionarioId
        and j.diaSemana = :dia and j.vigenciaInicio <= :data
        and (j.vigenciaFim is null or j.vigenciaFim >= :data)
        order by j.vigenciaInicio desc
        """)
    Slice<JornadaTrabalho> findVigentes(@Param("funcionarioId") UUID funcionarioId,
        @Param("dia") DayOfWeek dia, @Param("data") LocalDate data, Pageable pageable);
}
