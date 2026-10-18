package br.com.example.autogestao_backend.dto;

import br.com.example.autogestao_backend.model.CategoriaServico;
import br.com.example.autogestao_backend.model.StatusOrdemServico;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public record DashboardResposta(LocalDate inicio, LocalDate fimExclusivo, BigDecimal faturamento,
    BigDecimal variacaoPercentual, BigDecimal custosDiretos, BigDecimal lucroBruto,
    BigDecimal margemPercentual, long ordensConcluidas, BigDecimal ticketMedio,
    long clientesNoMes, long clientesNaSemana, Map<StatusOrdemServico, Long> ordensPorStatus,
    List<ServicoResumo> servicosMaisExecutados) {
    public record ServicoResumo(UUID servicoId, String nome, CategoriaServico categoria,
                                long quantidade, BigDecimal receita, BigDecimal custoDireto) {}
}
