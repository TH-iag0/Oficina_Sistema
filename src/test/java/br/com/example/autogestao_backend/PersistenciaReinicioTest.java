package br.com.example.autogestao_backend;

import br.com.example.autogestao_backend.support.PersistenciaProcesso;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PersistenciaReinicioTest {
    @TempDir Path pasta;

    @Test void recuperaMesmosDadosFinanceirosECadastrosEmNovaJVM() throws Exception {
        String postgres = System.getenv("AUTOGESTAO_TEST_DB_URL");
        if (postgres != null && !postgres.matches("jdbc:postgresql://127\\.0\\.0\\.1:[0-9]+/autogestao_migration_test")) {
            throw new IllegalArgumentException("O teste de reinício exige banco local dedicado de testes.");
        }
        String url = postgres == null ? "jdbc:h2:file:" + pasta.resolve("persistencia").toString().replace('\\', '/')
            + ";MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_ON_EXIT=FALSE" : postgres;
        String schema = "reinicio_" + UUID.randomUUID().toString().replace("-", "");
        var opcoes = List.of("--server.address=127.0.0.1", "--server.port=0", "--spring.main.banner-mode=off", "--logging.level.root=ERROR",
            "--spring.datasource.url=" + url, "--spring.datasource.username=" + (postgres == null ? "sa" : "postgres"),
            "--spring.datasource.password=", "--spring.datasource.driver-class-name=" + (postgres == null ? "org.h2.Driver" : "org.postgresql.Driver"),
            "--spring.flyway.schemas=" + schema, "--spring.flyway.default-schema=" + schema,
            "--spring.jpa.properties.hibernate.default_schema=" + schema, "--spring.flyway.enabled=true", "--autogestao.fuso=America/Sao_Paulo");
        executar("gravar", opcoes);
        assertTrue(Files.exists(pasta.resolve("dados.json")), () -> lerLog(pasta.resolve("gravar.log")));
        executar("verificar", opcoes);
        assertTrue(Files.readString(pasta.resolve("verificar.log")).contains("PERSISTENCIA_VALIDADA_PID="),
            () -> lerLog(pasta.resolve("verificar.log")));
    }

    private void executar(String modo, List<String> opcoes) throws Exception {
        String java = Path.of(System.getProperty("java.home"), "bin", "java").toString();
        var comando = new ArrayList<>(List.of(java, "-Dspring.devtools.restart.enabled=false", "-cp", System.getProperty("surefire.test.class.path", System.getProperty("java.class.path")),
            PersistenciaProcesso.class.getName(), modo, pasta.resolve("dados.json").toString()));
        comando.addAll(opcoes);
        Path log = pasta.resolve(modo + ".log");
        var processo = new ProcessBuilder(comando).redirectErrorStream(true).redirectOutput(log.toFile()).start();
        try {
            assertTrue(processo.waitFor(120, TimeUnit.SECONDS), "Processo de persistência não encerrou: " + log);
            assertEquals(0, processo.exitValue(), () -> lerLog(log));
        } finally {
            if (processo.isAlive()) { processo.destroyForcibly(); processo.waitFor(10, TimeUnit.SECONDS); }
        }
    }

    private String lerLog(Path log) {
        try { return Files.readString(log); }
        catch (java.io.IOException erro) { return erro.toString(); }
    }
}
