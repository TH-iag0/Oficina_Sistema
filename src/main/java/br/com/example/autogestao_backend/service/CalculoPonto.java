package br.com.example.autogestao_backend.service;

import br.com.example.autogestao_backend.dto.OficinaDtos;
import br.com.example.autogestao_backend.model.JornadaTrabalho;
import br.com.example.autogestao_backend.model.MarcacaoPonto;
import br.com.example.autogestao_backend.model.TipoMarcacao;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class CalculoPonto {
    private CalculoPonto() {}
    public static List<OficinaDtos.DiaPonto> calcular(List<MarcacaoPonto> registros,
        Map<LocalDate, JornadaTrabalho> jornadas, LocalDate inicio, LocalDate fim, ZoneId fuso) {
        Map<LocalDate, Dia> dias = new HashMap<>();
        Turno turno = new Turno();
        for (var registro : registros) processar(registro, dias, turno, fuso);
        if (turno.dia != null) dias.get(turno.dia).incompleto = true;
        List<OficinaDtos.DiaPonto> resultado = new ArrayList<>();
        for (LocalDate data = inicio; data.isBefore(fim); data = data.plusDays(1)) {
            resultado.add(resumo(data, dias.getOrDefault(data, new Dia()), jornadas.get(data), fuso));
        }
        return resultado;
    }

    private static void processar(MarcacaoPonto marcacao, Map<LocalDate, Dia> dias, Turno turno, ZoneId fuso) {
        LocalDate data = marcacao.getInstante().atZone(fuso).toLocalDate();
        if (marcacao.getTipo() == TipoMarcacao.ENTRADA) {
            iniciarTurno(marcacao, data, dias, turno);
            return;
        }
        if (turno.dia == null) {
            dias.computeIfAbsent(data, key -> new Dia()).incompleto = true;
            return;
        }
        Dia dia = dias.get(turno.dia);
        if (marcacao.getTipo() == TipoMarcacao.RETORNO && turno.anterior == TipoMarcacao.ALMOCO) {
            turno.segmento = marcacao.getInstante();
        } else if (podeFecharSegmento(marcacao.getTipo(), turno)) {
            fecharSegmento(marcacao, dia, turno);
        } else {
            dia.incompleto = true;
        }
        turno.anterior = marcacao.getTipo();
    }

    private static void iniciarTurno(MarcacaoPonto marcacao, LocalDate data, Map<LocalDate, Dia> dias, Turno turno) {
        if (turno.dia != null) {
            dias.get(turno.dia).incompleto = true;
        }
        turno.dia = data;
        turno.segmento = marcacao.getInstante();
        turno.anterior = marcacao.getTipo();
        Dia dia = dias.computeIfAbsent(data, key -> new Dia());
        if (dia.primeiraEntrada == null) {
            dia.primeiraEntrada = marcacao.getInstante();
        }
    }

    private static boolean podeFecharSegmento(TipoMarcacao tipo, Turno turno) {
        return (tipo == TipoMarcacao.ALMOCO && turno.anterior == TipoMarcacao.ENTRADA)
            || (tipo == TipoMarcacao.SAIDA && turno.segmento != null);
    }

    private static void fecharSegmento(MarcacaoPonto marcacao, Dia dia, Turno turno) {
        if (marcacao.getTipo() == TipoMarcacao.ALMOCO) dia.intervaloRegistrado = true;
        dia.segundos += Duration.between(turno.segmento, marcacao.getInstante()).getSeconds();
        turno.segmento = null;
        if (marcacao.getTipo() == TipoMarcacao.SAIDA) {
            dia.saidaRegistrada = true;
            turno.dia = null;
        }
    }

    private static OficinaDtos.DiaPonto resumo(LocalDate data, Dia dia, JornadaTrabalho jornada, ZoneId fuso) {
        long previsto = jornada == null ? 0 : jornada.getCargaPrevista().toMinutes();
        long atraso = 0;
        if (jornada != null && dia.primeiraEntrada != null) {
            long diferenca = Duration.between(data.atTime(jornada.getEntrada()).atZone(fuso).toInstant(), dia.primeiraEntrada).toMinutes();
            atraso = diferenca > jornada.getToleranciaMinutos() ? Math.max(0, diferenca) : 0;
        }
        boolean incompleto = dia.incompleto || (previsto > 0 && dia.primeiraEntrada == null);
        long minutos = dia.segundos / 60;
        if (jornada != null && jornada.isIntervaloAutomatico() && dia.saidaRegistrada && !dia.intervaloRegistrado) {
            minutos = Math.max(0, minutos - jornada.getIntervaloMinutos());
        }
        long extras = incompleto ? 0 : Math.max(0, minutos - previsto);
        return new OficinaDtos.DiaPonto(data, minutos, previsto, atraso, extras, incompleto);
    }
    private static final class Dia {
        private long segundos;
        private Instant primeiraEntrada;
        private boolean incompleto;
        private boolean intervaloRegistrado;
        private boolean saidaRegistrada;
    }
    private static final class Turno {
        private LocalDate dia;
        private Instant segmento;
        private TipoMarcacao anterior;
    }
}
