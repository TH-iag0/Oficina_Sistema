package br.com.example.autogestao_backend.service;

import br.com.example.autogestao_backend.repository.ClienteRepository;
import br.com.example.autogestao_backend.repository.ServicoRepository;
import br.com.example.autogestao_backend.repository.VeiculoRepository;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class ExclusaoCadastroService {
    private final ClienteRepository clientes;
    private final VeiculoRepository veiculos;
    private final ServicoRepository servicos;

    public ExclusaoCadastroService(ClienteRepository clientes, VeiculoRepository veiculos, ServicoRepository servicos) {
        this.clientes = clientes;
        this.veiculos = veiculos;
        this.servicos = servicos;
    }

    public void excluirCliente(UUID id) { excluir(clientes, id); }
    public void excluirVeiculo(UUID id) { excluir(veiculos, id); }
    public void excluirServico(UUID id) { excluir(servicos, id); }

    private <T> void excluir(JpaRepository<T, UUID> repositorio, UUID id) {
        // As chaves estrangeiras impedem remover cadastros vinculados, inclusive em operações concorrentes.
        T registro = Registros.obter(repositorio, id);
        repositorio.delete(registro);
        repositorio.flush();
    }
}
