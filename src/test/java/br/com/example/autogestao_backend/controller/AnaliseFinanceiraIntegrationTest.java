package br.com.example.autogestao_backend.controller;

import br.com.example.autogestao_backend.model.Despesa;
import br.com.example.autogestao_backend.repository.DespesaRepository;
import br.com.example.autogestao_backend.support.IntegrationApiTest;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@IntegrationApiTest
class AnaliseFinanceiraIntegrationTest {
    private final MockMvc mvc;
    private final DespesaRepository despesas;

    @Autowired
    AnaliseFinanceiraIntegrationTest(MockMvc mvc, DespesaRepository despesas) {
        this.mvc = mvc;
        this.despesas = despesas;
    }

    @Test
    void agregaMaisDeCemDespesasESeparaPagamentoDaCompetencia() throws Exception {
        for (int i = 0; i < 105; i++) despesa("Estrutura", "1.00", "2026-10-01", "2026-10-01T03:00:00Z");
        despesa("Operação", "7.00", "2026-10-01", null);
        despesa("Mês anterior", "5.00", "2026-09-01", "2026-11-01T02:59:59Z");
        despesa("Mês seguinte", "10.00", "2026-11-01", "2026-11-01T03:00:00Z");
        mvc.perform(get("/api/financeiro/analises").param("mes", "2026-10").param("meses", "1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.fluxoCaixa[0].entradas").value(0))
            .andExpect(jsonPath("$.fluxoCaixa[0].saidas").value(110))
            .andExpect(jsonPath("$.fluxoCaixa[0].saldo").value(-110))
            .andExpect(jsonPath("$.despesasPorCategoria.length()").value(2))
            .andExpect(jsonPath("$.despesasPorCategoria[0].nome").value("Estrutura"))
            .andExpect(jsonPath("$.despesasPorCategoria[0].valor").value(105))
            .andExpect(jsonPath("$.despesasPorCategoria[1].valor").value(7));
        assertEquals(108, despesas.count());
    }

    @Test
    void validaMesEQuantidadeAntesDeConsultar() throws Exception {
        for (String quantidade : new String[] { "0", "25" }) {
            mvc.perform(get("/api/financeiro/analises").param("mes", "2026-10").param("meses", quantidade))
                .andExpect(status().isBadRequest());
        }
        mvc.perform(get("/api/financeiro/analises").param("mes", "2026-13"))
            .andExpect(status().isBadRequest());
    }

    private void despesa(String categoria, String valor, String competencia, String pagamento) {
        var data = LocalDate.parse(competencia);
        var despesa = new Despesa("Teste", categoria, new BigDecimal(valor), data, data);
        if (pagamento != null) despesa.confirmarPagamento(Clock.fixed(Instant.parse(pagamento), ZoneOffset.UTC));
        despesas.saveAndFlush(despesa);
    }
}
