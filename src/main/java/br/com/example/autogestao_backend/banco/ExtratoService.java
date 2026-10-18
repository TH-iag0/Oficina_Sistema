package br.com.example.autogestao_backend.banco;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ExtratoService {
    public record MovimentoResposta(String identificador, String descricao, Instant instante, BigDecimal valor) {}
    public record Resposta(boolean configurado, Instant ultimaConsulta, Instant proximaTentativa, String aviso,
        BigDecimal entradas, BigDecimal saidas, BigDecimal saldoDoPeriodo, long total,
        List<MovimentoResposta> movimentos) {}
    private final PluggyConfig config;
    private final PluggyClient client;
    private final ExtratoStore store;
    private final MovimentoRepository movimentos;
    private final Clock clock;
    private final ZoneId fuso;


    public ExtratoService(PluggyConfig config, PluggyClient client, ExtratoStore store,
        MovimentoRepository movimentos, Clock clock, ZoneId fuso) {
        this.config = config;
        this.client = client;
        this.store = store;
        this.movimentos = movimentos;
        this.clock = clock;
        this.fuso = fuso;
    }

    public synchronized void sincronizar() {
        if (!config.configurado()) return;
        var tentativa = store.iniciar(config.itemId, clock.instant());
        if (tentativa.isEmpty()) return;
        importar(tentativa.get());
    }

    private void importar(ExtratoStore.Tentativa tentativa) {
        try {
            LocalDate desde = inicioBusca(tentativa.ultima(), LocalDate.now(clock));
            var dados = client.ler(desde);
            String aviso = tentativa.ultima() != null
                && tentativa.ultima().isBefore(clock.instant().minus(Duration.ofDays(365)))
                ? "O computador ficou mais de um ano sem sincronizar. Confira o histórico anterior ao período disponível no banco." : null;
            store.salvar(dados, desde, clock.instant(), tentativa, aviso);
        } catch (RuntimeException erro) {
            Duration espera = erro instanceof FalhaPluggy falha ? falha.getEspera() : Duration.ofMinutes(1);
            String aviso = erro instanceof FalhaPluggy ? erro.getMessage()
                : "A consulta bancária falhou. Os dados anteriores foram preservados. Confira a conexão e o consentimento.";
            store.falhar(tentativa, clock.instant(), espera, aviso);
        }
    }

    static LocalDate inicioBusca(Instant ultima, LocalDate hoje) {
        if (ultima == null) return hoje.minusDays(365);
        LocalDate anterior = ultima.atZone(java.time.ZoneOffset.UTC).toLocalDate().minusDays(7);
        LocalDate recente = hoje.minusDays(30);
        LocalDate inicio = anterior.isBefore(recente) ? anterior : recente;
        return inicio.isBefore(hoje.minusDays(365)) ? hoje.minusDays(365) : inicio;
    }

    @Transactional(readOnly = true)
    public Resposta consultar(YearMonth mes) {
        Instant inicio = mes.atDay(1).atStartOfDay(fuso).toInstant();
        Instant fim = mes.plusMonths(1).atDay(1).atStartOfDay(fuso).toInstant();
        var dados = movimentos.findFirst100ByConfirmadoTrueAndInstanteGreaterThanEqualAndInstanteLessThanOrderByInstanteDesc(inicio, fim);
        var totais = movimentos.totais(inicio, fim);
        BigDecimal entradas = totais.getEntradas();
        BigDecimal saidas = totais.getSaidas();
        var linhas = dados.stream().limit(100).map(m -> new MovimentoResposta(
            m.getOrigemId(), m.getDescricao(), m.getInstante(), m.getValor())).toList();
        var estado = store.estado();
        return new Resposta(config.configurado(), estado.ultima(), estado.proximaTentativa(), estado.aviso(), entradas, saidas,
            entradas.subtract(saidas), totais.getTotal(), linhas);
    }
}
