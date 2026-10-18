package br.com.example.autogestao_backend.service;

import br.com.example.autogestao_backend.model.Servico;
import br.com.example.autogestao_backend.repository.ClienteRepository;
import br.com.example.autogestao_backend.repository.ServicoRepository;
import br.com.example.autogestao_backend.repository.VeiculoRepository;
import br.com.example.autogestao_backend.shared.exception.ErroApi;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.doThrow;

class ExclusaoCadastroServiceTest {
    private final ServicoRepository servicos = mock(ServicoRepository.class);
    private final ExclusaoCadastroService service = new ExclusaoCadastroService(
        mock(ClienteRepository.class), mock(VeiculoRepository.class), servicos);

    @Test void excluiRegistroExistenteEConfirmaRestricoesNoBanco() {
        var id = UUID.randomUUID();
        var registro = mock(Servico.class);
        when(servicos.findById(id)).thenReturn(Optional.of(registro));
        service.excluirServico(id);
        verify(servicos).delete(registro);
        verify(servicos).flush();
    }

    @Test void registroAusenteNaoEhTratadoComoExcluido() {
        var requisicaoId = UUID.randomUUID();
        assertThrows(ErroApi.class, () -> service.excluirServico(requisicaoId));
    }

    @Test void vinculoImpedeExclusaoEPropagaConflitoParaRollback() {
        var id = UUID.randomUUID();
        when(servicos.findById(id)).thenReturn(Optional.of(mock(Servico.class)));
        doThrow(new DataIntegrityViolationException("FK")).when(servicos).flush();
        assertThrows(DataIntegrityViolationException.class, () -> service.excluirServico(id));
    }
}
