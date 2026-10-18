package br.com.example.autogestao_backend.service;

import br.com.example.autogestao_backend.dto.DashboardResposta;
import br.com.example.autogestao_backend.model.StatusOrdemServico;
import br.com.example.autogestao_backend.repository.ItemOrdemServicoRepository;
import br.com.example.autogestao_backend.repository.OrdemServicoRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.temporal.TemporalAdjusters;
import java.util.EnumMap;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class DashboardService {
    private final OrdemServicoRepository ordens;
    private final ItemOrdemServicoRepository itens;
    private final Clock relogio;
    private final ZoneId fuso;

    public DashboardService(OrdemServicoRepository ordens, ItemOrdemServicoRepository itens, Clock relogio, ZoneId fuso) {
        this.ordens = ordens;
        this.itens = itens;
        this.relogio = relogio;
        this.fuso = fuso;
    }

    public DashboardResposta resumo(YearMonth mes) {
        Instant inicio = instante(mes.atDay(1));
        Instant fim = instante(mes.plusMonths(1).atDay(1));
        BigDecimal receita = ordens.somarFaturamento(inicio, fim);
        BigDecimal custo = ordens.somarCustosDiretos(inicio, fim);
        BigDecimal anterior = ordens.somarFaturamento(instante(mes.minusMonths(1).atDay(1)), inicio);
        long quantidade = ordens.contarFinalizadas(inicio, fim);
        LocalDate semana = LocalDate.now(relogio.withZone(fuso)).with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        var servicos = itens.findMaisExecutados(inicio, fim, PageRequest.of(0, 10)).getContent().stream()
            .map(item -> new DashboardResposta.ServicoResumo(item.getServicoId(), item.getNome(), item.getCategoria(),
                item.getQuantidade(), item.getReceita(), item.getCustoDireto())).toList();
        return new DashboardResposta(mes.atDay(1), mes.plusMonths(1).atDay(1), receita,
            percentual(receita.subtract(anterior), anterior), custo, receita.subtract(custo),
            percentual(receita.subtract(custo), receita), quantidade,
            quantidade == 0 ? BigDecimal.ZERO.setScale(2) : receita.divide(BigDecimal.valueOf(quantidade), 2, RoundingMode.HALF_UP),
            ordens.contarClientesAtendidos(inicio, fim),
            ordens.contarClientesAtendidos(instante(semana), instante(semana.plusWeeks(1))), operacao(), servicos);
    }

    private EnumMap<StatusOrdemServico, Long> operacao() {
        var resultado = new EnumMap<StatusOrdemServico, Long>(StatusOrdemServico.class);
        for (StatusOrdemServico status : StatusOrdemServico.values()) {
            resultado.put(status, ordens.countByStatus(status));
        }
        return resultado;
    }

    private Instant instante(LocalDate data) {
        return data.atStartOfDay(fuso).toInstant();
    }

    private BigDecimal percentual(BigDecimal valor, BigDecimal base) {
        return base.signum() == 0 ? null : valor.multiply(BigDecimal.valueOf(100)).divide(base, 2, RoundingMode.HALF_UP);
    }
}
