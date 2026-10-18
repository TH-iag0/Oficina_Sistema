package br.com.example.autogestao_backend.service;

import br.com.example.autogestao_backend.dto.OficinaDtos;
import br.com.example.autogestao_backend.dto.PaginaResposta;
import br.com.example.autogestao_backend.model.Servico;
import br.com.example.autogestao_backend.model.CategoriaCatalogo;
import br.com.example.autogestao_backend.repository.CategoriaCatalogoRepository;
import br.com.example.autogestao_backend.repository.ServicoRepository;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class CatalogoService {
    private final ServicoRepository servicos;
    private final CategoriaCatalogoRepository categorias;
    public CatalogoService(ServicoRepository servicos, CategoriaCatalogoRepository categorias) {
        this.servicos = servicos;
        this.categorias = categorias;
    }

    @Transactional
    public OficinaDtos.ServicoResposta cadastrar(OficinaDtos.ServicoEntrada dados) {
        var categoria = categoria(dados);
        var servico = new Servico(dados.nome(), categoria == null ? dados.categoria() : categoria.getClassificacao(), dados.preco(), dados.custo());
        servico.definirCategoriaCatalogo(categoria);
        return resposta(servicos.saveAndFlush(servico));
    }

    public PaginaResposta<OficinaDtos.ServicoResposta> listar(int pagina, int tamanho) {
        return listar("", null, pagina, tamanho);
    }

    public PaginaResposta<OficinaDtos.ServicoResposta> listar(String busca, Boolean ativo, int pagina, int tamanho) {
        return Registros.mapear(servicos.pesquisar(busca.strip(), ativo,
            Registros.pagina(pagina, tamanho, org.springframework.data.domain.Sort.by("nome", "id"))), this::resposta);
    }

    public OficinaDtos.ServicoResposta buscar(UUID id) {
        return resposta(Registros.obter(servicos, id));
    }

    @Transactional
    public OficinaDtos.ServicoResposta atualizar(UUID id, OficinaDtos.ServicoEntrada dados) {
        var servico = Registros.obter(servicos, id);
        var categoria = categoria(dados);
        servico.atualizarCadastro(dados.nome(), categoria == null ? dados.categoria() : categoria.getClassificacao(), dados.preco(), dados.custo());
        servico.definirCategoriaCatalogo(categoria);
        return resposta(servico);
    }

    @Transactional
    public void desativar(UUID id) {
        Registros.obter(servicos, id).desativar();
    }

    private OficinaDtos.ServicoResposta resposta(Servico s) {
        var categoria = s.getCategoriaCatalogo();
        return new OficinaDtos.ServicoResposta(s.getId(), s.getNome(), s.getCategoria(), s.getPrecoSugerido(), s.getCustoDiretoEstimado(), s.isAtivo(),
            categoria != null && !categoria.isPadrao() ? categoria.getId() : null,
            categoria == null ? s.getCategoria().getNome() : categoria.getNome());
    }

    private CategoriaCatalogo categoria(OficinaDtos.ServicoEntrada dados) {
        return dados.categoriaId() == null ? null : Registros.obter(categorias, dados.categoriaId());
    }
}
