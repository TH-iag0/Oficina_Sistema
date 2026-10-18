package br.com.example.autogestao_backend.service;

import br.com.example.autogestao_backend.dto.DashboardResposta;
import br.com.example.autogestao_backend.dto.IndicadoresResposta;
import br.com.example.autogestao_backend.dto.OficinaDtos;
import br.com.example.autogestao_backend.model.CategoriaServico;
import br.com.example.autogestao_backend.model.StatusOrdemServico;
import br.com.example.autogestao_backend.repository.ClienteRepository;
import br.com.example.autogestao_backend.repository.DespesaRepository;
import br.com.example.autogestao_backend.repository.DiagnosticoRepository;
import br.com.example.autogestao_backend.repository.EstornoPagamentoRepository;
import br.com.example.autogestao_backend.repository.GrupoQuantidade;
import br.com.example.autogestao_backend.repository.ItemOrdemServicoRepository;
import br.com.example.autogestao_backend.repository.OrdemServicoRepository;
import br.com.example.autogestao_backend.repository.PagamentoRepository;
import br.com.example.autogestao_backend.repository.RemapRepository;
import br.com.example.autogestao_backend.repository.VeiculoRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class IndicadoresService {
    private final OrdemServicoRepository ordens;
    private final ClienteRepository clientes;
    private final VeiculoRepository veiculos;
    private final DespesaRepository despesas;
    private final PagamentoRepository pagamentos;
    private final EstornoPagamentoRepository estornos;
    private final DiagnosticoRepository diagnosticos;
    private final RemapRepository remaps;
    private final ItemOrdemServicoRepository itens;
    private final ZoneId fuso;
    private final Clock relogio;
    public IndicadoresService(OrdemServicoRepository ordens, ClienteRepository clientes, VeiculoRepository veiculos,
        DespesaRepository despesas, PagamentoRepository pagamentos, EstornoPagamentoRepository estornos,
        DiagnosticoRepository diagnosticos, RemapRepository remaps, ItemOrdemServicoRepository itens, ZoneId fuso, Clock relogio) {
        this.ordens = ordens;
        this.clientes = clientes;
        this.veiculos = veiculos;
        this.despesas = despesas;
        this.pagamentos = pagamentos;
        this.estornos = estornos;
        this.diagnosticos = diagnosticos;
        this.remaps = remaps;
        this.itens = itens;
        this.fuso = fuso;
        this.relogio = relogio;
    }

    public IndicadoresResposta indicadores(YearMonth fim, int quantidadeMeses) {
        if (quantidadeMeses < 1 || quantidadeMeses > 24) {
            throw new IllegalArgumentException("Informe entre 1 e 24 meses.");
        }
        List<IndicadoresResposta.Mes> serie = new ArrayList<>();
        for (int i = quantidadeMeses - 1; i >= 0; i--) serie.add(mes(fim.minusMonths(i)));
        Instant inicio = instante(fim.atDay(1));
        Instant ate = instante(fim.plusMonths(1).atDay(1));
        var rentaveis = itens.findRentabilidade(inicio, ate, PageRequest.of(0, 4)).stream()
            .map(i -> new DashboardResposta.ServicoResumo(i.getServicoId(), i.getNome(), i.getCategoria(), i.getQuantidade(), i.getReceita(), i.getCustoDireto())).toList();
        var revisoes = veiculos.findRevisoesAte(LocalDate.now(relogio), PageRequest.of(0, 20)).stream()
            .map(v -> new OficinaDtos.VeiculoResposta(v.getId(), v.getCliente().getId(), v.getPlaca(), v.getMarca(),
                v.getModelo(), v.getQuilometragem(), v.getProximaRevisaoData(), v.getProximaRevisaoKm(), v.isAtivo())).toList();
        return new IndicadoresResposta(serie, ordens.countByStatus(StatusOrdemServico.DIAGNOSTICO), tempoMedio(inicio, ate),
            itens.contarRetrabalhos(inicio, ate), diagnosticos.contarNoPeriodo(inicio, ate),
            diagnosticos.contarConvertidos(inicio, ate), grupos(ordens.marcas(inicio, ate, PageRequest.of(0, 10))),
            grupos(ordens.modelos(inicio, ate, PageRequest.of(0, 10))), grupos(diagnosticos.causas(inicio, ate, PageRequest.of(0, 10))),
            rentaveis, revisoes, clientes.countByCadastradoEmGreaterThanEqualAndCadastradoEmLessThan(inicio, ate),
            media(ordens.somarFaturamento(inicio, ate), ordens.contarClientesFinalizados(inicio, ate)), intervaloMedio(inicio, ate), semanas(fim));
    }

    private List<IndicadoresResposta.Semana> semanas(YearMonth mes) {
        var resultado = new ArrayList<IndicadoresResposta.Semana>();
        LocalDate limite = mes.plusMonths(1).atDay(1);
        LocalDate inicio = mes.atDay(1);
        while (inicio.isBefore(limite)) {
            LocalDate fim = inicio.with(TemporalAdjusters.next(DayOfWeek.MONDAY));
            if (fim.isAfter(limite)) fim = limite;
            resultado.add(new IndicadoresResposta.Semana(inicio, fim,
                ordens.contarClientesAtendidos(instante(inicio), instante(fim)),
                clientes.countByCadastradoEmGreaterThanEqualAndCadastradoEmLessThan(instante(inicio), instante(fim))));
            inicio = fim;
        }
        return resultado;
    }

    private IndicadoresResposta.Mes mes(YearMonth mes) {
        Instant inicio = instante(mes.atDay(1));
        Instant fim = instante(mes.plusMonths(1).atDay(1));
        BigDecimal receita = ordens.somarFaturamento(inicio, fim);
        BigDecimal custo = ordens.somarCustosDiretos(inicio, fim);
        BigDecimal gasto = despesas.somarPorCompetencia(mes.atDay(1), mes.plusMonths(1).atDay(1));
        long atendidos = ordens.contarClientesAtendidos(inicio, fim);
        long recorrentes = ordens.contarRecorrentes(inicio, fim);
        return new IndicadoresResposta.Mes(mes, receita, custo, gasto, receita.subtract(custo), receita.subtract(custo).subtract(gasto),
            pagamentos.somarRecebimentos(inicio, fim), estornos.somarEstornos(inicio, fim),
            atendidos, atendidos - recorrentes, recorrentes,
            remaps.contarCategoriaNoPeriodo(CategoriaServico.REMAP_ECU, inicio, fim),
            remaps.contarCategoriaNoPeriodo(CategoriaServico.REMAP_TCU, inicio, fim));
    }

    private BigDecimal tempoMedio(Instant inicio, Instant fim) {
        long segundos = 0;
        long quantidade = 0;
        int pagina = 0;
        Slice<OrdemServicoRepository.IntervaloAtendimento> lote;
        do {
            lote = ordens.findTemposConclusao(inicio, fim, PageRequest.of(pagina++, 100));
            for (var intervalo : lote) {
                segundos += Duration.between(intervalo.getInicio(), intervalo.getFim()).getSeconds();
                quantidade++;
            }
        } while (lote.hasNext());
        return media(BigDecimal.valueOf(segundos), quantidade);
    }

    private BigDecimal intervaloMedio(Instant inicio, Instant fim) {
        long segundos = 0;
        long quantidade = 0;
        int pagina = 0;
        Slice<OrdemServicoRepository.IntervaloAtendimento> lote;
        do {
            lote = ordens.findIntervalosVisitas(inicio, fim, PageRequest.of(pagina++, 100));
            for (var intervalo : lote) {
                if (intervalo.getInicio() == null) {
                    continue;
                }
                segundos += Duration.between(intervalo.getInicio(), intervalo.getFim()).getSeconds();
                quantidade++;
            }
        } while (lote.hasNext());
        return quantidade == 0 ? null : BigDecimal.valueOf(segundos).divide(BigDecimal.valueOf(quantidade * 86400), 2, RoundingMode.HALF_UP);
    }

    private List<IndicadoresResposta.Grupo> grupos(List<GrupoQuantidade> grupos) {
        return grupos.stream().map(g -> new IndicadoresResposta.Grupo(g.getNome(), g.getQuantidade())).toList();
    }

    private BigDecimal media(BigDecimal valor, long quantidade) {
        return quantidade == 0 ? BigDecimal.ZERO.setScale(2) : valor.divide(BigDecimal.valueOf(quantidade), 2, RoundingMode.HALF_UP);
    }

    private Instant instante(LocalDate data) {
        return data.atStartOfDay(fuso).toInstant();
    }
}
