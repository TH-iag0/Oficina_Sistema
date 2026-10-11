package br.com.example.autogestao_backend.config;

import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.config.YamlPropertiesFactoryBean;
import org.springframework.core.env.*;
import org.springframework.core.io.ClassPathResource;
import static org.junit.jupiter.api.Assertions.*;

class DatabaseConfigurationTest {
    private Properties configuracao() {
        var yaml = new YamlPropertiesFactoryBean();
        yaml.setResources(new ClassPathResource("application.yaml"));
        return Objects.requireNonNull(yaml.getObject());
    }
    private String resolver(String propriedade, Map<String,Object> ambiente) {
        var fontes = new MutablePropertySources();
        fontes.addFirst(new MapPropertySource("ambiente-teste", ambiente));
        return new PropertySourcesPropertyResolver(fontes)
            .resolveRequiredPlaceholders(configuracao().getProperty(propriedade));
    }
    @Test void apontaParaPostgreSql17EBancoAutorizadoPorPadrao() {
        assertEquals("jdbc:postgresql://127.0.0.1:5433/Oficina_Felipe?currentSchema=autogestao", resolver("spring.datasource.url", Map.of()));
        assertEquals("org.postgresql.Driver", configuracao().getProperty("spring.datasource.driver-class-name"));
        assertEquals("postgres", resolver("spring.datasource.username", Map.of()));
    }
    @Test void permitePersonalizarConexaoSemEditarCodigo() {
        assertEquals("jdbc:postgresql://localhost:5544/outro_banco?currentSchema=autogestao", resolver("spring.datasource.url", Map.of(
            "AUTOGESTAO_DB_HOST", "localhost", "AUTOGESTAO_DB_PORT", "5544", "AUTOGESTAO_DB_NAME", "outro_banco")));
        assertEquals("autogestao", resolver("spring.datasource.username", Map.of("AUTOGESTAO_DB_USER", "autogestao")));
    }
    @Test void exigeSenhaExternaSemCredencialPadraoNoArquivo() {
        assertEquals("${AUTOGESTAO_DB_PASSWORD}", configuracao().getProperty("spring.datasource.password"));
        assertThrows(IllegalArgumentException.class, () -> resolver("spring.datasource.password", Map.of()));
        assertEquals("senha-ficticia-do-teste", resolver("spring.datasource.password", Map.of("AUTOGESTAO_DB_PASSWORD", "senha-ficticia-do-teste")));
    }
    @Test void conexaoNaoAlteraEsquemaAutomaticamente() {
        assertEquals("validate", configuracao().getProperty("spring.jpa.hibernate.ddl-auto"));
        assertEquals("true", resolver("spring.flyway.enabled", Map.of()));
        assertEquals("false", resolver("spring.flyway.enabled", Map.of("AUTOGESTAO_FLYWAY_ENABLED", "false")));
        assertEquals("autogestao", configuracao().getProperty("spring.flyway.schemas"));
        assertEquals("false", configuracao().getProperty("spring.flyway.baseline-on-migrate"));
        assertEquals("true", configuracao().getProperty("spring.flyway.clean-disabled"));
    }
    @Test void usaUtcNaPersistenciaERestringeApiAoComputadorLocal() {
        assertEquals("UTC", configuracao().getProperty("spring.jpa.properties.hibernate.jdbc.time_zone"));
        assertEquals("false", configuracao().getProperty("spring.jpa.open-in-view"));
        assertEquals("127.0.0.1", configuracao().getProperty("server.address"));
    }
}
