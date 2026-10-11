package br.com.example.autogestao_backend.repository;

import java.util.*;
import br.com.example.autogestao_backend.model.Diagnostico;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DiagnosticoRepository extends JpaRepository<Diagnostico, UUID> {
    Optional<Diagnostico> findByItemId(UUID itemId);
    Page<Diagnostico> findByCausaIdentificadaContainingIgnoreCase(String causa, Pageable pageable);
}
