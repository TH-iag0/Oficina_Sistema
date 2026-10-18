package br.com.example.autogestao_backend.service;

import br.com.example.autogestao_backend.dto.ClienteEntrada;
import br.com.example.autogestao_backend.model.Cliente;
import br.com.example.autogestao_backend.model.TipoCliente;
import br.com.example.autogestao_backend.repository.ClienteRepository;
import br.com.example.autogestao_backend.shared.exception.ErroApi;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ClienteServiceTest {
    private final ClienteRepository repository = mock(ClienteRepository.class);
    private final Clock clock = Clock.fixed(Instant.parse("2026-10-11T12:00:00Z"), ZoneOffset.UTC);
    private final ClienteService service = new ClienteService(repository, clock);

    @Test
    void cadastraComHorarioDoServidorEDto() {
        when(repository.saveAndFlush(any(Cliente.class))).thenAnswer(call -> call.getArgument(0));
        var resposta = service.cadastrar(new ClienteEntrada(TipoCliente.PF, " João ", null, "11999999999"));
        assertEquals("João", resposta.nome());
        assertEquals(clock.instant(), resposta.cadastradoEm());
    }

    @Test
    void rejeitaDocumentoDuplicadoAposNormalizar() {
        when(repository.existsByDocumento("52998224725")).thenReturn(true);
        var clienteEntrada = new ClienteEntrada(TipoCliente.PF, "João", "529.982.247-25", null);
        assertThrows(ErroApi.class, () ->
            service.cadastrar(clienteEntrada));
        verify(repository, never()).saveAndFlush(any());
    }

    @Test
    void devolve404ParaClienteInexistente() {
        var requisicaoId = UUID.randomUUID();
        ErroApi erro = assertThrows(ErroApi.class, () -> service.buscar(requisicaoId));
        assertEquals(404, erro.getStatus().value());
    }

    @Test
    void desativaSemApagarHistorico() {
        Cliente cliente = new Cliente(TipoCliente.PF, "João", null, null, clock);
        when(repository.findById(cliente.getId())).thenReturn(Optional.of(cliente));
        service.desativar(cliente.getId());
        assertFalse(cliente.isAtivo());
        verify(repository, never()).delete(any());
    }

    @Test
    void rejeitaPaginacaoInvalidaMesmoSemController() {
        assertThrows(ErroApi.class, () -> service.listar("", -1, 20));
        assertThrows(ErroApi.class, () -> service.listar("", 0, 101));
        verify(repository, never()).findByAtivoTrueAndNomeContainingIgnoreCase(any(), any());
    }

    @Test
    void atualizacaoMantemCadastroOriginalEValidaDocumento() {
        Cliente cliente = new Cliente(TipoCliente.PF, "João", null, null, clock);
        when(repository.findById(cliente.getId())).thenReturn(Optional.of(cliente));
        var resposta = service.atualizar(cliente.getId(),
            new ClienteEntrada(TipoCliente.PF, " João Silva ", "529.982.247-25", "11999999999"));
        assertEquals(cliente.getId(), resposta.id());
        assertEquals(clock.instant(), resposta.cadastradoEm());
        assertEquals("52998224725", resposta.documento());
        var clienteId = cliente.getId();
        var clienteEntrada = new ClienteEntrada(TipoCliente.PF, "Nome inválido", "52998224726", null);
        assertThrows(ErroApi.class, () -> service.atualizar(clienteId,
            clienteEntrada));
        assertEquals("João Silva", cliente.getNome());
    }
}
