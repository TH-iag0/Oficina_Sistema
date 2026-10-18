package br.com.example.autogestao_backend.service;

import br.com.example.autogestao_backend.shared.exception.ErroApi;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DocumentoClienteTest {
    @Test
    void aceitaCpfECnpjComDigitosValidos() {
        assertDoesNotThrow(() -> DocumentoCliente.validar("52998224725"));
        assertDoesNotThrow(() -> DocumentoCliente.validar("11222333000181"));
    }

    @Test
    void documentoOpcionalNaoBloqueiaCadastro() {
        assertDoesNotThrow(() -> DocumentoCliente.validar(null));
    }

    @Test
    void rejeitaDigitosVerificadoresInvalidos() {
        assertThrows(ErroApi.class, () -> DocumentoCliente.validar("52998224726"));
        assertThrows(ErroApi.class, () -> DocumentoCliente.validar("11222333000182"));
    }

    @Test
    void rejeitaSequenciasDeDigitosRepetidos() {
        assertThrows(ErroApi.class, () -> DocumentoCliente.validar("11111111111"));
        assertThrows(ErroApi.class, () -> DocumentoCliente.validar("00000000000000"));
    }
}
