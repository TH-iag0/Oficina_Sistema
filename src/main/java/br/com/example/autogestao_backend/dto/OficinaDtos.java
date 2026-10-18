package br.com.example.autogestao_backend.dto;

import br.com.example.autogestao_backend.model.CategoriaServico;
import br.com.example.autogestao_backend.model.FormaPagamento;
import br.com.example.autogestao_backend.model.StatusOrdemServico;
import br.com.example.autogestao_backend.model.TipoMarcacao;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

public final class OficinaDtos {
    private OficinaDtos() {}
    public record VeiculoEntrada(
        @NotNull UUID clienteId,
        @NotBlank @Size(max = 8) String placa,
        @NotBlank @Size(max = 80) String marca,
        @NotBlank @Size(max = 120) String modelo,
        @PositiveOrZero long quilometragem
    ) {
    }

    public record VeiculoEdicao(
        @NotBlank @Size(max = 80) String marca,
        @NotBlank @Size(max = 120) String modelo,
        @PositiveOrZero long quilometragem
    ) {
    }

    public record RevisaoEntrada(LocalDate data, @Positive Long quilometragem) {
    }

    public record VeiculoResposta(
        UUID id,
        UUID clienteId,
        String placa,
        String marca,
        String modelo,
        long quilometragem,
        LocalDate proximaRevisaoData,
        Long proximaRevisaoKm,
        boolean ativo
    ) {
    }

    public record ServicoEntrada(
        @NotBlank @Size(max = 120) String nome,
        @NotNull CategoriaServico categoria,
        @NotNull @PositiveOrZero @Digits(integer = 13, fraction = 2) BigDecimal preco,
        @NotNull @PositiveOrZero @Digits(integer = 13, fraction = 2) BigDecimal custo,
        UUID categoriaId
    ) {
        public ServicoEntrada(String nome, CategoriaServico categoria, BigDecimal preco, BigDecimal custo) {
            this(nome, categoria, preco, custo, null);
        }
    }

    public record ServicoResposta(
        UUID id,
        String nome,
        CategoriaServico categoria,
        BigDecimal preco,
        BigDecimal custo,
        boolean ativo, UUID categoriaId, String categoriaNome
    ) {
    }

    public record FuncionarioEntrada(
        @NotBlank @Size(max = 120) String nome,
        @NotBlank @Size(max = 32) String matricula,
        @NotBlank @Pattern(regexp = "\\d{4,12}") String pin
    ) {
        @Override
        public String toString() {
            return "FuncionarioEntrada[credencial omitida]";
        }
    }

    public record FuncionarioEdicao(@NotBlank @Size(max = 120) String nome, @NotNull Boolean ativo) {
    }

    public record PinEntrada(@NotBlank @Pattern(regexp = "\\d{4,12}") String pin) {
        @Override
        public String toString() {
            return "PinEntrada[credencial omitida]";
        }
    }

    public record FuncionarioResposta(UUID id, String nome, String matricula, boolean ativo) {
    }

    public record JornadaEntrada(
        @NotNull DayOfWeek dia,
        @NotNull LocalTime entrada,
        @NotNull LocalTime saida,
        @PositiveOrZero int intervaloMinutos,
        @PositiveOrZero int toleranciaMinutos,
        @NotNull LocalDate vigenciaInicio,
        LocalDate vigenciaFim,
        Boolean intervaloAutomatico
    ) {
        public JornadaEntrada(DayOfWeek dia, LocalTime entrada, LocalTime saida, int intervaloMinutos,
            int toleranciaMinutos, LocalDate vigenciaInicio, LocalDate vigenciaFim) {
            this(dia, entrada, saida, intervaloMinutos, toleranciaMinutos, vigenciaInicio, vigenciaFim, false);
        }
    }

    public record JornadaResposta(
        UUID id,
        UUID funcionarioId,
        DayOfWeek dia,
        LocalTime entrada,
        LocalTime saida,
        int intervaloMinutos,
        int toleranciaMinutos,
        LocalDate vigenciaInicio,
        LocalDate vigenciaFim,
        boolean intervaloAutomatico
    ) {
    }

    public record SemanaEntrada(
        @NotNull @Size(min = 1, max = 7) List<@NotNull @Valid JornadaEntrada> jornadas,
        Boolean encerrarAnteriores
    ) {}

    public record OsEntrada(@NotNull UUID clienteId, @NotNull UUID veiculoId, Instant previsaoEntrega,
        @Valid ItemEntrada servicoInicial, StatusOrdemServico situacaoInicial) {
        public OsEntrada(UUID clienteId, UUID veiculoId, Instant previsaoEntrega) {
            this(clienteId, veiculoId, previsaoEntrega, null, null);
        }
    }

    public record OsResposta(
        UUID id,
        long numero,
        UUID clienteId,
        UUID veiculoId,
        StatusOrdemServico status,
        Instant abertaEm,
        Instant previsaoEntrega,
        BigDecimal total,
        BigDecimal custo,
        List<ItemResposta> itens,
        List<StatusResposta> historico
    ) {
    }

    public record ItemEntrada(
        @NotNull UUID servicoId,
        @Positive int quantidade,
        @NotNull @PositiveOrZero @Digits(integer = 13, fraction = 2) BigDecimal preco,
        @NotNull @PositiveOrZero @Digits(integer = 13, fraction = 2) BigDecimal custo
    ) {
    }

