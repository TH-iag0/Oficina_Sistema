package br.com.example.autogestao_backend.service;

import br.com.example.autogestao_backend.dto.ClienteEntrada;
import br.com.example.autogestao_backend.dto.ClienteResposta;
import br.com.example.autogestao_backend.dto.PaginaResposta;
import br.com.example.autogestao_backend.model.Cliente;
import br.com.example.autogestao_backend.repository.ClienteRepository;
import br.com.example.autogestao_backend.shared.exception.ErroApi;
import java.time.Clock;
import java.util.UUID;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class ClienteService {
    private final ClienteRepository clientes;
    private final Clock relogio;

    public ClienteService(ClienteRepository clientes, Clock relogio) {
        this.clientes = clientes;
        this.relogio = relogio;
    }

    @Transactional
    public ClienteResposta cadastrar(ClienteEntrada entrada) {
        Cliente cliente = new Cliente(entrada.tipo(), entrada.nome(), entrada.documento(), entrada.telefone(), relogio);
        DocumentoCliente.validar(cliente.getDocumento());
        if (cliente.getDocumento() != null && clientes.existsByDocumento(cliente.getDocumento())) {
            throw new ErroApi(HttpStatus.CONFLICT, "Documento já cadastrado.");
        }
        return resposta(clientes.saveAndFlush(cliente));
    }

    public PaginaResposta<ClienteResposta> listar(String nome, int pagina, int tamanho) {
        var resultado = clientes.findByAtivoTrueAndNomeContainingIgnoreCase(nome,
            Registros.pagina(pagina, tamanho, Sort.by("nome", "id")));
        return Registros.mapear(resultado, this::resposta);
    }

    public ClienteResposta buscar(UUID id) {
        return resposta(encontrar(id));
    }

    @Transactional
    public ClienteResposta atualizar(UUID id, ClienteEntrada entrada) {
        Cliente cliente = encontrar(id);
        String documento = Cliente.normalizarDocumento(entrada.documento(), entrada.tipo());
        DocumentoCliente.validar(documento);
        if (documento != null) {
            clientes.findByDocumento(documento).filter(outro -> !outro.getId().equals(id))
                .ifPresent(outro -> { throw new ErroApi(HttpStatus.CONFLICT, "Documento já cadastrado."); });
        }
        cliente.atualizarCadastro(entrada.tipo(), entrada.nome(), documento, entrada.telefone());
        return resposta(cliente);
    }

    @Transactional
    public void desativar(UUID id) {
        encontrar(id).desativar();
    }

    private Cliente encontrar(UUID id) {
        return clientes.findById(id).orElseThrow(() -> new ErroApi(HttpStatus.NOT_FOUND, "Cliente não encontrado."));
    }

    private ClienteResposta resposta(Cliente cliente) {
        return new ClienteResposta(cliente.getId(), cliente.getTipo(), cliente.getNome(),
            cliente.getDocumento(), cliente.getTelefone(), cliente.getCadastradoEm(), cliente.isAtivo());
    }
}
