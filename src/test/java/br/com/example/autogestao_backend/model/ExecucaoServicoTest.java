package br.com.example.autogestao_backend.model;

import java.time.Duration;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static br.com.example.autogestao_backend.model.ModelFixtures.*;

class ExecucaoServicoTest {
    @Test void medeTempoEfetivoSemUsarHorasDePresenca() {
        var os = os(); var item = item(os, CategoriaServico.MANUTENCAO); iniciar(os);
        var execucao = new ExecucaoServico(item, funcionario(), RELOGIO);
        assertNull(execucao.getDuracao());
        assertThrows(IllegalArgumentException.class, () -> execucao.encerrar(depois(-1)));
        assertNull(execucao.getFim());
        execucao.encerrar(depois(5400));
        assertEquals(Duration.ofMinutes(90), execucao.getDuracao());
        assertThrows(IllegalStateException.class, () -> execucao.encerrar(depois(5401)));
    }
    @Test void impedeExecutarOsNaoAprovadaFuncionarioInativoOuServicoConcluido() {
        var os = os(); var item = item(os, CategoriaServico.MANUTENCAO); var funcionario = funcionario();
        assertThrows(IllegalStateException.class, () -> new ExecucaoServico(item, funcionario, RELOGIO));
        iniciar(os); funcionario.desativar();
        assertThrows(IllegalStateException.class, () -> new ExecucaoServico(item, funcionario, RELOGIO));
        funcionario.ativar(); item.concluir(RELOGIO);
        assertThrows(IllegalStateException.class, () -> new ExecucaoServico(item, funcionario, RELOGIO));
    }
    @Test void impedePeriodoPosteriorAConclusaoDoServico() {
        var os = os(); var item = item(os, CategoriaServico.MANUTENCAO); iniciar(os);
        var execucao = new ExecucaoServico(item, funcionario(), RELOGIO);
        item.concluir(depois(100));
        assertThrows(IllegalStateException.class, () -> execucao.encerrar(depois(101)));
        assertNull(execucao.getFim());
    }
}
