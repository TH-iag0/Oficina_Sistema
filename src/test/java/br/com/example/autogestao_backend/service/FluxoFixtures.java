package br.com.example.autogestao_backend.service;

import br.com.example.autogestao_backend.model.CategoriaServico;
import br.com.example.autogestao_backend.model.Cliente;
import br.com.example.autogestao_backend.model.OrdemServico;
import br.com.example.autogestao_backend.model.Servico;
import br.com.example.autogestao_backend.model.StatusOrdemServico;
import br.com.example.autogestao_backend.model.TipoCliente;
import br.com.example.autogestao_backend.model.Veiculo;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;

final class FluxoFixtures {
    static final Clock CLOCK = Clock.fixed(Instant.parse("2026-10-13T15:00:00Z"), ZoneId.of("America/Sao_Paulo"));
    static Cliente cliente() { return new Cliente(TipoCliente.PF, "João", null, null, CLOCK); }
    static OrdemServico ordem(CategoriaServico categoria) {
        var cliente = cliente();
        var ordem = new OrdemServico(1, cliente, new Veiculo(cliente, "ABC1D23", "VW", "Golf", 1000), CLOCK);
        ordem.adicionarServico(new Servico("Teste", categoria, new BigDecimal("100"), new BigDecimal("30")), 1, new BigDecimal("100"), new BigDecimal("30"));
        ordem.transicionar(StatusOrdemServico.RECEBIDO, CLOCK);
        return ordem;
    }
    static OrdemServico emServico(CategoriaServico categoria) {
        var ordem = ordem(categoria);
        ordem.transicionar(StatusOrdemServico.DIAGNOSTICO, CLOCK);
        ordem.transicionar(StatusOrdemServico.AGUARDANDO_APROVACAO, CLOCK);
        ordem.aprovar("Aprovado", CLOCK);
        ordem.transicionar(StatusOrdemServico.EM_SERVICO, CLOCK);
        return ordem;
    }

    private FluxoFixtures() {}
}
