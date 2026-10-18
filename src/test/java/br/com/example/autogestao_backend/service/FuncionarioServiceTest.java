package br.com.example.autogestao_backend.service;

import br.com.example.autogestao_backend.dto.OficinaDtos;
import br.com.example.autogestao_backend.model.Funcionario;
import br.com.example.autogestao_backend.model.JornadaTrabalho;
import br.com.example.autogestao_backend.repository.FuncionarioRepository;
import br.com.example.autogestao_backend.repository.JornadaTrabalhoRepository;
import br.com.example.autogestao_backend.shared.exception.ErroApi;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.Arrays;
import java.util.Collections;
import java.util.UUID;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verifyNoInteractions;

class FuncionarioServiceTest {
    @ParameterizedTest
    @MethodSource("semanasInvalidas")
    void semanaInvalidaNaoBloqueiaFuncionarioNemAlteraHistorico(OficinaDtos.SemanaEntrada semana) {
        UUID funcionarioId = UUID.randomUUID();
        assertThrows(IllegalArgumentException.class, () -> service.configurarSemana(funcionarioId, semana));
        verifyNoInteractions(repo, jornadas);
    }

    private static Stream<OficinaDtos.SemanaEntrada> semanasInvalidas() {
        var inicio = LocalDate.of(2026, 10, 1);
        var entrada = new OficinaDtos.JornadaEntrada(DayOfWeek.MONDAY, LocalTime.of(8, 0),
            LocalTime.of(17, 0), 60, 5, inicio, null);
        var semDia = new OficinaDtos.JornadaEntrada(null, LocalTime.of(8, 0),
            LocalTime.of(17, 0), 60, 5, inicio, null);
        return Stream.of(
            new OficinaDtos.SemanaEntrada(null, true),
            new OficinaDtos.SemanaEntrada(List.of(), true),
            new OficinaDtos.SemanaEntrada(Collections.nCopies(8, entrada), true),
            new OficinaDtos.SemanaEntrada(Arrays.asList(entrada, null), true),
            new OficinaDtos.SemanaEntrada(List.of(entrada, semDia), true)
        );
    }

    @Test void semanaRejeitaDiasRepetidosAntesDePersistir() {
        var entrada = new OficinaDtos.JornadaEntrada(DayOfWeek.MONDAY, LocalTime.of(8,0), LocalTime.of(17,0),
            60, 5, LocalDate.of(2026,10,1), null);
        var requisicaoId = java.util.UUID.randomUUID();
        var semanaEntrada = new OficinaDtos.SemanaEntrada(List.of(entrada, entrada), true);
        assertThrows(IllegalArgumentException.class, () -> service.configurarSemana(requisicaoId,
            semanaEntrada));
        verifyNoInteractions(repo, jornadas);
    }

    @Test void semanaSalvaOsDiasSelecionadosComHorariosIndependentes() {
        var f = new Funcionario("Carlos", "OF-1", pins.codificar("123456"));
        when(repo.findParaAtualizacao(f.getId())).thenReturn(Optional.of(f));
        when(jornadas.saveAndFlush(any())).thenAnswer(call -> call.getArgument(0));
        var inicio = LocalDate.of(2026,10,1);
        var segunda = new OficinaDtos.JornadaEntrada(DayOfWeek.MONDAY, LocalTime.of(8,0), LocalTime.of(17,0), 60, 5, inicio, null);
        var sabado = new OficinaDtos.JornadaEntrada(DayOfWeek.SATURDAY, LocalTime.of(8,0), LocalTime.of(12,0), 0, 0, inicio, null);
        var resultado = service.configurarSemana(f.getId(), new OficinaDtos.SemanaEntrada(List.of(segunda, sabado), false));
        org.junit.jupiter.api.Assertions.assertEquals(2, resultado.size());
        org.junit.jupiter.api.Assertions.assertEquals(LocalTime.of(12,0), resultado.get(1).saida());
        verify(jornadas, times(2)).saveAndFlush(any());
    }
    private final FuncionarioRepository repo = mock(FuncionarioRepository.class);
    private final JornadaTrabalhoRepository jornadas = mock(JornadaTrabalhoRepository.class);
    private final PinAuthenticator pins = new PinAuthenticator();
    private final FuncionarioService service = new FuncionarioService(repo, jornadas, pins);
    @Test void guardaSomenteHashDoPin() {
        when(repo.saveAndFlush(any())).thenAnswer(call -> {
            Funcionario f = call.getArgument(0);
            assertTrue(pins.matches("123456", f.pinHashParaAutenticacao()));
            return f;
        });
        var dto = service.cadastrar(new OficinaDtos.FuncionarioEntrada("Carlos", "OF-1", "123456"));
        assertTrue(dto.ativo());
    }

    @Test void matriculaDuplicadaNaoCadastra() {
        when(repo.existsByMatricula("OF-1")).thenReturn(true);
        var funcionarioEntrada = new OficinaDtos.FuncionarioEntrada("Carlos", " OF-1 ", "123456");
        assertThrows(ErroApi.class, () -> service.cadastrar(funcionarioEntrada));
    }

    @Test void rejeitaJornadaSobreposta() {
        var f = new Funcionario("Carlos", "OF-1", pins.codificar("123456"));
        var inicio = LocalDate.of(2026, 1, 1);
        when(repo.findParaAtualizacao(f.getId())).thenReturn(Optional.of(f));
        when(jornadas.findByFuncionarioIdAndDiaSemana(f.getId(), DayOfWeek.MONDAY)).thenReturn(List.of(
            new JornadaTrabalho(f, DayOfWeek.MONDAY, LocalTime.of(8,0), LocalTime.of(17,0), 60, 5, inicio)));
        var dados = new OficinaDtos.JornadaEntrada(DayOfWeek.MONDAY, LocalTime.of(9,0), LocalTime.of(18,0), 60, 5, inicio.plusMonths(1), null);
        var fId = f.getId();
        assertThrows(ErroApi.class, () -> service.jornada(fId, dados));
    }

    @Test void alteraPinSobBloqueio() {
        var f = new Funcionario("Carlos", "OF-1", pins.codificar("123456"));
        when(repo.findParaAtualizacao(f.getId())).thenReturn(Optional.of(f));
        service.alterarPin(f.getId(), new OficinaDtos.PinEntrada("654321"));
        assertTrue(pins.matches("654321", f.pinHashParaAutenticacao()));
    }
}
