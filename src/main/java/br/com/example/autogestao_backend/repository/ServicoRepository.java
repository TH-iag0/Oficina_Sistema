package br.com.example.autogestao_backend.repository;

import java.util.UUID;
import br.com.example.autogestao_backend.model.*;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ServicoRepository extends JpaRepository<Servico, UUID> {
    Page<Servico> findByAtivoTrueAndNomeContainingIgnoreCase(String nome, Pageable pageable);
    Page<Servico> findByAtivoTrueAndCategoria(CategoriaServico categoria, Pageable pageable);
}