    public record ItemResposta(
        UUID id,
        UUID servicoId,
        String descricao,
        CategoriaServico categoria,
        int quantidade,
        BigDecimal preco,
        BigDecimal custo,
        Instant concluidoEm,
        boolean retrabalho,
        String relato
    ) {
    }

    public record StatusEntrada(@NotNull StatusOrdemServico status) {
    }

    public record AprovacaoEntrada(@NotBlank @Size(max = 1000) String consentimento) {
    }

    public record ConclusaoEntrada(
        @NotNull @PositiveOrZero @Digits(integer = 13, fraction = 2) BigDecimal custoReal,
        @Size(max = 500) String motivoRetrabalho,
        @Size(max = 4000) String relato
    ) {
    }

    public record StatusResposta(StatusOrdemServico status, Instant instante) {
    }

    public record ExecucaoEntrada(@NotNull UUID itemId, @NotNull UUID funcionarioId) {
    }

    public record ExecucaoResposta(UUID id, UUID itemId, UUID funcionarioId, Instant inicio, Instant fim) {
    }

    public record PagamentoEntrada(
        @NotNull UUID ordemServicoId,
        @NotNull @Positive @Digits(integer = 13, fraction = 2) BigDecimal valor,
        @NotNull FormaPagamento forma,
        @NotNull UUID requisicaoId
    ) {
    }

    public record PagamentoResposta(
        UUID id,
        UUID ordemServicoId,
        BigDecimal valor,
        FormaPagamento forma,
        Instant recebidoEm,
        UUID requisicaoId
    ) {
    }

    public record EstornoEntrada(
        @NotNull UUID pagamentoId,
        @NotNull @Positive @Digits(integer = 13, fraction = 2) BigDecimal valor,
        @NotBlank @Size(max = 500) String motivo,
        @NotNull UUID requisicaoId
    ) {
    }

    public record EstornoResposta(
        UUID id,
        UUID pagamentoId,
        BigDecimal valor,
        String motivo,
        Instant instante,
        UUID requisicaoId
    ) {
    }

    public record DespesaEntrada(
        @NotBlank @Size(max = 160) String descricao,
        @NotBlank @Size(max = 80) String categoria,
        @NotNull @Positive @Digits(integer = 13, fraction = 2) BigDecimal valor,
        @NotNull LocalDate competencia,
        @NotNull LocalDate vencimento
    ) {
    }

    public record DespesaResposta(
        UUID id,
        String descricao,
        String categoria,
        BigDecimal valor,
        LocalDate competencia,
        LocalDate vencimento,
        Instant pagaEm
    ) {
    }

    public record RemapEntrada(
        @NotNull UUID itemId,
        @NotBlank @Size(max = 80) String tipo,
        @NotBlank @Size(max = 100) String fabricante,
        @NotBlank @Size(max = 120) String modeloModulo,
        @NotBlank @Size(max = 1000) String objetivo
    ) {
    }

    public record RemapEdicao(
        @Size(max = 120) String hardware,
        @Size(max = 120) String software,
        @Size(max = 120) String calibracao,
        @Size(max = 2000) String alteracoes
    ) {
    }

    public record RemapResposta(
        UUID id,
        UUID itemId,
        String tipo,
        String fabricante,
        String modeloModulo,
        String objetivo,
        String hardware,
        String software,
        String calibracao,
        String alteracoes
    ) {
    }

    public record DiagnosticoEntrada(@NotNull UUID itemId, @NotBlank @Size(max = 2000) String sintomas) {
    }

    public record DiagnosticoEdicao(
        @Size(max = 120) String equipamento,
        @Size(max = 1000) String dtcs,
        @NotBlank @Size(max = 4000) String testes,
        @Size(max = 500) String causa,
        UUID reparoAprovadoId
    ) {
    }

    public record DiagnosticoResposta(
        UUID id,
        UUID itemId,
        String sintomas,
        String equipamento,
        String dtcs,
        String testes,
        String causa,
        UUID reparoAprovadoId
    ) {
    }

    public record AjustePontoEntrada(
        @NotNull UUID funcionarioId,
        UUID originalId,
        TipoMarcacao tipo,
        Instant instante,
        @NotBlank @Size(max = 500) String motivo,
        @NotNull UUID requisicaoId
    ) {
    }

    public record AjusteResposta(
        UUID id,
        UUID originalId,
        UUID substitutaId,
        String motivo,
        String autor,
        Instant registradoEm
    ) {
    }

    public record DiaPonto(
        LocalDate data,
        long minutosTrabalhados,
        long minutosPrevistos,
        long minutosAtraso,
        long minutosExtras,
        boolean incompleto
    ) {
    }

    public record ResumoPonto(
        UUID funcionarioId,
        LocalDate inicio,
        LocalDate fimExclusivo,
        long minutosTrabalhados,
        long minutosExtras,
        long minutosAtraso,
        long minutosEmServicos,
        List<DiaPonto> dias
    ) {
    }

    public record SenhaEntrada(
        @NotBlank String senhaAtual,
        @NotBlank @Size(min = 12, max = 72) String novaSenha
    ) {
        @Override
        public String toString() {
            return "SenhaEntrada[credencial omitida]";
        }
    }
}
