package br.com.example.autogestao_backend.model;

import java.time.*;
import java.util.Objects;
import jakarta.persistence.*;

/** Configuração por dia da semana e vigência; não declara conformidade trabalhista. */
@Entity
@Table(name = "jornada_trabalho", uniqueConstraints = @UniqueConstraint(name = "uk_jornada_dia_vigencia",
    columnNames = {"funcionario_id", "dia_semana", "vigencia_inicio"}))
public class JornadaTrabalho extends Entidade {
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "funcionario_id", nullable = false, updatable = false)
    private Funcionario funcionario;
    @Enumerated(EnumType.STRING) @Column(name = "dia_semana", nullable = false, updatable = false, length = 16) private DayOfWeek diaSemana;
    @Column(nullable = false, updatable = false) private LocalTime entrada;
    @Column(nullable = false, updatable = false) private LocalTime saida;
    @Column(name = "intervalo_minutos", nullable = false, updatable = false) private int intervaloMinutos;
    @Column(name = "tolerancia_minutos", nullable = false, updatable = false) private int toleranciaMinutos;
    @Column(name = "vigencia_inicio", nullable = false, updatable = false) private LocalDate vigenciaInicio;
    @Column(name = "vigencia_fim") private LocalDate vigenciaFim;
    protected JornadaTrabalho() {}
    public JornadaTrabalho(Funcionario funcionario, DayOfWeek dia, LocalTime entrada, LocalTime saida,
                           int intervaloMinutos, int toleranciaMinutos, LocalDate vigenciaInicio) {
        this.funcionario = Objects.requireNonNull(funcionario); this.diaSemana = Objects.requireNonNull(dia);
        this.entrada = Objects.requireNonNull(entrada); this.saida = Objects.requireNonNull(saida);
        this.vigenciaInicio = Objects.requireNonNull(vigenciaInicio);
        if (entrada.equals(saida) || entrada.getSecond() != 0 || saida.getSecond() != 0 || entrada.getNano() != 0 || saida.getNano() != 0)
            throw new IllegalArgumentException("Horários devem ter precisão de minutos e definir um período não nulo.");
        if (intervaloMinutos < 0 || intervaloMinutos >= minutosPresenca() || toleranciaMinutos < 0 || toleranciaMinutos > minutosPresenca())
            throw new IllegalArgumentException("Intervalo ou tolerância inválidos.");
        this.intervaloMinutos = intervaloMinutos; this.toleranciaMinutos = toleranciaMinutos;
    }
    private long minutosPresenca() {
        long minutos = Duration.between(entrada, saida).toMinutes();
        return minutos < 0 ? minutos + 1440 : minutos;
    }
    public Duration getCargaPrevista() { return Duration.ofMinutes(minutosPresenca() - intervaloMinutos); }
    public void encerrarVigencia(LocalDate fim) {
        if (vigenciaFim != null) throw new IllegalStateException("Vigência já encerrada.");
        if (Objects.requireNonNull(fim).isBefore(vigenciaInicio)) throw new IllegalArgumentException("Fim anterior ao início da vigência.");
        vigenciaFim = fim;
    }
    public Funcionario getFuncionario() { return funcionario; }
    public DayOfWeek getDiaSemana() { return diaSemana; }
    public LocalTime getEntrada() { return entrada; }
    public LocalTime getSaida() { return saida; }
    public int getIntervaloMinutos() { return intervaloMinutos; }
    public int getToleranciaMinutos() { return toleranciaMinutos; }
    public LocalDate getVigenciaInicio() { return vigenciaInicio; }
    public LocalDate getVigenciaFim() { return vigenciaFim; }
}
