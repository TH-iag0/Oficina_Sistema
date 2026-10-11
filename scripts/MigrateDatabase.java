import java.nio.file.Path;
import org.flywaydb.core.Flyway;

/** Executa somente migrations; a senha é recebida do ambiente do processo. */
public class MigrateDatabase {
    private static String ambiente(String nome, String padrao) {
        String valor = System.getenv(nome);
        return valor == null || valor.isBlank() ? padrao : valor;
    }
    public static void main(String[] args) {
        if (args.length != 1) throw new IllegalArgumentException("Informe o diretório de migrations.");
        String senha = System.getenv("AUTOGESTAO_DB_PASSWORD");
        if (senha == null || senha.isBlank()) throw new IllegalArgumentException("Senha do banco obrigatória.");
        String url = "jdbc:postgresql://" + ambiente("AUTOGESTAO_DB_HOST", "127.0.0.1") + ":"
            + ambiente("AUTOGESTAO_DB_PORT", "5433") + "/" + ambiente("AUTOGESTAO_DB_NAME", "Oficina_Felipe");
        var flyway = Flyway.configure()
            .dataSource(url, ambiente("AUTOGESTAO_DB_USER", "postgres"), senha)
            .schemas("autogestao").defaultSchema("autogestao")
            .locations("filesystem:" + Path.of(args[0]).toAbsolutePath().toString().replace('\\', '/'))
            .baselineOnMigrate(false).cleanDisabled(true).validateMigrationNaming(true)
            .load();
        var resultado = flyway.migrate();
        System.out.println("Migrations aplicadas: " + resultado.migrationsExecuted);
        System.out.println("Schema autogestao validado pelo Flyway.");
    }
}
