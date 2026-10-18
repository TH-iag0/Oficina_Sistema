package br.com.example.autogestao_backend.repository;

import br.com.example.autogestao_backend.model.Veiculo;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface VeiculoRepository extends JpaRepository<Veiculo, UUID> {
    @Query("""
        select v from Veiculo v where (:clienteId is null or v.cliente.id = :clienteId)
        and (:ativo is null or v.ativo = :ativo)
        and (lower(v.placa) like lower(concat('%', replace(:busca, '-', ''), '%'))
            or lower(v.marca) like lower(concat('%', :busca, '%'))
            or lower(v.modelo) like lower(concat('%', :busca, '%')))
        """)
    Page<Veiculo> pesquisar(String busca, UUID clienteId, Boolean ativo, Pageable pageable);

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
