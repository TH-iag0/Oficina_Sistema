package br.com.example.autogestao_backend.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Clock;
import java.time.Instant;
import java.util.Objects;

@Entity
@Table(name = "cliente", uniqueConstraints = @UniqueConstraint(name = "uk_cliente_documento", columnNames = "documento"),
    indexes = @Index(name = "idx_cliente_cadastro", columnList = "cadastrado_em"))
public class Cliente extends Entidade {
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 2)
    private TipoCliente tipo;
    @Column(nullable = false, length = 160)
    private String nome;
    @Column(length = 14)
    private String documento;
    @Column(length = 30)
    private String telefone;
    @Column(name = "cadastrado_em", nullable = false, updatable = false)
    private Instant cadastradoEm;
    @Column(nullable = false)
    private boolean ativo = true;

    protected Cliente() {}
    public Cliente(TipoCliente tipo, String nome, String documento, String telefone, Clock relogio) {
        this.tipo = Objects.requireNonNull(tipo);
        this.nome = RegrasModel.texto(nome, 160, "Nome");
        this.documento = normalizarDocumento(documento, tipo);
        this.telefone = RegrasModel.opcional(telefone, 30, "Telefone");
        this.cadastradoEm = Objects.requireNonNull(relogio).instant();
    }
    // Validação de dígitos verificadores e busca de duplicatas cabem ao serviço de cadastro.
    public static String normalizarDocumento(String documento, TipoCliente tipo) {
        if (documento == null || documento.isBlank()) {
            return null;
        }
        if (!documento.matches("[0-9. /-]+")) {
            throw new IllegalArgumentException("Documento inválido.");
        }
        String digitos = documento.replaceAll("\\D", "");
        if (digitos.length() != (tipo == TipoCliente.PF ? 11 : 14)) {
            throw new IllegalArgumentException("Documento incompatível com PF/PJ.");
        }
        return digitos;
    }

    public void desativar() {
        ativo = false;
    }

    public void atualizarCadastro(TipoCliente tipo, String nome, String documento, String telefone) {
        String novoNome = RegrasModel.texto(nome, 160, "Nome");
        String novoDocumento = normalizarDocumento(documento, Objects.requireNonNull(tipo));
        String novoTelefone = RegrasModel.opcional(telefone, 30, "Telefone");
        this.tipo = tipo;
        this.nome = novoNome;
        this.documento = novoDocumento;
        this.telefone = novoTelefone;
    }

    public TipoCliente getTipo() {
        return tipo;
    }

    public String getNome() {
        return nome;
    }

    public String getDocumento() {
        return documento;
    }

    public String getTelefone() {
        return telefone;
    }

    public Instant getCadastradoEm() {
        return cadastradoEm;
    }

    public boolean isAtivo() {
        return ativo;
    }
}
