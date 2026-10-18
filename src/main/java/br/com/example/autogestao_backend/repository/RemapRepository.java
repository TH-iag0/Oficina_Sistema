package br.com.example.autogestao_backend.repository;

import br.com.example.autogestao_backend.model.CategoriaServico;
import br.com.example.autogestao_backend.model.Remap;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RemapRepository extends JpaRepository<Remap, UUID> {
    @Query("select count(r) from Remap r where r.item.categoria = :categoria and r.item.ordemServico.finalizadaEm >= :inicio and r.item.ordemServico.finalizadaEm < :fim")
    long contarCategoriaNoPeriodo(@Param("categoria") CategoriaServico categoria, @Param("inicio") Instant inicio, @Param("fim") Instant fim);
    Optional<Remap> findByItemId(UUID itemId);
    Page<Remap> findByItemCategoria(CategoriaServico categoria, Pageable pageable);
}
