package br.com.example.autogestao_backend.shared.exception;

import org.springframework.http.HttpStatus;

public class ErroApi extends RuntimeException {
    private final HttpStatus status;

    public ErroApi(HttpStatus status, String mensagem) {
        super(mensagem);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
