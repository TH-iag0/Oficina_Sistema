package br.com.example.autogestao_backend.repository;

import java.time.LocalDate;
import java.util.*;
import br.com.example.autogestao_backend.model.Veiculo;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

public interface VeiculoRepository extends JpaRepository<Veiculo, UUID> {
    Optional<Veiculo> findByPlaca(String placa);
    boolean existsByPlaca(String placa);
    Page<Veiculo> findByClienteIdAndAtivoTrue(UUID clienteId, Pageable pageable);

    // Inclui revisões vencidas; km é o último informado, não uma estimativa atual.
    @Query("""
        select v from Veiculo v where v.ativo = true and
        (v.proximaRevisaoData <= :limite or v.proximaRevisaoKm <= v.quilometragem)
        """)
    Page<Veiculo> findRevisoesAte(@Param("limite") LocalDate limite, Pageable pageable);
}
