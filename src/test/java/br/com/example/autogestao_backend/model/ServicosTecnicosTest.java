package br.com.example.autogestao_backend.model;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import static org.junit.jupiter.api.Assertions.*;
import static br.com.example.autogestao_backend.model.ModelFixtures.*;

class ServicosTecnicosTest {
    @ParameterizedTest @EnumSource(value = CategoriaServico.class, names = {"REMAP_ECU", "REMAP_TCU"})
    void remapAceitaTipoConfiguravelERegistraAlteracoesEfetivas(CategoriaServico categoria) {
        var os = os(); var item = item(os, categoria);
        var remap = new Remap(item, "Perfil personalizado", "Bosch", "EDC17", "Melhorar dirigibilidade");
        remap.identificarVersoes("HW01", "SW02", "CAL03");
        assertEquals("Perfil personalizado", remap.getTipoConfigurado());
        assertEquals("SW02", remap.getSoftware());
        assertThrows(IllegalStateException.class, () -> remap.registrarAlteracoes("Ajuste de calibração"));
        iniciar(os); remap.registrarAlteracoes("Ajuste de calibração");
        assertEquals("Ajuste de calibração", remap.getAlteracoesRealizadas());
        item.concluir(RELOGIO);
        assertThrows(IllegalStateException.class, () -> remap.identificarVersoes("outro", null, null));
    }
    @Test void rejeitaModelTecnicoAssociadoACategoriaIncorreta() {
        var item = item(os(), CategoriaServico.MANUTENCAO);
        assertThrows(IllegalArgumentException.class, () -> new Remap(item, "Stage 1", "Bosch", "EDC17", "Objetivo"));
        assertThrows(IllegalArgumentException.class, () -> new Diagnostico(item, "Sintomas"));
    }
    @Test void diagnosticoPreservaResultadoEValidaReparoDoMesmoVeiculo() {
        var os = os(); var diagnostico = new Diagnostico(item(os, CategoriaServico.DIAGNOSTICO), "Falha intermitente");
        var reparo = item(os, CategoriaServico.MANUTENCAO);
        diagnostico.registrarResultado("Scanner", "P0300", "Teste de ignição", "Falha de ignição");
        assertEquals("P0300", diagnostico.getDtcs()); assertEquals("Falha de ignição", diagnostico.getCausaIdentificada());
        assertThrows(IllegalArgumentException.class, () -> diagnostico.vincularReparoAprovado(reparo));
        aprovar(os); diagnostico.vincularReparoAprovado(reparo);
        assertSame(reparo, diagnostico.getReparoAprovado());
        assertThrows(IllegalStateException.class, () -> diagnostico.vincularReparoAprovado(reparo));
        var outra = os(); item(outra, CategoriaServico.DIAGNOSTICO); var reparoOutro = item(outra, CategoriaServico.MANUTENCAO); aprovar(outra);
        var novo = new Diagnostico(item(os(), CategoriaServico.DIAGNOSTICO), "Sintoma");
        assertThrows(IllegalArgumentException.class, () -> novo.vincularReparoAprovado(reparoOutro));
    }
    @Test void diagnosticoInvalidoNaoSobrescreveResultadoAnteriorENaoEditaServicoConcluido() {
        var os = os(); var item = item(os, CategoriaServico.DIAGNOSTICO); var diagnostico = new Diagnostico(item, "Sintoma");
        diagnostico.registrarResultado("Scanner", "P0300", "Teste", "Causa");
        assertThrows(IllegalArgumentException.class, () -> diagnostico.registrarResultado("Outro", null, " ", null));
        assertEquals("Scanner", diagnostico.getEquipamento()); assertEquals("Causa", diagnostico.getCausaIdentificada());
        iniciar(os); item.concluir(RELOGIO);
        assertThrows(IllegalStateException.class, () -> diagnostico.registrarResultado("Outro", null, "Teste", null));
    }
}
