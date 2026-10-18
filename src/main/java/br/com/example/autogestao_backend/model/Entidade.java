package br.com.example.autogestao_backend.model;

import jakarta.persistence.Column;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.Version;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@MappedSuperclass
public abstract class Entidade {
    @Id
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(nullable = false, updatable = false, length = 36)
    private UUID id = UUID.randomUUID();

    @Version
    private long versao;

    public UUID getId() {
        return id;
    }

    public long getVersao() {
        return versao;
    }
}
