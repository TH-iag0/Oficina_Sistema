package br.com.example.autogestao_backend.service;

import br.com.example.autogestao_backend.dto.OficinaDtos;
import br.com.example.autogestao_backend.model.UsuarioAdmin;
import br.com.example.autogestao_backend.repository.UsuarioAdminRepository;
import br.com.example.autogestao_backend.shared.exception.ErroApi;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AdminServiceTest {
    private final UsuarioAdminRepository repo = mock(UsuarioAdminRepository.class);
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(4);
    private final AdminService service = new AdminService(repo, encoder);
    @Test void senhaInicialNaoEhGravadaEmTexto() {
        when(repo.saveAndFlush(any())).thenAnswer(call -> {
            UsuarioAdmin u = call.getArgument(0);
            assertTrue(encoder.matches("senha-teste-123", u.hashParaAutenticacao()));
            return u;
        });
        service.inicializar("admin", "senha-teste-123");
    }

    @Test void reinicioNaoSobrescreveSenhaPersistida() {
        when(repo.count()).thenReturn(1L);
        service.inicializar("admin", "outra-senha-123");
        verify(repo, never()).saveAndFlush(any());
    }

    @Test void naoCriaSegundoUsuarioAoMudarConfiguracaoDeLogin() {
        when(repo.count()).thenReturn(1L);
        service.inicializar("outro-login", "senha-teste-123");
        verify(repo, never()).saveAndFlush(any());
    }

    @Test void semSenhaConfiguradaNaoCriaUsuario() {
        service.inicializar("proprietario", "");
        verify(repo, never()).saveAndFlush(any());
    }

    @Test void exigeSenhaAtualParaAlterar() {
        when(repo.bloquear("admin")).thenReturn(Optional.of(new UsuarioAdmin("admin", encoder.encode("senha-teste-123"))));
        var senhaEntrada = new OficinaDtos.SenhaEntrada("errada", "nova-senha-123");
        assertThrows(ErroApi.class, () -> service.alterarSenha("admin", senhaEntrada));
    }

    @Test void evitaTruncamentoSilenciosoDeSenhaMultibyte() {
        var texto = "á".repeat(40);
        assertThrows(ErroApi.class, () -> service.inicializar("admin", texto));
    }
}
