package br.com.example.autogestao_backend.service;

import br.com.example.autogestao_backend.model.CategoriaServico;
import br.com.example.autogestao_backend.model.ExecucaoServico;
import br.com.example.autogestao_backend.model.Funcionario;
import br.com.example.autogestao_backend.model.JornadaTrabalho;
import br.com.example.autogestao_backend.repository.ExecucaoServicoRepository;
import br.com.example.autogestao_backend.repository.FuncionarioRepository;
import br.com.example.autogestao_backend.repository.JornadaTrabalhoRepository;
import br.com.example.autogestao_backend.repository.MarcacaoPontoRepository;
import br.com.example.autogestao_backend.shared.exception.ErroApi;
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ResumoPontoServiceTest {
    @Test void resumoAnualBuscaVigenciasUmaVezESelecionaJornadaCorreta() {
        var funcionarios = mock(FuncionarioRepository.class);
        var jornadas = mock(JornadaTrabalhoRepository.class);
        var marcacoes = mock(MarcacaoPontoRepository.class);
        var execucoes = mock(ExecucaoServicoRepository.class);
        var funcionario = new Funcionario("Carlos", "OF1", new PinAuthenticator().codificar("123456"));
        when(funcionarios.findById(funcionario.getId())).thenReturn(Optional.of(funcionario));
        var inicio = LocalDate.of(2026, 1, 1);
        var antiga = new JornadaTrabalho(funcionario, DayOfWeek.MONDAY,
            LocalTime.of(8, 0), LocalTime.of(17, 0), 60, 5, inicio);
        antiga.encerrarVigencia(LocalDate.of(2026, 6, 30));
        var nova = new JornadaTrabalho(funcionario, DayOfWeek.MONDAY,
            LocalTime.of(8, 0), LocalTime.of(16, 0), 60, 5, LocalDate.of(2026, 7, 1));
        when(jornadas.findSobrepostas(funcionario.getId(), inicio, inicio.plusYears(1))).thenReturn(List.of(nova, antiga));
        when(execucoes.findSobrepostas(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(),
            org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any())).thenReturn(Page.empty());
        var service = new ResumoPontoService(funcionarios, jornadas, marcacoes, execucoes, FluxoFixtures.CLOCK.getZone());
        var resumo = service.resumo(funcionario.getId(), inicio, inicio.plusYears(1));
        org.junit.jupiter.api.Assertions.assertEquals(480, resumo.dias().stream()
            .filter(d -> d.data().equals(LocalDate.of(2026, 6, 29))).findFirst().orElseThrow().minutosPrevistos());
        org.junit.jupiter.api.Assertions.assertEquals(420, resumo.dias().stream()
            .filter(d -> d.data().equals(LocalDate.of(2026, 7, 6))).findFirst().orElseThrow().minutosPrevistos());
        verify(jornadas).findSobrepostas(funcionario.getId(), inicio, inicio.plusYears(1));
        verifyNoMoreInteractions(jornadas);
    }

    @Test void contabilizaSomenteExecucoesEncerradasDentroDoPeriodo() {
        var funcionarios = mock(FuncionarioRepository.class);
        var jornadas = mock(JornadaTrabalhoRepository.class);
        var marcacoes = mock(MarcacaoPontoRepository.class);
        var execucoes = mock(ExecucaoServicoRepository.class);
        var funcionario = new Funcionario("Carlos", "OF1", new PinAuthenticator().codificar("123456"));
        when(funcionarios.findById(funcionario.getId())).thenReturn(Optional.of(funcionario));
        when(jornadas.findSobrepostas(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(),
            org.mockito.ArgumentMatchers.any())).thenReturn(List.of());
        var item = FluxoFixtures.emServico(CategoriaServico.MANUTENCAO).getItens().getFirst();
        var inicioExecucao = Clock.fixed(Instant.parse("2026-10-13T23:00:00Z"), FluxoFixtures.CLOCK.getZone());
        var fechada = new ExecucaoServico(item, funcionario, inicioExecucao);
        fechada.encerrar(Clock.fixed(Instant.parse("2026-10-14T04:00:00Z"), FluxoFixtures.CLOCK.getZone()));
        var aberta = new ExecucaoServico(item, funcionario, inicioExecucao);
        when(execucoes.findSobrepostas(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(),
            org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any()))
            .thenReturn(new PageImpl<>(List.of(fechada, aberta)));
        var service = new ResumoPontoService(funcionarios, jornadas, marcacoes, execucoes, FluxoFixtures.CLOCK.getZone());
        var resumo = service.resumo(funcionario.getId(), LocalDate.of(2026, 10, 14), LocalDate.of(2026, 10, 15));
        org.junit.jupiter.api.Assertions.assertEquals(60, resumo.minutosEmServicos());
        org.junit.jupiter.api.Assertions.assertEquals(0, resumo.minutosTrabalhados());
    }

    @Test void limitaPeriodoAntesDeConsultarRegistros() {
        var repo = mock(FuncionarioRepository.class);
        var f = new Funcionario("Carlos", "OF1", new PinAuthenticator().codificar("123456"));
        when(repo.findById(f.getId())).thenReturn(Optional.of(f));
        var service = new ResumoPontoService(repo, mock(JornadaTrabalhoRepository.class), mock(MarcacaoPontoRepository.class),
            mock(ExecucaoServicoRepository.class), FluxoFixtures.CLOCK.getZone());
        var inicio = LocalDate.of(2026,1,1);
        var fId = f.getId();
        var dados = inicio.plusYears(2);
        assertThrows(ErroApi.class, () -> service.resumo(fId, inicio, dados));
    }
}
