package br.com.example.autogestao_backend.repository;

import br.com.example.autogestao_backend.model.CategoriaServico;
import br.com.example.autogestao_backend.model.ItemOrdemServico;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ItemOrdemServicoRepository extends JpaRepository<ItemOrdemServico, UUID> {
    @Query("select count(i) from ItemOrdemServico i where i.retrabalho = true and i.ordemServico.finalizadaEm >= :inicio and i.ordemServico.finalizadaEm < :fim")
    long contarRetrabalhos(@Param("inicio") Instant inicio, @Param("fim") Instant fim);
    @Query("select i.servico.id as servicoId, i.descricaoServico as nome, i.categoria as categoria, sum(i.quantidade) as quantidade, sum(i.precoUnitario * i.quantidade) as receita, sum(i.custoDiretoUnitario * i.quantidade) as custoDireto from ItemOrdemServico i where i.ordemServico.finalizadaEm >= :inicio and i.ordemServico.finalizadaEm < :fim group by i.servico.id, i.descricaoServico, i.categoria order by sum((i.precoUnitario - i.custoDiretoUnitario) * i.quantidade) desc, i.descricaoServico")
    List<ResumoServico> findRentabilidade(@Param("inicio") Instant inicio, @Param("fim") Instant fim, Pageable pagina);
    Page<ItemOrdemServico> findByOrdemServicoId(UUID ordemServicoId, Pageable pageable);

    interface ResumoServico {
        UUID getServicoId();
        String getNome();
        CategoriaServico getCategoria();
        Long getQuantidade();
        BigDecimal getReceita();
        BigDecimal getCustoDireto();
    }

    @Query("""
        select i.servico.id as servicoId, i.descricaoServico as nome, i.categoria as categoria,
        sum(i.quantidade) as quantidade, sum(i.precoUnitario * i.quantidade) as receita,
        sum(i.custoDiretoUnitario * i.quantidade) as custoDireto
        from ItemOrdemServico i
        where i.ordemServico.finalizadaEm >= :inicio and i.ordemServico.finalizadaEm < :fim
        and i.concluidoEm is not null and i.retrabalho = false
        group by i.servico.id, i.descricaoServico, i.categoria
        order by sum(i.quantidade) desc, i.descricaoServico asc, i.servico.id asc
        """)
    Slice<ResumoServico> findMaisExecutados(@Param("inicio") Instant inicio, @Param("fim") Instant fim, Pageable pageable);
}
