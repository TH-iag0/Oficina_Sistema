package br.com.example.autogestao_backend.banco;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "sincronizacao_bancaria")
public class SincronizacaoBancaria {
    @Id
    private Integer id = 1;
    private Instant sucesso;
    @Column(name = "item_id", length = 120)
    private String itemId;
    @Column(name = "execucao_id", length = 36)
    private String execucaoId;
    @Column(name = "ocupado_ate")
    private Instant ocupadoAte;
    @Column(name = "proxima_tentativa")
    private Instant proximaTentativa;
    @Column(nullable = false)
    private int falhas;
    @Column(length = 500)
    private String aviso;

    protected SincronizacaoBancaria() {}
    public Instant getSucesso() { return sucesso; }
    public Instant getProximaTentativa() { return proximaTentativa; }
    public String getAviso() { return aviso; }

    public String iniciar(String item, Instant agora) {
        if (itemId != null && !itemId.equals(item)) {
            aviso = "A conexão bancária foi alterada. Reconcilie a conexão anterior antes de importar outra, para evitar duplicidades.";
            return null;
        }
        if (ocupadoAte != null && agora.isBefore(ocupadoAte)) return null;
        if (proximaTentativa != null && agora.isBefore(proximaTentativa)) return null;
        if (sucesso != null && agora.isBefore(sucesso.plus(Duration.ofHours(24)))) return null;
        execucaoId = UUID.randomUUID().toString();
        ocupadoAte = agora.plus(Duration.ofMinutes(5));
        return execucaoId;
    }

    public void validarExecucao(String execucao, Instant agora) {
        if (execucao == null || !execucao.equals(execucaoId) || ocupadoAte == null || !agora.isBefore(ocupadoAte)) {
            throw new IllegalStateException("A tentativa de importação expirou ou já foi substituída.");
        }
    }

    public void concluir(String execucao, String item, Instant agora, String observacao) {
        validarExecucao(execucao, agora);
        sucesso = agora;
        itemId = item;
        falhas = 0;
        aviso = observacao;
        proximaTentativa = agora.plus(Duration.ofHours(24));
        liberar();
    }

    public void falhar(String execucao, Instant agora, Duration minimo, String mensagem) {
        if (!java.util.Objects.equals(execucaoId, execucao)) return;
        falhas = Math.min(falhas + 1, 10);
        long segundos = Math.max(Math.clamp(60L << (falhas - 1), 0, 3600), minimo.toSeconds());
        proximaTentativa = agora.plusSeconds(segundos);
        aviso = mensagem;
        liberar();
    }

    private void liberar() {
        execucaoId = null;
        ocupadoAte = null;
    }
}
