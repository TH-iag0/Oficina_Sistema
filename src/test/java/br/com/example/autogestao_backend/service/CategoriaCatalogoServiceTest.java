package br.com.example.autogestao_backend.service;

import br.com.example.autogestao_backend.dto.CategoriaCatalogoDtos;
import br.com.example.autogestao_backend.model.CategoriaCatalogo;
import br.com.example.autogestao_backend.model.CategoriaServico;
import br.com.example.autogestao_backend.repository.CategoriaCatalogoRepository;
import br.com.example.autogestao_backend.shared.exception.ErroApi;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CategoriaCatalogoServiceTest {
    @Test
    void normalizaAcentosUnicodeEEspacosNaoSeparaveisAntesDeConsultarDuplicidade() {
        var repo = mock(CategoriaCatalogoRepository.class);
        var service = new CategoriaCatalogoService(repo);
        when(repo.existsByNomeNormalizado("manutenção")).thenReturn(true);
        var entrada = new CategoriaCatalogoDtos.Entrada("\u00a0MANUTENC\u0327A\u0303O\u00a0");
        assertThrows(ErroApi.class, () -> service.cadastrar(entrada));
        verify(repo, never()).saveAndFlush(any());
        assertEquals("Limpeza interna", new CategoriaCatalogo("Limpeza\u00a0\u00a0interna").getNome());
    }

    @Test
    void normalizaNomeSemPerderExibicaoEClassificaNovoServicoSemAlterarRemap() {
        var categoria = new CategoriaCatalogo("  Higienização   Automotiva  ");
        assertEquals("Higienização Automotiva", categoria.getNome());
        assertEquals("higienização automotiva", categoria.getNomeNormalizado());
        assertEquals(CategoriaServico.OUTRO, categoria.getClassificacao());
        assertFalse(categoria.isPadrao());
        assertThrows(IllegalArgumentException.class, () -> new CategoriaCatalogo(" "));
        var texto = "x".repeat(81);
        assertThrows(IllegalArgumentException.class, () -> new CategoriaCatalogo(texto));
    }

    @Test
    void recusaCategoriaRepetidaMesmoComMaiusculasEEspacos() {
        var repo = mock(CategoriaCatalogoRepository.class);
        var service = new CategoriaCatalogoService(repo);
        when(repo.existsByNomeNormalizado("manutenção")).thenReturn(true);
        var entrada2 = new CategoriaCatalogoDtos.Entrada(" MANUTENÇÃO ");
        assertThrows(ErroApi.class, () -> service.cadastrar(entrada2));
        verify(repo, never()).saveAndFlush(any());
    }
}
