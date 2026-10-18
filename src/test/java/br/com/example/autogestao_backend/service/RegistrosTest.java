package br.com.example.autogestao_backend.service;

import br.com.example.autogestao_backend.shared.exception.ErroApi;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.data.domain.PageImpl;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RegistrosTest {
    @ParameterizedTest
    @CsvSource({"-1,20", "0,0", "0,101"})
    void rejeitaLimitesInvalidos(int pagina, int tamanho) {
        assertThrows(ErroApi.class, () -> Registros.pagina(pagina, tamanho));
    }

    @Test
    void paginasPossuemOrdenacaoEstavel() {
        var pagina = Registros.pagina(2, 100);
        assertEquals(200, pagina.getOffset());
        assertEquals("id: ASC", pagina.getSort().toString());
    }

    @Test
    void mapeamentoPreservaMetadadosDaPagina() {
        var origem = new PageImpl<>(List.of("a", "b"), Registros.pagina(1, 2), 5);
        var resultado = Registros.mapear(origem, String::toUpperCase);
        assertEquals(List.of("A", "B"), resultado.itens());
        assertEquals(1, resultado.pagina());
        assertEquals(5, resultado.total());
        assertEquals(3, resultado.totalPaginas());
    }
}
