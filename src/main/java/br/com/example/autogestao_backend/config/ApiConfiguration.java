package br.com.example.autogestao_backend.config;

import java.time.Clock;
import java.time.ZoneId;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ApiConfiguration {
    @Bean
    public ZoneId fusoOficina(@Value("${autogestao.fuso:America/Sao_Paulo}") String fuso) {
        return ZoneId.of(fuso);
    }

    @Bean
    public Clock relogio(ZoneId fusoOficina) {
        return Clock.system(fusoOficina);
    }

}
