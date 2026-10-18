package br.com.example.autogestao_backend.service;

import br.com.example.autogestao_backend.dto.AnaliseFinanceiraResposta;
import br.com.example.autogestao_backend.repository.DespesaRepository;
import br.com.example.autogestao_backend.repository.EstornoPagamentoRepository;
import br.com.example.autogestao_backend.repository.PagamentoRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.ArrayList;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class AnaliseFinanceiraService {
    private final PagamentoRepository pagamentos;
    private final DespesaRepository despesas;
    private final EstornoPagamentoRepository estornos;
    private final ZoneId fuso;

    public AnaliseFinanceiraService(PagamentoRepository pagamentos, DespesaRepository despesas,
        EstornoPagamentoRepository estornos, ZoneId fuso) {
        this.pagamentos = pagamentos;
        this.despesas = despesas;
        this.estornos = estornos;
        this.fuso = fuso;
    }

    public AnaliseFinanceiraResposta consultar(YearMonth mes, int meses) {
        if (meses < 1 || meses > 24) throw new IllegalArgumentException("Informe entre 1 e 24 meses.");
        var fluxo = new ArrayList<AnaliseFinanceiraResposta.Mes>();
        for (int i = meses - 1; i >= 0; i--) fluxo.add(fluxoMes(mes.minusMonths(i)));
        var categorias = despesas.somarPorCategoria(mes.atDay(1), mes.plusMonths(1).atDay(1)).stream()
            .map(c -> new AnaliseFinanceiraResposta.Categoria(c.getNome(), c.getValor())).toList();
        return new AnaliseFinanceiraResposta(fluxo, categorias);
    }

    private AnaliseFinanceiraResposta.Mes fluxoMes(YearMonth mes) {
        Instant inicio = mes.atDay(1).atStartOfDay(fuso).toInstant();
        Instant fim = mes.plusMonths(1).atDay(1).atStartOfDay(fuso).toInstant();
        BigDecimal entradas = pagamentos.somarRecebimentos(inicio, fim);
        BigDecimal pagas = despesas.somarPagas(inicio, fim);
        BigDecimal devolucoes = estornos.somarEstornos(inicio, fim);
        BigDecimal saidas = pagas.add(devolucoes);
        return new AnaliseFinanceiraResposta.Mes(mes, entradas, pagas, devolucoes, saidas, entradas.subtract(saidas));
    }
}
