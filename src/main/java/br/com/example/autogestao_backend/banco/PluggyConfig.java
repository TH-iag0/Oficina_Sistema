package br.com.example.autogestao_backend.banco;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
@EnableScheduling
public class PluggyConfig {
    final String clientId;
    final String clientSecret;
    final String itemId;

    public PluggyConfig(@Value("${PLUGGY_CLIENT_ID:}") String clientId,
        @Value("${PLUGGY_CLIENT_SECRET:}") String clientSecret,
        @Value("${PLUGGY_ITEM_ID:}") String itemId) {
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.itemId = itemId;
    }

    public boolean configurado() {
        return !clientId.isBlank() && !clientSecret.isBlank() && !itemId.isBlank();
    }
}
