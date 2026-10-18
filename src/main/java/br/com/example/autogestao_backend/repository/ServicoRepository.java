package br.com.example.autogestao_backend.repository;

import br.com.example.autogestao_backend.model.CategoriaServico;
import br.com.example.autogestao_backend.model.Servico;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;

public interface ServicoRepository extends JpaRepository<Servico, UUID> {
    @EntityGraph(attributePaths = "categoriaCatalogo")
    @org.springframework.data.jpa.repository.Query("""
        select s from Servico s where (:ativo is null or s.ativo = :ativo)
        and lower(s.nome) like lower(concat('%', :busca, '%'))
        """)
    Page<Servico> pesquisar(String busca, Boolean ativo, Pageable pageable);

    Page<Servico> findByAtivoTrueAndNomeContainingIgnoreCase(String nome, Pageable pageable);
    Page<Servico> findByAtivoTrueAndCategoria(CategoriaServico categoria, Pageable pageable);
}
