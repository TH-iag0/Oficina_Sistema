package br.com.example.autogestao_backend.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Clock;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "ajuste_ponto")
public class AjustePonto extends Entidade {
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "original_id", unique = true)
    private MarcacaoPonto original;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "substituta_id")
    private MarcacaoPonto substituta;
    @Column(nullable = false, length = 500)
    private String motivo;
    @Column(nullable = false, length = 120)
    private String autor;
    @Column(name = "registrado_em", nullable = false)
    private Instant registradoEm;
    @JdbcTypeCode(SqlTypes.CHAR) @Column(name = "requisicao_id", length = 36, nullable = false, unique = true)
    private UUID requisicaoId;
    protected AjustePonto() {}
    public AjustePonto(MarcacaoPonto original, MarcacaoPonto substituta, String motivo, String autor, UUID requisicaoId, Clock relogio) {
        if (original == null && substituta == null) {
            throw new IllegalArgumentException("Ajuste sem marcação.");
        }
        this.original = original;
        this.substituta = substituta;
        this.motivo = RegrasModel.texto(motivo, 500, "Motivo");
        this.autor = RegrasModel.texto(autor, 120, "Autor");
        this.requisicaoId = Objects.requireNonNull(requisicaoId);
        this.registradoEm = relogio.instant();
    }

    public MarcacaoPonto getOriginal() {
        return original;
    }

    public MarcacaoPonto getSubstituta() {
        return substituta;
    }

    public String getMotivo() {
        return motivo;
    }

    public String getAutor() {
        return autor;
    }

    public UUID getRequisicaoId() {
        return requisicaoId;
    }

    public Instant getRegistradoEm() {
        return registradoEm;
    }
}
