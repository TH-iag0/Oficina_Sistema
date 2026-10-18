package br.com.example.autogestao_backend.banco;

import java.time.Duration;

public class FalhaPluggy extends RuntimeException {
    private final Duration espera;
    public FalhaPluggy(String mensagem, Duration espera) {
        super(mensagem);
        this.espera = espera;
    }
    public Duration getEspera() { return espera; }
}
