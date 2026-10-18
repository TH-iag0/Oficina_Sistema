package br.com.example.autogestao_backend.service;

import br.com.example.autogestao_backend.dto.OficinaDtos;
import br.com.example.autogestao_backend.model.Veiculo;
import br.com.example.autogestao_backend.repository.ClienteRepository;
import br.com.example.autogestao_backend.repository.VeiculoRepository;
import br.com.example.autogestao_backend.shared.exception.ErroApi;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class VeiculoServiceTest {
    private final VeiculoRepository veiculos = mock(VeiculoRepository.class);
    private final ClienteRepository clientes = mock(ClienteRepository.class);
    private final VeiculoService service = new VeiculoService(veiculos, clientes);
    @Test void normalizaPlacaAntesDeVerificarDuplicidade() {
        var cliente = FluxoFixtures.cliente();
        when(clientes.findById(cliente.getId())).thenReturn(Optional.of(cliente));
        when(veiculos.existsByPlaca("ABC1234")).thenReturn(true);
        var veiculoEntrada = new OficinaDtos.VeiculoEntrada(cliente.getId(), "abc-1234", "VW", "Golf", 0);
        assertThrows(ErroApi.class, () -> service.cadastrar(veiculoEntrada));
        verify(veiculos, never()).saveAndFlush(any());
    }

    @Test void naoPermiteQuilometragemRegredir() {
        var veiculo = new Veiculo(FluxoFixtures.cliente(), "ABC1D23", "VW", "Golf", 100);
        when(veiculos.findById(veiculo.getId())).thenReturn(Optional.of(veiculo));
        var veiculoId = veiculo.getId();
        var veiculoEdicao = new OficinaDtos.VeiculoEdicao("Ford", "Fiesta", 99);
        assertThrows(IllegalArgumentException.class, () -> service.atualizar(veiculoId, veiculoEdicao));
        assertEquals("VW", veiculo.getMarca());
    }

    @Test void clienteInativoNaoRecebeNovoVeiculo() {
        var cliente = FluxoFixtures.cliente();
        cliente.desativar();
        when(clientes.findById(cliente.getId())).thenReturn(Optional.of(cliente));
        var veiculoEntrada = new OficinaDtos.VeiculoEntrada(cliente.getId(), "ABC1D23", "VW", "Golf", 0);
        assertThrows(ErroApi.class, () -> service.cadastrar(veiculoEntrada));
    }

    @Test void idAusenteRetorna404() {
        var requisicaoId = UUID.randomUUID();
        assertEquals(404, assertThrows(ErroApi.class, () -> service.buscar(requisicaoId)).getStatus().value());
    }
}
