package br.com.example.autogestao_backend.model;

import java.time.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static br.com.example.autogestao_backend.model.ModelFixtures.*;

class JornadaTrabalhoTest {
    private JornadaTrabalho jornada(LocalTime entrada, LocalTime saida, int intervalo, int tolerancia) {
        return new JornadaTrabalho(funcionario(), DayOfWeek.MONDAY, entrada, saida, intervalo, tolerancia, LocalDate.of(2026, 10, 1));
    }
    @Test void calculaCargaDiurnaENoturnaDescontandoIntervalo() {
        assertEquals(Duration.ofHours(8), jornada(LocalTime.of(8, 0), LocalTime.of(17, 0), 60, 5).getCargaPrevista());
        assertEquals(Duration.ofHours(7), jornada(LocalTime.of(22, 0), LocalTime.of(6, 0), 60, 5).getCargaPrevista());
    }
    @Test void rejeitaIntervaloToleranciaEHorariosInvalidos() {
        assertThrows(IllegalArgumentException.class, () -> jornada(LocalTime.of(8, 0), LocalTime.of(8, 0), 0, 0));
        assertThrows(IllegalArgumentException.class, () -> jornada(LocalTime.of(8, 0), LocalTime.of(9, 0), 60, 0));
        assertThrows(IllegalArgumentException.class, () -> jornada(LocalTime.of(8, 0), LocalTime.of(9, 0), -1, 0));
        assertThrows(IllegalArgumentException.class, () -> jornada(LocalTime.of(8, 0), LocalTime.of(9, 0), 0, -1));
        assertThrows(IllegalArgumentException.class, () -> jornada(LocalTime.of(8, 0, 1), LocalTime.of(9, 0), 0, 0));
    }
    @Test void mantemConfiguracaoHistoricaAoEncerrarVigencia() {
        var jornada = jornada(LocalTime.of(8, 0), LocalTime.of(17, 0), 60, 5);
        assertThrows(IllegalArgumentException.class, () -> jornada.encerrarVigencia(LocalDate.of(2026, 9, 30)));
        assertNull(jornada.getVigenciaFim());
        jornada.encerrarVigencia(LocalDate.of(2026, 10, 31));
        assertEquals(Duration.ofHours(8), jornada.getCargaPrevista());
        assertThrows(IllegalStateException.class, () -> jornada.encerrarVigencia(LocalDate.of(2026, 11, 30)));
    }
}
