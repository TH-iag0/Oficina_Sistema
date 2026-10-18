package br.com.example.autogestao_backend.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

public record IndicadoresResposta(List<Mes> evolucao, long aguardandoDiagnostico,
    BigDecimal segundosMediosConclusao, long retrabalhos, long diagnosticos, long diagnosticosConvertidos,
    List<Grupo> marcas, List<Grupo> modelos, List<Grupo> causas,
    List<DashboardResposta.ServicoResumo> servicosRentaveis,
    List<OficinaDtos.VeiculoResposta> revisoesPendentes, long novosClientesCadastrados,
    BigDecimal gastoMedioCliente, BigDecimal diasMediosEntreVisitas, List<Semana> atendimentosSemanais) {
    public record Semana(LocalDate inicio, LocalDate fimExclusivo,
        long clientesAtendidos, long novosClientesCadastrados) {}
    public record Mes(YearMonth mes, BigDecimal receita, BigDecimal custosDiretos, BigDecimal despesasOperacionais,
        BigDecimal lucroBruto, BigDecimal resultadoOperacional, BigDecimal recebimentos, BigDecimal estornos,
        long clientesAtendidos, long clientesNovos, long clientesRecorrentes, long remapsEcu, long remapsTcu) {}
    public record Grupo(String nome, long quantidade) {}
}
