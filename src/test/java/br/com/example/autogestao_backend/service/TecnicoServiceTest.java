package br.com.example.autogestao_backend.service;

import br.com.example.autogestao_backend.dto.OficinaDtos;
import br.com.example.autogestao_backend.model.CategoriaServico;
import br.com.example.autogestao_backend.model.Remap;
import br.com.example.autogestao_backend.repository.DiagnosticoRepository;
import br.com.example.autogestao_backend.repository.RemapRepository;
import br.com.example.autogestao_backend.shared.exception.ErroApi;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class TecnicoServiceTest {
    @Test void naoCriaRemapParaItemDeManutencao() {
        var ordens = mock(OrdemServicoService.class);
        var item = FluxoFixtures.ordem(CategoriaServico.MANUTENCAO).getItens().getFirst();
        when(ordens.itemBloqueado(item.getId())).thenReturn(item);
        var service = new TecnicoService(ordens, mock(RemapRepository.class), mock(DiagnosticoRepository.class));
        var remapEntrada = new OficinaDtos.RemapEntrada(item.getId(), "Stage 1", "Bosch", "EDC17", "Resposta");
        assertThrows(IllegalArgumentException.class, () -> service.cadastrarRemap(remapEntrada));
    }

    @Test void rejeitaSegundoRemapDoMesmoItem() {
        var ordens = mock(OrdemServicoService.class);
        var repo = mock(RemapRepository.class);
        var item = FluxoFixtures.ordem(CategoriaServico.REMAP_ECU).getItens().getFirst();
        when(ordens.itemBloqueado(item.getId())).thenReturn(item);
        when(repo.findByItemId(item.getId())).thenReturn(Optional.of(new Remap(item, "Stage 1", "Bosch", "EDC17", "Resposta")));
        var service = new TecnicoService(ordens, repo, mock(DiagnosticoRepository.class));
        var remapEntrada = new OficinaDtos.RemapEntrada(item.getId(), "Stage 1", "Bosch", "EDC17", "Resposta");
        assertThrows(ErroApi.class, () -> service.cadastrarRemap(remapEntrada));
    }
}
