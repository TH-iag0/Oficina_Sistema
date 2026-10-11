package br.com.example.autogestao_backend.model;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import static org.junit.jupiter.api.Assertions.*;

class FuncionarioTest {
    private static final BCryptPasswordEncoder ENCODER = new BCryptPasswordEncoder(4);
    private static final String HASH = ENCODER.encode("123456");

    @Test
    void cadastraFuncionarioAtivoComDadosNormalizadosEHashAutenticavel() {
        var funcionario = new Funcionario("  João Silva  ", "  OF-001  ", HASH);

        assertNotNull(funcionario.getId());
        assertEquals("João Silva", funcionario.getNome());
        assertEquals("OF-001", funcionario.getMatricula());
        assertTrue(funcionario.isAtivo());
        assertTrue(ENCODER.matches("123456", funcionario.pinHashParaAutenticacao()));
        assertFalse(ENCODER.matches("654321", funcionario.pinHashParaAutenticacao()));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    void rejeitaNomeOuMatriculaAusentes(String valor) {
        assertThrows(IllegalArgumentException.class, () -> new Funcionario(valor, "OF-001", HASH));
        assertThrows(IllegalArgumentException.class, () -> new Funcionario("João", valor, HASH));
    }

    @Test
    void rejeitaCamposAcimaDosLimitesSemAlterarNomeExistente() {
        assertThrows(IllegalArgumentException.class, () -> new Funcionario("a".repeat(121), "OF-001", HASH));
        assertThrows(IllegalArgumentException.class, () -> new Funcionario("João", "a".repeat(33), HASH));
        var funcionario = new Funcionario("João", "OF-001", HASH);
        assertThrows(IllegalArgumentException.class, () -> funcionario.alterarNome(" "));
        assertEquals("João", funcionario.getNome());
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"123456", "hash-invalido", "$2b$03$....................................................."})
    void rejeitaPinEmTextoPuroOuHashInvalido(String valor) {
        assertThrows(IllegalArgumentException.class, () -> new Funcionario("João", "OF-001", valor));
        var funcionario = new Funcionario("João", "OF-001", HASH);
        assertThrows(IllegalArgumentException.class, () -> funcionario.alterarPinHash(valor));
        assertEquals(HASH, funcionario.pinHashParaAutenticacao());
    }

    @Test
    void permiteAtualizarCadastroEPinSemTrocarIdentidade() {
        var funcionario = new Funcionario("João", "OF-001", HASH);
        var id = funcionario.getId();
        funcionario.desativar();
        assertFalse(funcionario.isAtivo());
        funcionario.ativar();
        funcionario.alterarNome("  João Souza  ");
        funcionario.alterarPinHash(ENCODER.encode("987654"));

        assertTrue(funcionario.isAtivo());
        assertEquals(id, funcionario.getId());
        assertEquals("OF-001", funcionario.getMatricula());
        assertEquals("João Souza", funcionario.getNome());
        assertTrue(ENCODER.matches("987654", funcionario.pinHashParaAutenticacao()));
        assertFalse(ENCODER.matches("123456", funcionario.pinHashParaAutenticacao()));
    }
}
