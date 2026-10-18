package br.com.example.autogestao_backend.service;

import br.com.example.autogestao_backend.model.Funcionario;
import br.com.example.autogestao_backend.model.JornadaTrabalho;
import br.com.example.autogestao_backend.model.MarcacaoPonto;
import br.com.example.autogestao_backend.model.TipoMarcacao;
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CalculoPontoTest {
    private final ZoneId fuso = ZoneId.of("America/Sao_Paulo");
    private final Funcionario funcionario = new Funcionario("Carlos", "OF1", new BCryptPasswordEncoder(4).encode("123456"));
    private final LocalDate data = LocalDate.of(2026,10,13);
    @Test void entradaESaidaDescontamAlmocoAutomaticoSemExigirMarcacoesIntermediarias() {
        var jornada = new JornadaTrabalho(funcionario, DayOfWeek.TUESDAY, new JornadaTrabalho.Horario(LocalTime.of(8,0), LocalTime.of(17,0), 60, 5, true), data);
        var pontos = List.of(ponto(TipoMarcacao.ENTRADA, "2026-10-13T11:00:00Z"), ponto(TipoMarcacao.SAIDA, "2026-10-13T20:00:00Z"));
        var resultado = CalculoPonto.calcular(pontos, Map.of(data, jornada), data, data.plusDays(1), fuso).getFirst();
        assertEquals(480, resultado.minutosTrabalhados());
        assertEquals(0, resultado.minutosExtras());
        assertFalse(resultado.incompleto());
    }

    @Test void intervaloRegistradoNaoSofreDescontoAutomaticoEmDobro() {
        var jornada = new JornadaTrabalho(funcionario, DayOfWeek.TUESDAY, new JornadaTrabalho.Horario(LocalTime.of(8,0), LocalTime.of(17,0), 60, 5, true), data);
        var pontos = List.of(ponto(TipoMarcacao.ENTRADA, "2026-10-13T11:00:00Z"), ponto(TipoMarcacao.ALMOCO, "2026-10-13T15:00:00Z"),
            ponto(TipoMarcacao.RETORNO, "2026-10-13T15:30:00Z"), ponto(TipoMarcacao.SAIDA, "2026-10-13T20:00:00Z"));
        var resultado = CalculoPonto.calcular(pontos, Map.of(data, jornada), data, data.plusDays(1), fuso).getFirst();
        assertEquals(510, resultado.minutosTrabalhados());
        assertFalse(resultado.incompleto());
    }

    @Test void intervaloAutomaticoNaoProduzHorasNegativasNemCompletaEntradaSemSaida() {
        var jornada = new JornadaTrabalho(funcionario, DayOfWeek.TUESDAY, new JornadaTrabalho.Horario(LocalTime.of(8,0), LocalTime.of(17,0), 60, 5, true), data);
        var entrada = ponto(TipoMarcacao.ENTRADA, "2026-10-13T11:00:00Z");
        var curto = CalculoPonto.calcular(List.of(entrada, ponto(TipoMarcacao.SAIDA, "2026-10-13T11:30:00Z")),
            Map.of(data, jornada), data, data.plusDays(1), fuso).getFirst();
        assertEquals(0, curto.minutosTrabalhados());
        var aberto = CalculoPonto.calcular(List.of(entrada), Map.of(data, jornada), data, data.plusDays(1), fuso).getFirst();
        assertTrue(aberto.incompleto());
        assertEquals(0, aberto.minutosTrabalhados());
    }
    @Test void descontaIntervaloECalculaAtrasoEExtras() {
        var jornada = new JornadaTrabalho(funcionario, DayOfWeek.TUESDAY, LocalTime.of(8,0), LocalTime.of(17,0), 60, 5, data);
        var pontos = List.of(ponto(TipoMarcacao.ENTRADA, "2026-10-13T11:10:00Z"), ponto(TipoMarcacao.ALMOCO, "2026-10-13T15:00:00Z"),
            ponto(TipoMarcacao.RETORNO, "2026-10-13T16:00:00Z"), ponto(TipoMarcacao.SAIDA, "2026-10-13T21:00:00Z"));
        var resultado = CalculoPonto.calcular(pontos, Map.of(data, jornada), data, data.plusDays(1), fuso).getFirst();
        assertEquals(530, resultado.minutosTrabalhados());
        assertEquals(50, resultado.minutosExtras());
        assertEquals(10, resultado.minutosAtraso());
        assertFalse(resultado.incompleto());
    }

    @Test void suportaTurnoQueCruzaMeiaNoite() {
        var pontos = List.of(ponto(TipoMarcacao.ENTRADA, "2026-10-14T01:00:00Z"), ponto(TipoMarcacao.SAIDA, "2026-10-14T09:00:00Z"));
        var resultado = CalculoPonto.calcular(pontos, Map.of(), data, data.plusDays(1), fuso).getFirst();
        assertEquals(480, resultado.minutosTrabalhados());
        assertFalse(resultado.incompleto());
    }

    @Test void naoInventaHorasDeMarcacaoIncompleta() {
        var pontos = List.of(ponto(TipoMarcacao.ENTRADA, "2026-10-13T11:00:00Z"));
        var resultado = CalculoPonto.calcular(pontos, Map.of(), data, data.plusDays(1), fuso).getFirst();
        assertEquals(0, resultado.minutosTrabalhados());
        assertEquals(0, resultado.minutosExtras());
        assertTrue(resultado.incompleto());
    }

    @Test void saidaSemEntradaEhIncompleta() {
        var resultado = CalculoPonto.calcular(List.of(ponto(TipoMarcacao.SAIDA, "2026-10-13T20:00:00Z")),
            Map.of(), data, data.plusDays(1), fuso).getFirst();
        assertTrue(resultado.incompleto());
    }

    private MarcacaoPonto ponto(TipoMarcacao tipo, String instante) {
        return MarcacaoPonto.registrar(funcionario, tipo, UUID.randomUUID(), Clock.fixed(Instant.parse(instante), fuso), fuso);
    }
}
