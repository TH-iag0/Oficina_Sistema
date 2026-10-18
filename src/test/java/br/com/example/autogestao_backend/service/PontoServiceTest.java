package br.com.example.autogestao_backend.service;

import br.com.example.autogestao_backend.dto.PontoEntrada;
import br.com.example.autogestao_backend.model.Funcionario;
import br.com.example.autogestao_backend.model.MarcacaoPonto;
import br.com.example.autogestao_backend.model.TipoMarcacao;
import br.com.example.autogestao_backend.repository.FuncionarioRepository;
import br.com.example.autogestao_backend.repository.MarcacaoPontoRepository;
import br.com.example.autogestao_backend.shared.exception.ErroApi;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PontoServiceTest {
    private final FuncionarioRepository funcionarios = mock(FuncionarioRepository.class);
    private final MarcacaoPontoRepository marcacoes = mock(MarcacaoPontoRepository.class);
    private final ZoneId fuso = ZoneId.of("America/Sao_Paulo");
    private final Clock clock = Clock.fixed(Instant.parse("2026-10-11T02:00:00Z"), fuso);
    private final Funcionario funcionario = new Funcionario("Carlos", "OF-001", new BCryptPasswordEncoder(4).encode("123456"));
    private final PontoService service = new PontoService(funcionarios, marcacoes, new PinAuthenticator(), clock, fuso);

    @Test
    void usaHorarioEDiaDoServidor() {
        preparar();
        when(marcacoes.saveAndFlush(any())).thenAnswer(call -> call.getArgument(0));
        var resposta = service.registrar(entrada(TipoMarcacao.ENTRADA, UUID.randomUUID(), "123456"));
        assertEquals(clock.instant(), resposta.instante());
        assertEquals("2026-10-10", resposta.dataRegistro().toString());
        verify(funcionarios).findParaAtualizacao(funcionario.getId());
    }

    @Test
    void repeticaoDoMesmoPedidoNaoCriaOutraMarcacao() {
        preparar();
        UUID requisicao = UUID.randomUUID();
        MarcacaoPonto existente = MarcacaoPonto.registrar(funcionario, TipoMarcacao.ENTRADA, requisicao, clock, fuso);
        when(marcacoes.findByFuncionario_IdAndRequisicaoId(funcionario.getId(), requisicao)).thenReturn(Optional.of(existente));
        assertEquals(existente.getId(), service.registrar(entrada(TipoMarcacao.ENTRADA, requisicao, "123456")).id());
        verify(marcacoes, never()).saveAndFlush(any());
        var dados = entrada(TipoMarcacao.SAIDA, requisicao, "123456");
        assertThrows(ErroApi.class, () -> service.registrar(dados));
    }

    @Test
    void rejeitaPinInvalidoMesmoEmRequisicaoRepetida() {
        preparar();
        var dados = entrada(TipoMarcacao.ENTRADA, UUID.randomUUID(), "999999");
        ErroApi erro = assertThrows(ErroApi.class, () -> service.registrar(dados));
        assertEquals(401, erro.getStatus().value());
        verify(funcionarios).findParaAtualizacao(funcionario.getId());
        verify(marcacoes, never()).saveAndFlush(any());
    }

    @Test
    void rejeitaFuncionarioInativo() {
        preparar();
        funcionario.desativar();
        var dados = entrada(TipoMarcacao.ENTRADA, UUID.randomUUID(), "123456");
        assertThrows(ErroApi.class, () -> service.registrar(dados));
        verify(marcacoes, never()).saveAndFlush(any());
    }

    @Test
    void rejeitaEntradaDuplicadaComOutroIdentificador() {
        preparar();
        MarcacaoPonto ultima = MarcacaoPonto.registrar(funcionario, TipoMarcacao.ENTRADA, UUID.randomUUID(), clock, fuso);
        when(marcacoes.findUltimasVigentes(org.mockito.ArgumentMatchers.eq(funcionario.getId()), any())).thenReturn(List.of(ultima));
        var dados = entrada(TipoMarcacao.ENTRADA, UUID.randomUUID(), "123456");
        assertThrows(ErroApi.class, () -> service.registrar(dados));
        verify(marcacoes, never()).saveAndFlush(any());
    }

    @Test
    void almocoExigeRetornoAntesDaSaida() {
        preparar();
        var ultima = MarcacaoPonto.registrar(funcionario, TipoMarcacao.ALMOCO, UUID.randomUUID(), clock, fuso);
        when(marcacoes.findUltimasVigentes(org.mockito.ArgumentMatchers.eq(funcionario.getId()), any())).thenReturn(List.of(ultima));
        var dados = entrada(TipoMarcacao.SAIDA, UUID.randomUUID(), "123456");
        assertThrows(ErroApi.class, () -> service.registrar(dados));
    }

    private void preparar() {
        when(funcionarios.findIdByMatricula("OF-001")).thenReturn(Optional.of(funcionario.getId()));
        when(funcionarios.findParaAtualizacao(funcionario.getId())).thenReturn(Optional.of(funcionario));
    }

    private PontoEntrada entrada(TipoMarcacao tipo, UUID id, String pin) {
        return new PontoEntrada("OF-001", pin, tipo, id);
    }
}
