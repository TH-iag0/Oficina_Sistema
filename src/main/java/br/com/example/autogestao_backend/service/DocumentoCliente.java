package br.com.example.autogestao_backend.service;

import br.com.example.autogestao_backend.shared.exception.ErroApi;
import org.springframework.http.HttpStatus;

final class DocumentoCliente {
    private DocumentoCliente() {}

    static void validar(String documento) {
        if (documento == null) {
            return;
        }
        int tamanhoBase = documento.length() - 2;
        if (documento.chars().distinct().count() == 1
            || digito(documento, tamanhoBase) != documento.charAt(tamanhoBase) - '0'
            || digito(documento, tamanhoBase + 1) != documento.charAt(tamanhoBase + 1) - '0') {
            throw new ErroApi(HttpStatus.BAD_REQUEST, "CPF ou CNPJ inválido.");
        }
    }

    private static int digito(String documento, int tamanho) {
        int soma = 0;
        for (int i = 0; i < tamanho; i++) {
            int peso = documento.length() == 11 ? tamanho + 1 - i : (tamanho - i - 1) % 8 + 2;
            soma += (documento.charAt(i) - '0') * peso;
        }
        int resto = soma % 11;
        return resto < 2 ? 0 : 11 - resto;
    }
}
