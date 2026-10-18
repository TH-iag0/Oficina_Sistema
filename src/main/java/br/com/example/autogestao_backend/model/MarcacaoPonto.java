package br.com.example.autogestao_backend.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Objects;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/** Registro imutável de uma marcação confirmada pelo servidor. */
@Entity
@Table(name = "marcacao_ponto",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_ponto_funcionario_requisicao",
        columnNames = {"funcionario_id", "requisicao_id"}),
    indexes = {
        @Index(name = "idx_ponto_funcionario_instante", columnList = "funcionario_id, instante"),
        @Index(name = "idx_ponto_data_registro", columnList = "data_registro")
    })
public class MarcacaoPonto {

    @Id
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(nullable = false, updatable = false, length = 36)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "funcionario_id", nullable = false, updatable = false,
        foreignKey = @ForeignKey(name = "fk_ponto_funcionario"))
    private Funcionario funcionario;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, updatable = false, length = 16)
    private TipoMarcacao tipo;

    @Column(nullable = false, updatable = false)
    private Instant instante;

    @Column(name = "data_registro", nullable = false, updatable = false)
    private LocalDate dataRegistro;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "requisicao_id", nullable = false, updatable = false, length = 36)
    private UUID requisicaoId;

    /** Construtor necessário para o JPA. */
    protected MarcacaoPonto() {
    }

    private MarcacaoPonto(Funcionario funcionario, TipoMarcacao tipo, UUID requisicaoId,
                          Instant instante, ZoneId fuso) {
        this.id = UUID.randomUUID();
        this.funcionario = Objects.requireNonNull(funcionario, "Funcionário obrigatório.");
        if (!funcionario.isAtivo()) {
            throw new IllegalStateException("Funcionário inativo não pode registrar ponto.");
        }
        this.tipo = Objects.requireNonNull(tipo, "Tipo de marcação obrigatório.");
        this.requisicaoId = Objects.requireNonNull(requisicaoId, "Identificador de confirmação obrigatório.");
        this.instante = Objects.requireNonNull(instante, "Horário obrigatório.");
        this.dataRegistro = instante.atZone(Objects.requireNonNull(fuso, "Fuso obrigatório.")).toLocalDate();
    }

    /** O serviço deve fornecer o relógio e o fuso configurados no servidor. */
    public static MarcacaoPonto registrar(Funcionario funcionario, TipoMarcacao tipo,
                                          UUID requisicaoId, Clock relogio, ZoneId fuso) {
        return new MarcacaoPonto(funcionario, tipo, requisicaoId,
            Objects.requireNonNull(relogio, "Relógio obrigatório.").instant(), fuso);
    }

    public UUID getId() {
        return id;
    }

    public Funcionario getFuncionario() {
        return funcionario;
    }

    public UUID getFuncionarioId() {
        return funcionario.getId();
    }

    public TipoMarcacao getTipo() {
        return tipo;
    }

    public Instant getInstante() {
        return instante;
    }

    public LocalDate getDataRegistro() {
        return dataRegistro;
    }

    public UUID getRequisicaoId() {
        return requisicaoId;
    }
}
