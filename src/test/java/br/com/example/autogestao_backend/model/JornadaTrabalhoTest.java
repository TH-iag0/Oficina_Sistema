package br.com.example.autogestao_backend.model;

import java.time.*;
import org.junit.jupiter.api.Test;
import static br.com.example.autogestao_backend.model.ModelFixtures.*;
import static org.junit.jupiter.api.Assertions.*;

class JornadaTrabalhoTest {
    private JornadaTrabalho jornada(LocalTime entrada, LocalTime saida, int intervalo, int tolerancia) {
        return new JornadaTrabalho(funcionario(), DayOfWeek.MONDAY, entrada, saida, intervalo, tolerancia, LocalDate.of(2026, 10, 1));
    }

    @Test void calculaCargaDiurnaENoturnaDescontandoIntervalo() {
        assertEquals(Duration.ofHours(8), jornada(LocalTime.of(8, 0), LocalTime.of(17, 0), 60, 5).getCargaPrevista());
        assertEquals(Duration.ofHours(7), jornada(LocalTime.of(22, 0), LocalTime.of(6, 0), 60, 5).getCargaPrevista());
    }

    @Test void rejeitaIntervaloToleranciaEHorariosInvalidos() {
        var horario = LocalTime.of(8, 0);
        var horario2 = LocalTime.of(8, 0);
        assertThrows(IllegalArgumentException.class, () -> jornada(horario, horario2, 0, 0));
        var horario3 = LocalTime.of(8, 0);
        var horario4 = LocalTime.of(9, 0);
        assertThrows(IllegalArgumentException.class, () -> jornada(horario3, horario4, 60, 0));
        var horario5 = LocalTime.of(8, 0);
        var horario6 = LocalTime.of(9, 0);
        assertThrows(IllegalArgumentException.class, () -> jornada(horario5, horario6, -1, 0));
        var horario7 = LocalTime.of(8, 0);
        var horario8 = LocalTime.of(9, 0);
        assertThrows(IllegalArgumentException.class, () -> jornada(horario7, horario8, 0, -1));
        var horario9 = LocalTime.of(8, 0, 1);
        var horario10 = LocalTime.of(9, 0);
        assertThrows(IllegalArgumentException.class, () -> jornada(horario9, horario10, 0, 0));
    }

    @Test void mantemConfiguracaoHistoricaAoEncerrarVigencia() {
        var jornada = jornada(LocalTime.of(8, 0), LocalTime.of(17, 0), 60, 5);
        var data = LocalDate.of(2026, 9, 30);
        assertThrows(IllegalArgumentException.class, () -> jornada.encerrarVigencia(data));
        assertNull(jornada.getVigenciaFim());
        jornada.encerrarVigencia(LocalDate.of(2026, 10, 31));
        assertEquals(Duration.ofHours(8), jornada.getCargaPrevista());
        var data2 = LocalDate.of(2026, 11, 30);
        assertThrows(IllegalStateException.class, () -> jornada.encerrarVigencia(data2));
    }
}
