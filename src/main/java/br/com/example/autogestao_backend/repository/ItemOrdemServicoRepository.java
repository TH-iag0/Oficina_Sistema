package br.com.example.autogestao_backend.repository;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;
import br.com.example.autogestao_backend.model.*;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

public interface ItemOrdemServicoRepository extends JpaRepository<ItemOrdemServico, UUID> {
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
