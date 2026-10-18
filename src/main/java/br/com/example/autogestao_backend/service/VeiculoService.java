package br.com.example.autogestao_backend.service;

import br.com.example.autogestao_backend.dto.OficinaDtos;
import br.com.example.autogestao_backend.dto.PaginaResposta;
import br.com.example.autogestao_backend.model.Veiculo;
import br.com.example.autogestao_backend.repository.ClienteRepository;
import br.com.example.autogestao_backend.repository.VeiculoRepository;
import br.com.example.autogestao_backend.shared.exception.ErroApi;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class VeiculoService {
    private final VeiculoRepository veiculos;
    private final ClienteRepository clientes;
    public VeiculoService(VeiculoRepository veiculos, ClienteRepository clientes) {
        this.veiculos = veiculos;
        this.clientes = clientes;
    }

    @Transactional
    public OficinaDtos.VeiculoResposta cadastrar(OficinaDtos.VeiculoEntrada dados) {
        var cliente = Registros.obter(clientes, dados.clienteId());
        if (!cliente.isAtivo()) {
            throw new ErroApi(HttpStatus.CONFLICT, "Cliente inativo.");
        }
        var veiculo = new Veiculo(cliente, dados.placa(), dados.marca(), dados.modelo(), dados.quilometragem());
        if (veiculos.existsByPlaca(veiculo.getPlaca())) {
            throw new ErroApi(HttpStatus.CONFLICT, "Placa já cadastrada.");
        }
        return resposta(veiculos.saveAndFlush(veiculo));
    }

    public PaginaResposta<OficinaDtos.VeiculoResposta> listar(int pagina, int tamanho) {
        return listar("", null, null, pagina, tamanho);
    }

    public PaginaResposta<OficinaDtos.VeiculoResposta> listar(String busca, UUID clienteId, Boolean ativo, int pagina, int tamanho) {
        return Registros.mapear(veiculos.pesquisar(busca.strip(), clienteId, ativo,
            Registros.pagina(pagina, tamanho, org.springframework.data.domain.Sort.by("placa", "id"))), this::resposta);
    }

    public OficinaDtos.VeiculoResposta buscar(UUID id) {
        return resposta(Registros.obter(veiculos, id));
    }

    @Transactional
    public OficinaDtos.VeiculoResposta atualizar(UUID id, OficinaDtos.VeiculoEdicao dados) {
        var veiculo = Registros.obter(veiculos, id);
        veiculo.atualizarCadastro(dados.marca(), dados.modelo(), dados.quilometragem());
        return resposta(veiculo);
    }

    @Transactional
    public OficinaDtos.VeiculoResposta revisao(UUID id, OficinaDtos.RevisaoEntrada dados) {
        var veiculo = Registros.obter(veiculos, id);
        veiculo.programarRevisao(dados.data(), dados.quilometragem());
        return resposta(veiculo);
    }

    @Transactional
    public void desativar(UUID id) {
        Registros.obter(veiculos, id).desativar();
    }

    private OficinaDtos.VeiculoResposta resposta(Veiculo v) {
        return new OficinaDtos.VeiculoResposta(v.getId(), v.getCliente().getId(), v.getPlaca(),
            v.getMarca(), v.getModelo(), v.getQuilometragem(), v.getProximaRevisaoData(), v.getProximaRevisaoKm(), v.isAtivo());
    }
}
