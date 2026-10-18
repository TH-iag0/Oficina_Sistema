package br.com.example.autogestao_backend.service;

import br.com.example.autogestao_backend.dto.OficinaDtos;
import br.com.example.autogestao_backend.dto.PaginaResposta;
import br.com.example.autogestao_backend.model.ExecucaoServico;
import br.com.example.autogestao_backend.model.ItemOrdemServico;
import br.com.example.autogestao_backend.model.OrdemServico;
import br.com.example.autogestao_backend.model.StatusOrdemServico;
import br.com.example.autogestao_backend.repository.ClienteRepository;
import br.com.example.autogestao_backend.repository.ExecucaoServicoRepository;
import br.com.example.autogestao_backend.repository.FuncionarioRepository;
import br.com.example.autogestao_backend.repository.ItemOrdemServicoRepository;
import br.com.example.autogestao_backend.repository.NumeradorOsRepository;
import br.com.example.autogestao_backend.repository.OrdemServicoRepository;
import br.com.example.autogestao_backend.repository.ServicoRepository;
import br.com.example.autogestao_backend.repository.VeiculoRepository;
import br.com.example.autogestao_backend.shared.exception.ErroApi;
import java.time.Clock;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class OrdemServicoService {
    private final OrdemServicoRepository ordens;
    private final NumeradorOsRepository numeros;
    private final ClienteRepository clientes;
    private final VeiculoRepository veiculos;
    private final ServicoRepository servicos;
    private final ItemOrdemServicoRepository itens;
    private final ExecucaoServicoRepository execucoes;
    private final FuncionarioRepository funcionarios;
    private final Clock relogio;
    public OrdemServicoService(OrdemServicoRepository ordens, NumeradorOsRepository numeros,
        ClienteRepository clientes, VeiculoRepository veiculos, ServicoRepository servicos,
        ItemOrdemServicoRepository itens, ExecucaoServicoRepository execucoes,
        FuncionarioRepository funcionarios, Clock relogio) {
        this.ordens = ordens;
        this.numeros = numeros;
        this.clientes = clientes;
        this.veiculos = veiculos;
        this.servicos = servicos;
        this.itens = itens;
        this.execucoes = execucoes;
        this.funcionarios = funcionarios;
        this.relogio = relogio;
    }

    @Transactional
    public OficinaDtos.OsResposta abrir(OficinaDtos.OsEntrada dados) {
        var cliente = Registros.obter(clientes, dados.clienteId());
        var veiculo = Registros.obter(veiculos, dados.veiculoId());
        if (!cliente.isAtivo() || !veiculo.isAtivo()) {
            throw new ErroApi(HttpStatus.CONFLICT, "Cliente ou veículo inativo.");
        }
        var ordem = new OrdemServico(numeros.reservar().proximo(), cliente, veiculo, relogio);
        if (dados.previsaoEntrega() != null) ordem.definirPrevisaoEntrega(dados.previsaoEntrega());
        prepararAbertura(ordem, dados);
        return resposta(ordens.saveAndFlush(ordem));
    }

    private void prepararAbertura(OrdemServico ordem, OficinaDtos.OsEntrada dados) {
        var situacao = dados.situacaoInicial();
        if (situacao != null && situacao != StatusOrdemServico.RASCUNHO
            && situacao != StatusOrdemServico.RECEBIDO && situacao != StatusOrdemServico.DIAGNOSTICO) {
            throw new IllegalArgumentException("Abra a ordem como rascunho, recebida ou em diagnóstico.");
        }
        if (dados.servicoInicial() != null) {
            var item = dados.servicoInicial();
            ordem.adicionarServico(Registros.obter(servicos, item.servicoId()), item.quantidade(), item.preco(), item.custo());
        }
        if (situacao == null || situacao == StatusOrdemServico.RASCUNHO) return;
        ordem.transicionar(StatusOrdemServico.RECEBIDO, relogio);
        if (situacao == StatusOrdemServico.DIAGNOSTICO) ordem.transicionar(StatusOrdemServico.DIAGNOSTICO, relogio);
    }

    public PaginaResposta<OficinaDtos.OsResposta> listar(int pagina, int tamanho) {
        return listar("", null, pagina, tamanho);
    }

    public PaginaResposta<OficinaDtos.OsResposta> listar(String busca, StatusOrdemServico status, int pagina, int tamanho) {
        var limites = Registros.pagina(pagina, tamanho);
        var ordenacao = Sort.by(Sort.Order.desc("abertaEm"), Sort.Order.desc("id"));
        return Registros.mapear(ordens.pesquisar(busca.strip(), status,
            PageRequest.of(limites.getPageNumber(), limites.getPageSize(), ordenacao)), this::resposta);
    }

    public OficinaDtos.OsResposta buscar(UUID id) {
        return resposta(Registros.obter(ordens, id));
    }

    @Transactional
    public OficinaDtos.OsResposta adicionar(UUID id, OficinaDtos.ItemEntrada dados) {
        var ordem = bloquear(id);
        ordem.adicionarServico(Registros.obter(servicos, dados.servicoId()), dados.quantidade(), dados.preco(), dados.custo());
        return resposta(ordem);
    }

    @Transactional
    public OficinaDtos.OsResposta status(UUID id, OficinaDtos.StatusEntrada dados) {
        var ordem = bloquear(id);
        if (dados.status() == StatusOrdemServico.FINALIZADO
            && execucoes.existsByItemOrdemServicoIdAndFimIsNull(id)) {
            throw new ErroApi(HttpStatus.CONFLICT, "Encerre as execuções antes de finalizar.");
        }
        ordem.transicionar(dados.status(), relogio);
        return resposta(ordem);
    }

    @Transactional
    public OficinaDtos.OsResposta aprovar(UUID id, OficinaDtos.AprovacaoEntrada dados) {
        var ordem = bloquear(id);
        ordem.aprovar(dados.consentimento(), relogio);
        return resposta(ordem);
    }

    @Transactional
    public OficinaDtos.ItemResposta concluirItem(UUID id, OficinaDtos.ConclusaoEntrada dados) {
        var item = obterItemBloqueado(id);
        if (execucoes.existsByItemIdAndFimIsNull(id)) {
            throw new ErroApi(HttpStatus.CONFLICT, "Há execução em andamento.");
        }
        item.registrarCustoReal(dados.custoReal());
        if (dados.motivoRetrabalho() != null && !dados.motivoRetrabalho().isBlank()) item.registrarRetrabalho(dados.motivoRetrabalho());
        item.concluir(relogio, dados.relato());
        return itemResposta(item);
    }

    @Transactional
    public OficinaDtos.ExecucaoResposta iniciar(OficinaDtos.ExecucaoEntrada dados) {
        var item = obterItemBloqueado(dados.itemId());
        var funcionario = funcionarios.findParaAtualizacao(dados.funcionarioId())
            .orElseThrow(() -> new ErroApi(HttpStatus.NOT_FOUND, "Funcionário não encontrado."));
        if (execucoes.existsByFuncionarioIdAndFimIsNull(funcionario.getId())) {
            throw new ErroApi(HttpStatus.CONFLICT, "Funcionário já possui execução em andamento.");
        }
        return execucaoResposta(execucoes.saveAndFlush(new ExecucaoServico(item, funcionario, relogio)));
    }

    @Transactional
    public OficinaDtos.ExecucaoResposta encerrar(UUID id) {
        var execucao = Registros.obter(execucoes, id);
        bloquear(execucao.getItem().getOrdemServico().getId());
        funcionarios.findParaAtualizacao(execucao.getFuncionario().getId()).orElseThrow();
        execucao.encerrar(relogio);
        return execucaoResposta(execucao);
    }

    public PaginaResposta<OficinaDtos.ExecucaoResposta> execucoes(UUID itemId, int pagina, int tamanho) {
        return Registros.mapear(execucoes.findByItemId(itemId, Registros.pagina(pagina, tamanho)), this::execucaoResposta);
    }

    @Transactional
    public ItemOrdemServico itemBloqueado(UUID id) {
        return obterItemBloqueado(id);
    }

    private ItemOrdemServico obterItemBloqueado(UUID id) {
        var item = Registros.obter(itens, id);
        bloquear(item.getOrdemServico().getId());
        return item;
    }

    private OrdemServico bloquear(UUID id) {
        return ordens.findParaAtualizacao(id).orElseThrow(() -> new ErroApi(HttpStatus.NOT_FOUND, "OS não encontrada."));
    }

    private OficinaDtos.OsResposta resposta(OrdemServico o) {
        return new OficinaDtos.OsResposta(o.getId(), o.getNumero(), o.getCliente().getId(), o.getVeiculo().getId(),
            o.getStatus(), o.getAbertaEm(), o.getPrevisaoEntrega(), o.getTotalServicos(), o.getTotalCustosDiretos(),
            o.getItens().stream().map(this::itemResposta).toList(),
            o.getHistorico().stream().map(h -> new OficinaDtos.StatusResposta(h.getStatus(), h.getInstante())).toList());
    }

    private OficinaDtos.ItemResposta itemResposta(ItemOrdemServico i) {
        return new OficinaDtos.ItemResposta(i.getId(), i.getServico().getId(), i.getDescricaoServico(), i.getCategoria(),
            i.getQuantidade(), i.getPrecoUnitario(), i.getCustoDiretoUnitario(), i.getConcluidoEm(), i.isRetrabalho(), i.getRelato());
    }

    private OficinaDtos.ExecucaoResposta execucaoResposta(ExecucaoServico e) {
        return new OficinaDtos.ExecucaoResposta(e.getId(), e.getItem().getId(), e.getFuncionario().getId(), e.getInicio(), e.getFim());
    }
}
