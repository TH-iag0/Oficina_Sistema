package br.com.example.autogestao_backend.model;

import java.math.BigDecimal;
import java.time.*;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

final class ModelFixtures {
    static final Instant INSTANTE = Instant.parse("2026-10-10T12:00:00Z");
    static final Clock RELOGIO = Clock.fixed(INSTANTE, ZoneOffset.UTC);
    private static final String HASH = new BCryptPasswordEncoder(4).encode("123456");
    static Clock depois(long segundos) { return Clock.fixed(INSTANTE.plusSeconds(segundos), ZoneOffset.UTC); }
    static BigDecimal valor(String valor) { return new BigDecimal(valor); }
    static Cliente cliente() { return new Cliente(TipoCliente.PF, "João", null, "11999999999", RELOGIO); }
    static Veiculo veiculo(Cliente cliente) { return new Veiculo(cliente, "ABC1D23", "Volkswagen", "Golf", 50000); }
    static Funcionario funcionario() { return new Funcionario("Carlos", "OF-001", HASH); }
    static Servico servico(CategoriaServico categoria) { return new Servico("Serviço de teste", categoria, valor("150.00"), valor("40.00")); }
    static OrdemServico os() { Cliente cliente = cliente(); return new OrdemServico(1, cliente, veiculo(cliente), RELOGIO); }
    static ItemOrdemServico item(OrdemServico os, CategoriaServico categoria) { return os.adicionarServico(servico(categoria), 1, valor("150.00"), valor("40.00")); }
    static void aprovar(OrdemServico os) {
        os.transicionar(StatusOrdemServico.RECEBIDO, RELOGIO);
        os.transicionar(StatusOrdemServico.DIAGNOSTICO, RELOGIO);
        os.transicionar(StatusOrdemServico.AGUARDANDO_APROVACAO, RELOGIO);
        os.aprovar("Cliente autorizou o orçamento.", RELOGIO);
    }
    static void iniciar(OrdemServico os) { aprovar(os); os.transicionar(StatusOrdemServico.EM_SERVICO, RELOGIO); }
}
