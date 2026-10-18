package br.com.example.autogestao_backend.service;

import br.com.example.autogestao_backend.shared.exception.ErroApi;
import java.time.Clock;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class LimitePonto {
    private final Clock relogio;
    private final Map<String, Janela> janelas = new HashMap<>();

    public LimitePonto(Clock relogio) {
        this.relogio = relogio;
    }

    public synchronized void verificar(String endereco) {
        Instant agora = relogio.instant();
        janelas.entrySet().removeIf(entry -> !entry.getValue().fim().isAfter(agora));
        Janela janela = janelas.get(endereco);
        if (janela == null) {
            if (janelas.size() >= 10000) {
                throw new ErroApi(HttpStatus.TOO_MANY_REQUESTS, "Tente novamente em um minuto.");
            }
            janela = new Janela(agora.plusSeconds(60), 0);
        }
        if (janela.quantidade() >= 10) {
            throw new ErroApi(HttpStatus.TOO_MANY_REQUESTS, "Limite de tentativas atingido. Aguarde um minuto.");
        }
        janelas.put(endereco, new Janela(janela.fim(), janela.quantidade() + 1));
    }

    private record Janela(Instant fim, int quantidade) {}
}
