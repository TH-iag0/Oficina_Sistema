package br.com.example.autogestao_backend.model;

import org.junit.jupiter.api.Test;
import static br.com.example.autogestao_backend.model.ModelFixtures.RELOGIO;
import static br.com.example.autogestao_backend.model.ModelFixtures.iniciar;
import static br.com.example.autogestao_backend.model.ModelFixtures.item;
import static br.com.example.autogestao_backend.model.ModelFixtures.os;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RelatoServicoTest {
    @Test
    void guardaDescricaoSemEspacosExternosAoConcluir() {
        var ordem = os();
        var item = item(ordem, CategoriaServico.MANUTENCAO);
        iniciar(ordem);
        item.concluir(RELOGIO, "  Troquei óleo e filtro. Cliente relatou ruído.  ");
        assertEquals("Troquei óleo e filtro. Cliente relatou ruído.", item.getRelato());
        assertNotNull(item.getConcluidoEm());
        assertThrows(IllegalStateException.class, () -> item.concluir(RELOGIO, "Outra descrição"));
        assertEquals("Troquei óleo e filtro. Cliente relatou ruído.", item.getRelato());
    }

    @Test
    void relatoAcimaDoLimiteNaoConcluiNemAlteraOItem() {
        var ordem = os();
        var item = item(ordem, CategoriaServico.MANUTENCAO);
        iniciar(ordem);
        var texto = "a".repeat(4001);
        assertThrows(IllegalArgumentException.class, () -> item.concluir(RELOGIO, texto));
        assertNull(item.getConcluidoEm());
        assertNull(item.getRelato());
    }

    @Test
    void chamadasAntigasSemRelatoContinuamValidas() {
        var ordem = os();
        var item = item(ordem, CategoriaServico.MANUTENCAO);
        iniciar(ordem);
        item.concluir(RELOGIO);
        assertNull(item.getRelato());
        assertNotNull(item.getConcluidoEm());
    }
}
