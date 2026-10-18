package br.com.example.autogestao_backend.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Objects;

/** Configuração por dia da semana e vigência; não declara conformidade trabalhista. */
@Entity
@Table(name = "jornada_trabalho", uniqueConstraints = @UniqueConstraint(name = "uk_jornada_dia_vigencia",
    columnNames = {"funcionario_id", "dia_semana", "vigencia_inicio"}))
public class JornadaTrabalho extends Entidade {
    public record Horario(LocalTime entrada, LocalTime saida, int intervaloMinutos,
                          int toleranciaMinutos, boolean intervaloAutomatico) {}
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "funcionario_id", nullable = false, updatable = false)
    private Funcionario funcionario;
    @Enumerated(EnumType.STRING) @Column(name = "dia_semana", nullable = false, updatable = false, length = 16)
    private DayOfWeek diaSemana;
    @Column(nullable = false, updatable = false)
    private LocalTime entrada;
    @Column(nullable = false, updatable = false)
    private LocalTime saida;
    @Column(name = "intervalo_minutos", nullable = false, updatable = false)
    private int intervaloMinutos;
    @Column(name = "intervalo_automatico", nullable = false, updatable = false)
    private boolean intervaloAutomatico;
    @Column(name = "tolerancia_minutos", nullable = false, updatable = false)
    private int toleranciaMinutos;
    @Column(name = "vigencia_inicio", nullable = false, updatable = false)
    private LocalDate vigenciaInicio;
    @Column(name = "vigencia_fim")
    private LocalDate vigenciaFim;
    protected JornadaTrabalho() {}
    public JornadaTrabalho(Funcionario funcionario, DayOfWeek dia, LocalTime entrada, LocalTime saida,
                           int intervaloMinutos, int toleranciaMinutos, LocalDate vigenciaInicio) {
        this(funcionario, dia, new Horario(entrada, saida, intervaloMinutos, toleranciaMinutos, false), vigenciaInicio);
    }

    public JornadaTrabalho(Funcionario funcionario, DayOfWeek dia, Horario horario, LocalDate vigenciaInicio) {
        Objects.requireNonNull(horario);
        this.funcionario = Objects.requireNonNull(funcionario);
        this.diaSemana = Objects.requireNonNull(dia);
        this.entrada = Objects.requireNonNull(horario.entrada());
        this.saida = Objects.requireNonNull(horario.saida());
        this.vigenciaInicio = Objects.requireNonNull(vigenciaInicio);
        if (entrada.equals(saida) || entrada.getSecond() != 0 || saida.getSecond() != 0 || entrada.getNano() != 0 || saida.getNano() != 0)
            throw new IllegalArgumentException("Horários devem ter precisão de minutos e definir um período não nulo.");
        if (horario.intervaloMinutos() < 0 || horario.intervaloMinutos() >= minutosPresenca()
            || horario.toleranciaMinutos() < 0 || horario.toleranciaMinutos() > minutosPresenca())
            throw new IllegalArgumentException("Intervalo ou tolerância inválidos.");
        this.intervaloMinutos = horario.intervaloMinutos();
        this.toleranciaMinutos = horario.toleranciaMinutos();
        this.intervaloAutomatico = horario.intervaloAutomatico();
    }

    private long minutosPresenca() {
        long minutos = Duration.between(entrada, saida).toMinutes();
        return minutos < 0 ? minutos + 1440 : minutos;
    }

    public Duration getCargaPrevista() {
        return Duration.ofMinutes(minutosPresenca() - intervaloMinutos);
    }

    public boolean isIntervaloAutomatico() { return intervaloAutomatico; }

    public void encerrarVigencia(LocalDate fim) {
        if (vigenciaFim != null) {
            throw new IllegalStateException("Vigência já encerrada.");
        }
        if (Objects.requireNonNull(fim).isBefore(vigenciaInicio)) {
            throw new IllegalArgumentException("Fim anterior ao início da vigência.");
        }
        vigenciaFim = fim;
    }

    public Funcionario getFuncionario() {
        return funcionario;
    }

    public DayOfWeek getDiaSemana() {
        return diaSemana;
    }

    public LocalTime getEntrada() {
        return entrada;
    }

    public LocalTime getSaida() {
        return saida;
    }

    public int getIntervaloMinutos() {
        return intervaloMinutos;
    }

    public int getToleranciaMinutos() {
        return toleranciaMinutos;
    }

    public LocalDate getVigenciaInicio() {
        return vigenciaInicio;
    }

    public LocalDate getVigenciaFim() {
        return vigenciaFim;
    }
}
