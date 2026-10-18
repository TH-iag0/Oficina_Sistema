package br.com.example.autogestao_backend.banco;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class ExtratoStore {
    public record Estado(Instant ultima, Instant proximaTentativa, String aviso) {}
    public record Tentativa(String execucao, String item, Instant ultima) {}
    private final MovimentoRepository movimentos;
    private final SincronizacaoRepository sincronizacoes;

    public ExtratoStore(MovimentoRepository movimentos, SincronizacaoRepository sincronizacoes) {
        this.movimentos = movimentos;
        this.sincronizacoes = sincronizacoes;
    }

    public Estado estado() {
        var estado = sincronizacoes.findById(1).orElseThrow();
        return new Estado(estado.getSucesso(), estado.getProximaTentativa(), estado.getAviso());
    }

    @Transactional
    public Optional<Tentativa> iniciar(String item, Instant agora) {
        var estado = sincronizacoes.bloquear().orElseThrow();
        String execucao = estado.iniciar(item, agora);
        return execucao == null ? Optional.empty() : Optional.of(new Tentativa(execucao, item, estado.getSucesso()));
    }

    @Transactional
    public void falhar(Tentativa tentativa, Instant agora, Duration espera, String mensagem) {
        sincronizacoes.bloquear().orElseThrow().falhar(tentativa.execucao(), agora, espera, mensagem);
    }

    @Transactional
    public void salvar(List<PluggyClient.Conta> contas, LocalDate desde, Instant agora, Tentativa tentativa, String aviso) {
        var estado = sincronizacoes.bloquear().orElseThrow();
        estado.validarExecucao(tentativa.execucao(), agora);
        if (contas.isEmpty()) throw new IllegalStateException("Nenhuma conta bancária recebida.");
        for (var conta : contas) salvarConta(conta, desde);
        estado.concluir(tentativa.execucao(), tentativa.item(), agora, aviso);
    }

    private void salvarConta(PluggyClient.Conta conta, LocalDate desde) {
        Set<String> presentes = new HashSet<>();
        for (var dado : conta.movimentos()) {
            if (!presentes.add(dado.origem())) throw new IllegalStateException("Identificador repetido no extrato recebido.");
            var movimento = movimentos.findByContaIdAndOrigemId(conta.id(), dado.origem())
                .orElseGet(() -> new MovimentoBancario(conta.id(), dado.origem()));
            movimento.atualizar(dado.descricao(), dado.instante(), dado.valor(), dado.confirmado());
            movimentos.save(movimento);
        }
        for (var anterior : movimentos.findByContaIdAndInstanteGreaterThanEqual(
            conta.id(), desde.atStartOfDay().toInstant(ZoneOffset.UTC))) {
            if (!presentes.contains(anterior.getOrigemId())) anterior.retirar();
        }
    }
}
