package br.com.example.autogestao_backend.repository;

import java.util.*;
import br.com.example.autogestao_backend.model.*;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RemapRepository extends JpaRepository<Remap, UUID> {
    Optional<Remap> findByItemId(UUID itemId);
    Page<Remap> findByItemCategoria(CategoriaServico categoria, Pageable pageable);
}
