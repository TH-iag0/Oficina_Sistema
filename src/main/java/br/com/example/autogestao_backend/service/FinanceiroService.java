package br.com.example.autogestao_backend.service;

import br.com.example.autogestao_backend.dto.OficinaDtos;
import br.com.example.autogestao_backend.dto.PaginaResposta;
import br.com.example.autogestao_backend.model.Despesa;
import br.com.example.autogestao_backend.model.EstornoPagamento;
import br.com.example.autogestao_backend.model.Pagamento;
import br.com.example.autogestao_backend.repository.DespesaRepository;
import br.com.example.autogestao_backend.repository.EstornoPagamentoRepository;
import br.com.example.autogestao_backend.repository.OrdemServicoRepository;
import br.com.example.autogestao_backend.repository.PagamentoRepository;
import br.com.example.autogestao_backend.shared.exception.ErroApi;
import java.math.BigDecimal;
import java.time.Clock;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class FinanceiroService {
    private final OrdemServicoRepository ordens;
    private final PagamentoRepository pagamentos;
    private final EstornoPagamentoRepository estornos;
    private final DespesaRepository despesas;
    private final Clock relogio;
    public FinanceiroService(OrdemServicoRepository ordens, PagamentoRepository pagamentos,
        EstornoPagamentoRepository estornos, DespesaRepository despesas, Clock relogio) {
        this.ordens = ordens;
        this.pagamentos = pagamentos;
        this.estornos = estornos;
        this.despesas = despesas;
        this.relogio = relogio;
    }

    @Transactional
    public OficinaDtos.PagamentoResposta receber(OficinaDtos.PagamentoEntrada dados) {
        var ordem = ordens.findParaAtualizacao(dados.ordemServicoId()).orElseThrow(this::ausente);
        var anterior = pagamentos.findByRequisicaoId(dados.requisicaoId());
        if (anterior.isPresent()) {
            var p = anterior.get();
            exigir(p.getOrdemServico().getId().equals(dados.ordemServicoId())
                && p.getValor().compareTo(dados.valor()) == 0 && p.getForma() == dados.forma(), "Confirmação já utilizada.");
            return resposta(p);
        }
        BigDecimal liquido = pagamentos.somarPorOrdemServico(ordem.getId()).subtract(estornos.somarPorOrdemServico(ordem.getId()));
        exigir(liquido.add(dados.valor()).compareTo(ordem.getTotalServicos()) <= 0, "Pagamento excede o saldo da OS.");
        return resposta(pagamentos.saveAndFlush(new Pagamento(ordem, dados.valor(), dados.forma(), dados.requisicaoId(), relogio)));
    }

    @Transactional
    public OficinaDtos.EstornoResposta estornar(OficinaDtos.EstornoEntrada dados) {
        var leitura = Registros.obter(pagamentos, dados.pagamentoId());
        ordens.findParaAtualizacao(leitura.getOrdemServico().getId()).orElseThrow(this::ausente);
        var pagamento = pagamentos.findParaAtualizacao(dados.pagamentoId()).orElseThrow(this::ausente);
        var anterior = estornos.findByRequisicaoId(dados.requisicaoId());
        if (anterior.isPresent()) {
            var e = anterior.get();
            exigir(e.getPagamento().getId().equals(dados.pagamentoId()) && e.getValor().compareTo(dados.valor()) == 0
                && e.getMotivo().equals(dados.motivo().strip()), "Confirmação já utilizada.");
            return resposta(e);
        }
        exigir(estornos.somarPorPagamento(pagamento.getId()).add(dados.valor()).compareTo(pagamento.getValor()) <= 0,
            "Total de estornos excede o pagamento.");
        return resposta(estornos.saveAndFlush(new EstornoPagamento(pagamento, dados.valor(), dados.motivo(), dados.requisicaoId(), relogio)));
    }

    public PaginaResposta<OficinaDtos.PagamentoResposta> pagamentos(UUID ordemId, int pagina, int tamanho) {
        return Registros.mapear(pagamentos.findByOrdemServicoId(ordemId, Registros.pagina(pagina, tamanho)), this::resposta);
    }

    public PaginaResposta<OficinaDtos.EstornoResposta> estornos(UUID pagamentoId, int pagina, int tamanho) {
        return Registros.mapear(estornos.findByPagamentoId(pagamentoId, Registros.pagina(pagina, tamanho)), this::resposta);
    }

    @Transactional
    public OficinaDtos.DespesaResposta despesa(OficinaDtos.DespesaEntrada dados) {
        return resposta(despesas.saveAndFlush(new Despesa(dados.descricao(), dados.categoria(), dados.valor(), dados.competencia(), dados.vencimento())));
    }

    @Transactional
    public OficinaDtos.DespesaResposta pagarDespesa(UUID id) {
        var despesa = despesas.findParaAtualizacao(id).orElseThrow(this::ausente);
        if (despesa.getPagaEm() == null) despesa.confirmarPagamento(relogio);
        return resposta(despesa);
    }

    public PaginaResposta<OficinaDtos.DespesaResposta> despesas(int pagina, int tamanho) {
        return Registros.mapear(despesas.findAll(Registros.pagina(pagina, tamanho)), this::resposta);
    }

    private void exigir(boolean condicao, String motivo) {
        if (!condicao) {
            throw new ErroApi(HttpStatus.CONFLICT, motivo);
        }
    }

    private ErroApi ausente() {
        return new ErroApi(HttpStatus.NOT_FOUND, "Registro não encontrado.");
    }

    private OficinaDtos.PagamentoResposta resposta(Pagamento p) {
        return new OficinaDtos.PagamentoResposta(p.getId(), p.getOrdemServico().getId(), p.getValor(), p.getForma(), p.getRecebidoEm(), p.getRequisicaoId());
    }

    private OficinaDtos.EstornoResposta resposta(EstornoPagamento e) {
        return new OficinaDtos.EstornoResposta(e.getId(), e.getPagamento().getId(), e.getValor(), e.getMotivo(), e.getInstante(), e.getRequisicaoId());
    }

    private OficinaDtos.DespesaResposta resposta(Despesa d) {
        return new OficinaDtos.DespesaResposta(d.getId(), d.getDescricao(), d.getCategoria(), d.getValor(), d.getCompetencia(), d.getVencimento(), d.getPagaEm());
    }
}
