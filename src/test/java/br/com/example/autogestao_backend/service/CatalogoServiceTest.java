package br.com.example.autogestao_backend.service;

import br.com.example.autogestao_backend.dto.OficinaDtos;
import br.com.example.autogestao_backend.model.CategoriaServico;
import br.com.example.autogestao_backend.repository.ServicoRepository;
import br.com.example.autogestao_backend.repository.CategoriaCatalogoRepository;
import java.math.BigDecimal;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CatalogoServiceTest {
    @Test void alteracaoDoCatalogoPreservaPrecoHistoricoDaOs() {
        var repo = mock(ServicoRepository.class);
        var service = new CatalogoService(repo, mock(CategoriaCatalogoRepository.class));
        var ordem = FluxoFixtures.ordem(CategoriaServico.MANUTENCAO);
        var item = ordem.getItens().getFirst();
        var servico = item.getServico();
        when(repo.findById(servico.getId())).thenReturn(Optional.of(servico));
        when(repo.saveAndFlush(any())).thenAnswer(call -> call.getArgument(0));
        service.atualizar(servico.getId(), new OficinaDtos.ServicoEntrada("Novo nome", CategoriaServico.OUTRO, new BigDecimal("200"), new BigDecimal("40")));
        assertEquals(new BigDecimal("100.00"), item.getPrecoUnitario());
        assertEquals("Teste", item.getDescricaoServico());
        assertEquals("Novo nome", servico.getNome());
    }

    @Test void precoInvalidoNaoModificaNome() {
        var repo = mock(ServicoRepository.class);
        var service = new CatalogoService(repo, mock(CategoriaCatalogoRepository.class));
        var servico = FluxoFixtures.ordem(CategoriaServico.MANUTENCAO).getItens().getFirst().getServico();
        when(repo.findById(servico.getId())).thenReturn(Optional.of(servico));
        var servicoId = servico.getId();
        var servicoEntrada = new OficinaDtos.ServicoEntrada("Nome", CategoriaServico.OUTRO, new BigDecimal("-1"), BigDecimal.ZERO);
        assertThrows(IllegalArgumentException.class, () -> service.atualizar(servicoId,
            servicoEntrada));
        assertEquals("Teste", servico.getNome());
    }
}
