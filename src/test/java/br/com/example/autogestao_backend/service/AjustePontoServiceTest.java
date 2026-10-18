package br.com.example.autogestao_backend.service;

import br.com.example.autogestao_backend.dto.OficinaDtos;
import br.com.example.autogestao_backend.model.Funcionario;
import br.com.example.autogestao_backend.model.MarcacaoPonto;
import br.com.example.autogestao_backend.model.TipoMarcacao;
import br.com.example.autogestao_backend.repository.AjustePontoRepository;
import br.com.example.autogestao_backend.repository.FuncionarioRepository;
import br.com.example.autogestao_backend.repository.MarcacaoPontoRepository;
import br.com.example.autogestao_backend.shared.exception.ErroApi;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AjustePontoServiceTest {
    private final FuncionarioRepository funcionarios = mock(FuncionarioRepository.class);
    private final MarcacaoPontoRepository marcacoes = mock(MarcacaoPontoRepository.class);
    private final AjustePontoRepository ajustes = mock(AjustePontoRepository.class);
    private final AjustePontoService service = new AjustePontoService(ajustes, marcacoes, funcionarios, FluxoFixtures.CLOCK, FluxoFixtures.CLOCK.getZone());
    @Test void correcaoPreservaOriginalERegistraAutor() {
        var f = funcionario();
        var original = MarcacaoPonto.registrar(f, TipoMarcacao.ENTRADA, UUID.randomUUID(), FluxoFixtures.CLOCK, FluxoFixtures.CLOCK.getZone());
        when(marcacoes.findById(original.getId())).thenReturn(Optional.of(original));
        when(ajustes.saveAndFlush(any())).thenAnswer(call -> call.getArgument(0));
        var resposta = service.ajustar(new OficinaDtos.AjustePontoEntrada(f.getId(), original.getId(), null, null, "Duplicidade", UUID.randomUUID()), "admin");
        assertEquals(original.getId(), resposta.originalId());
        assertEquals("admin", resposta.autor());
        assertEquals(TipoMarcacao.ENTRADA, original.getTipo());
    }

    @Test void rejeitaHorarioFuturo() {
        var f = funcionario();
        var ajustePontoEntrada = new OficinaDtos.AjustePontoEntrada(f.getId(), null, TipoMarcacao.ENTRADA,
            FluxoFixtures.CLOCK.instant().plusSeconds(1), "Ajuste", UUID.randomUUID());
        assertThrows(ErroApi.class, () -> service.ajustar(ajustePontoEntrada, "admin"));
    }

    @Test void naoAjustaMarcacaoDeOutroFuncionario() {
        var f = funcionario();
        var outro = new Funcionario("Outro", "OF2", new PinAuthenticator().codificar("123456"));
        var original = MarcacaoPonto.registrar(outro, TipoMarcacao.ENTRADA, UUID.randomUUID(), FluxoFixtures.CLOCK, FluxoFixtures.CLOCK.getZone());
        when(marcacoes.findById(original.getId())).thenReturn(Optional.of(original));
        var ajustePontoEntrada = new OficinaDtos.AjustePontoEntrada(f.getId(), original.getId(), null, null, "Ajuste", UUID.randomUUID());
        assertThrows(ErroApi.class, () -> service.ajustar(ajustePontoEntrada, "admin"));
    }

    private Funcionario funcionario() {
        var f = new Funcionario("Carlos", "OF1", new PinAuthenticator().codificar("123456"));
        when(funcionarios.findParaAtualizacao(f.getId())).thenReturn(Optional.of(f));
        return f;
    }
}
