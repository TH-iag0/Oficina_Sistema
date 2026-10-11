package br.com.example.autogestao_backend.model;

import java.util.UUID;
import jakarta.persistence.*;
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

    public UUID getId() { return id; }
    public long getVersao() { return versao; }
}
