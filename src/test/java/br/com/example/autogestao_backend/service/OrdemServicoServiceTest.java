package br.com.example.autogestao_backend.service;

import br.com.example.autogestao_backend.dto.OficinaDtos;
import br.com.example.autogestao_backend.model.CategoriaServico;
import br.com.example.autogestao_backend.repository.ClienteRepository;
import br.com.example.autogestao_backend.repository.ExecucaoServicoRepository;
import br.com.example.autogestao_backend.repository.FuncionarioRepository;
import br.com.example.autogestao_backend.repository.ItemOrdemServicoRepository;
import br.com.example.autogestao_backend.repository.NumeradorOsRepository;
import br.com.example.autogestao_backend.repository.OrdemServicoRepository;
import br.com.example.autogestao_backend.repository.ServicoRepository;
import br.com.example.autogestao_backend.repository.VeiculoRepository;
import br.com.example.autogestao_backend.shared.exception.ErroApi;
import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class OrdemServicoServiceTest {
    private final OrdemServicoRepository ordens = mock(OrdemServicoRepository.class);
    private final ItemOrdemServicoRepository itens = mock(ItemOrdemServicoRepository.class);
    private final ExecucaoServicoRepository execucoes = mock(ExecucaoServicoRepository.class);
    private final OrdemServicoService service = new OrdemServicoService(ordens, mock(NumeradorOsRepository.class),
        mock(ClienteRepository.class), mock(VeiculoRepository.class), mock(ServicoRepository.class), itens, execucoes,
        mock(FuncionarioRepository.class), FluxoFixtures.CLOCK);
    @Test void exigeEncerrarExecucoesAntesDeConcluirItem() {
        var ordem = FluxoFixtures.emServico(CategoriaServico.MANUTENCAO);
        var item = ordem.getItens().getFirst();
        when(itens.findById(item.getId())).thenReturn(Optional.of(item));
        when(ordens.findParaAtualizacao(ordem.getId())).thenReturn(Optional.of(ordem));
        when(execucoes.existsByItemIdAndFimIsNull(item.getId())).thenReturn(true);
        var itemId = item.getId();
        var conclusaoEntrada = new OficinaDtos.ConclusaoEntrada(new BigDecimal("30"), null, null);
        assertThrows(ErroApi.class, () -> service.concluirItem(itemId, conclusaoEntrada));
    }

    @Test void osInexistenteRetornaErro() {
        var requisicaoId = UUID.randomUUID();
        assertThrows(ErroApi.class, () -> service.buscar(requisicaoId));
    }
}
