param(
    [string]$JdkHome = $env:JAVA_HOME,
    [string]$Repository = (Join-Path $env:USERPROFILE '.m2/repository'),
    [switch]$IncludeRepositories
)
$ErrorActionPreference = 'Stop'
if (-not $JdkHome) { throw 'Informe -JdkHome com o caminho do JDK 21.' }
$projectRoot = Split-Path $PSScriptRoot -Parent
$outputDir = Join-Path $projectRoot 'target/model-tests-offline'
New-Item -ItemType Directory -Path $outputDir -Force | Out-Null

# Fallback com JUnit 5.12.2 instalado: não substitui o build Maven com JUnit 6.
$artifacts = @(
    'jakarta/persistence/jakarta.persistence-api/3.2.0/jakarta.persistence-api-3.2.0.jar',
    'jakarta/validation/jakarta.validation-api/3.1.1/jakarta.validation-api-3.1.1.jar',
    'org/hibernate/orm/hibernate-core/7.4.5.Final/hibernate-core-7.4.5.Final.jar',
    'org/springframework/security/spring-security-crypto/7.1.1/spring-security-crypto-7.1.1.jar',
    'org/springframework/spring-core/7.0.9/spring-core-7.0.9.jar',
    'org/springframework/spring-beans/7.0.9/spring-beans-7.0.9.jar',
    'org/yaml/snakeyaml/2.6/snakeyaml-2.6.jar',
    'commons-logging/commons-logging/1.3.6/commons-logging-1.3.6.jar',
    'org/junit/jupiter/junit-jupiter-api/5.12.2/junit-jupiter-api-5.12.2.jar',
    'org/junit/jupiter/junit-jupiter-params/5.12.2/junit-jupiter-params-5.12.2.jar',
    'org/junit/jupiter/junit-jupiter-engine/5.12.2/junit-jupiter-engine-5.12.2.jar',
    'org/junit/platform/junit-platform-commons/1.12.2/junit-platform-commons-1.12.2.jar',
    'org/junit/platform/junit-platform-engine/1.12.2/junit-platform-engine-1.12.2.jar',
    'org/junit/platform/junit-platform-launcher/1.12.2/junit-platform-launcher-1.12.2.jar',
    'org/opentest4j/opentest4j/1.3.0/opentest4j-1.3.0.jar',
    'org/apiguardian/apiguardian-api/1.1.2/apiguardian-api-1.1.2.jar'
)
if ($IncludeRepositories) {
    $artifacts += @(
        'org/springframework/data/spring-data-jpa/4.1.1/spring-data-jpa-4.1.1.jar',
        'org/springframework/data/spring-data-commons/4.1.1/spring-data-commons-4.1.1.jar',
        'org/hibernate/models/hibernate-models/1.1.1/hibernate-models-1.1.1.jar',
        'org/jboss/logging/jboss-logging/3.6.3.Final/jboss-logging-3.6.3.Final.jar',
        'net/bytebuddy/byte-buddy/1.18.11/byte-buddy-1.18.11.jar',
        'jakarta/transaction/jakarta.transaction-api/2.0.1/jakarta.transaction-api-2.0.1.jar',
        'jakarta/xml/bind/jakarta.xml.bind-api/4.0.5/jakarta.xml.bind-api-4.0.5.jar',
        'org/glassfish/jaxb/jaxb-runtime/4.0.9/jaxb-runtime-4.0.9.jar',
        'org/glassfish/jaxb/jaxb-core/4.0.9/jaxb-core-4.0.9.jar',
        'org/glassfish/jaxb/txw2/4.0.9/txw2-4.0.9.jar',
        'jakarta/inject/jakarta.inject-api/2.0.1/jakarta.inject-api-2.0.1.jar',
        'org/antlr/antlr4-runtime/4.13.2/antlr4-runtime-4.13.2.jar',
        'org/jspecify/jspecify/1.0.1/jspecify-1.0.1.jar',
        'org/slf4j/slf4j-api/2.0.18/slf4j-api-2.0.18.jar',
        'com/h2database/h2/2.3.232/h2-2.3.232.jar'
    )
    $artifacts += @(
        'org/flywaydb/flyway-core/11.7.2/flyway-core-11.7.2.jar',
        'org/flywaydb/flyway-database-postgresql/11.7.2/flyway-database-postgresql-11.7.2.jar',
        'org/postgresql/postgresql/42.7.11/postgresql-42.7.11.jar',
        'com/fasterxml/jackson/core/jackson-annotations/2.21/jackson-annotations-2.21.jar',
        'com/fasterxml/jackson/core/jackson-core/2.21.4/jackson-core-2.21.4.jar',
        'com/fasterxml/jackson/core/jackson-databind/2.21.4/jackson-databind-2.21.4.jar',
        'com/fasterxml/jackson/dataformat/jackson-dataformat-toml/2.21.4/jackson-dataformat-toml-2.21.4.jar',
        'com/fasterxml/jackson/datatype/jackson-datatype-jsr310/2.21.4/jackson-datatype-jsr310-2.21.4.jar'
    )
    foreach ($module in @('spring-aop','spring-expression','spring-context','spring-tx','spring-orm','spring-jdbc')) {
        $artifacts += "org/springframework/$module/7.0.9/$module-7.0.9.jar"
    }
}
$jars = foreach ($artifact in $artifacts) {
    $jar = Join-Path $Repository $artifact
    if (-not (Test-Path -LiteralPath $jar)) { throw "Biblioteca local ausente: $artifact" }
    $jar
}
$classpath = $jars -join [IO.Path]::PathSeparator
$runner = @'
import java.io.PrintWriter;
import org.junit.platform.engine.discovery.DiscoverySelectors;
import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder;
import org.junit.platform.launcher.core.LauncherFactory;
import org.junit.platform.launcher.listeners.SummaryGeneratingListener;
public class OfflineModelTestRunner {
    public static void main(String[] args) {
        var listener = new SummaryGeneratingListener();
        var builder = LauncherDiscoveryRequestBuilder.request()
            .selectors(DiscoverySelectors.selectPackage("br.com.example.autogestao_backend.model"),
                DiscoverySelectors.selectPackage("br.com.example.autogestao_backend.config"));
        if (args.length > 0) builder.selectors(DiscoverySelectors.selectPackage("br.com.example.autogestao_backend.repository"));
        LauncherFactory.create().execute(builder.build(), listener);
        var summary = listener.getSummary();
        summary.printTo(new PrintWriter(System.out, true));
        summary.printFailuresTo(new PrintWriter(System.out, true));
        if (summary.getTestsFoundCount() == 0 || summary.getTestsFailedCount() > 0
            || summary.getContainersFailedCount() > 0) System.exit(1);
    }
}
'@
$runnerFile = Join-Path $outputDir 'OfflineModelTestRunner.java'
Set-Content -LiteralPath $runnerFile -Value $runner -Encoding utf8
$mainSources = Get-ChildItem -LiteralPath (Join-Path $projectRoot 'src/main/java/br/com/example/autogestao_backend/model') -Filter '*.java'
$testSources = Get-ChildItem -LiteralPath (Join-Path $projectRoot 'src/test/java/br/com/example/autogestao_backend/model') -Filter '*.java'
$sources = @($mainSources.FullName) + @($testSources.FullName) + @($runnerFile)
$configTests = Get-ChildItem -LiteralPath (Join-Path $projectRoot 'src/test/java/br/com/example/autogestao_backend/config') -Filter '*.java'
$sources += @($configTests.FullName)
Copy-Item -LiteralPath (Join-Path $projectRoot 'src/main/resources/application.yaml') -Destination (Join-Path $outputDir 'application.yaml') -Force
New-Item -ItemType Directory -Path (Join-Path $outputDir 'db/migration') -Force | Out-Null
Get-ChildItem -LiteralPath (Join-Path $projectRoot 'src/main/resources/db/migration') -Filter '*.sql' | Copy-Item -Destination (Join-Path $outputDir 'db/migration') -Force
if ($IncludeRepositories) {
    $repoSources = Get-ChildItem -LiteralPath (Join-Path $projectRoot 'src/main/java/br/com/example/autogestao_backend/repository') -Filter '*.java'
    $repoTests = Get-ChildItem -LiteralPath (Join-Path $projectRoot 'src/test/java/br/com/example/autogestao_backend/repository') -Filter '*.java'
    $sources += @($repoSources.FullName) + @($repoTests.FullName)
}
& (Join-Path $JdkHome 'bin/javac.exe') --release 21 -encoding UTF-8 -cp $classpath -d $outputDir @sources
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
$runnerArgs = @()
if ($IncludeRepositories) { $runnerArgs += 'repositories' }
& (Join-Path $JdkHome 'bin/java.exe') -cp ($outputDir + [IO.Path]::PathSeparator + $classpath) OfflineModelTestRunner @runnerArgs
exit $LASTEXITCODE
