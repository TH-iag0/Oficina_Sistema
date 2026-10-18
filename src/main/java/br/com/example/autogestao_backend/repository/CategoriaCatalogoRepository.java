package br.com.example.autogestao_backend.repository;

import br.com.example.autogestao_backend.model.CategoriaCatalogo;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CategoriaCatalogoRepository extends JpaRepository<CategoriaCatalogo, UUID> {
    boolean existsByNomeNormalizado(String nome);
}
