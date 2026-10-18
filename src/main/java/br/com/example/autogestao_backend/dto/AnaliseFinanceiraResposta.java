package br.com.example.autogestao_backend.dto;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;

public record AnaliseFinanceiraResposta(List<Mes> fluxoCaixa, List<Categoria> despesasPorCategoria) {
    public record Mes(YearMonth mes, BigDecimal entradas, BigDecimal despesasPagas,
        BigDecimal estornos, BigDecimal saidas, BigDecimal saldo) {}
    public record Categoria(String nome, BigDecimal valor) {}
}
