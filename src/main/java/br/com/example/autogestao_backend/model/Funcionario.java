package br.com.example.autogestao_backend.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "funcionario", uniqueConstraints = @UniqueConstraint(
    name = "uk_funcionario_matricula", columnNames = "matricula"))
public class Funcionario {

    @Id
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(nullable = false, updatable = false, length = 36)
    private UUID id;

    @NotBlank
    @Size(max = 120)
    @Column(nullable = false, length = 120)
    private String nome;

    @NotBlank
    @Size(max = 32)
    @Column(nullable = false, updatable = false, length = 32)
    private String matricula;

    @NotBlank
    @Size(max = 100)
    @Column(name = "pin_hash", nullable = false, length = 100)
    private String pinHash;

    @Column(nullable = false)
    private boolean ativo;

    @Version
    private long versao;

    protected Funcionario() {
    }

    /** O serviço deve fornecer o hash BCrypt do PIN, nunca o PIN em texto puro. */
    public Funcionario(String nome, String matricula, String pinHash) {
        this.id = UUID.randomUUID();
        this.nome = textoObrigatorio(nome, 120, "Nome");
        this.matricula = textoObrigatorio(matricula, 32, "Matrícula");
        this.pinHash = validarHash(pinHash);
        this.ativo = true;
    }

    public void alterarNome(String nome) {
        this.nome = textoObrigatorio(nome, 120, "Nome");
    }

    public void alterarPinHash(String pinHash) {
        this.pinHash = validarHash(pinHash);
    }

    public void ativar() {
        this.ativo = true;
    }

    public void desativar() {
        this.ativo = false;
    }

    private static String textoObrigatorio(String valor, int limite, String campo) {
        if (valor == null || valor.isBlank() || valor.strip().length() > limite) {
            throw new IllegalArgumentException(campo + " obrigatório, com até " + limite + " caracteres.");
        }
        return valor.strip();
    }

    private static String validarHash(String hash) {
        if (hash == null || !hash.matches("\\$2[aby]\\$(0[4-9]|[12]\\d|3[01])\\$[./A-Za-z0-9]{53}")) {
            throw new IllegalArgumentException("Informe um hash BCrypt válido para o PIN.");
        }
        return hash;
    }

    public UUID getId() {
        return id;
    }

    public String getNome() {
        return nome;
    }

    public String getMatricula() {
        return matricula;
    }
    // Não segue o padrão de getter para evitar exposição automática em respostas JSON.
    public String pinHashParaAutenticacao() {
        return pinHash;
    }

    public boolean isAtivo() {
        return ativo;
    }

    public long getVersao() {
        return versao;
    }
}
