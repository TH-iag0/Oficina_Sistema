param(
    [string]$JdkHome = $env:JAVA_HOME,
    [string]$Repository = (Join-Path $env:USERPROFILE '.m2/repository')
)
$ErrorActionPreference = 'Stop'
if (-not $JdkHome) { $JdkHome = Join-Path $env:USERPROFILE '.jdks/ms-21.0.12.1' }
$javaExecutable = Join-Path $JdkHome 'bin/java.exe'
if (-not (Test-Path -LiteralPath $javaExecutable)) { throw 'Informe -JdkHome com o caminho do JDK 21.' }
$artifacts = @(
    'org/flywaydb/flyway-core/11.7.2/flyway-core-11.7.2.jar',
    'org/flywaydb/flyway-database-postgresql/11.7.2/flyway-database-postgresql-11.7.2.jar',
    'org/postgresql/postgresql/42.7.11/postgresql-42.7.11.jar',
    'com/fasterxml/jackson/core/jackson-annotations/2.21/jackson-annotations-2.21.jar',
    'com/fasterxml/jackson/core/jackson-core/2.21.4/jackson-core-2.21.4.jar',
    'com/fasterxml/jackson/core/jackson-databind/2.21.4/jackson-databind-2.21.4.jar',
    'com/fasterxml/jackson/dataformat/jackson-dataformat-toml/2.21.4/jackson-dataformat-toml-2.21.4.jar',
    'com/fasterxml/jackson/datatype/jackson-datatype-jsr310/2.21.4/jackson-datatype-jsr310-2.21.4.jar'
)
$jars = foreach ($artifact in $artifacts) {
    $jar = Join-Path $Repository $artifact
    if (-not (Test-Path -LiteralPath $jar)) { throw "Biblioteca local ausente: $artifact. Use a inicialização normal pelo Maven para instalar as dependências." }
    $jar
}
$projectRoot = Split-Path $PSScriptRoot -Parent
$previousPassword = $env:AUTOGESTAO_DB_PASSWORD
try {
    if (-not $env:AUTOGESTAO_DB_PASSWORD) {
        $securePassword = Read-Host 'Senha PostgreSQL (não será salva)' -AsSecureString
        $passwordPointer = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($securePassword)
        try { $env:AUTOGESTAO_DB_PASSWORD = [Runtime.InteropServices.Marshal]::PtrToStringBSTR($passwordPointer) }
        finally { [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($passwordPointer); $securePassword.Dispose() }
    }
    if (-not $env:AUTOGESTAO_DB_PASSWORD) { throw 'A senha não pode estar vazia.' }
    & $javaExecutable --class-path ($jars -join [IO.Path]::PathSeparator) (Join-Path $PSScriptRoot 'MigrateDatabase.java') (Join-Path $projectRoot 'src/main/resources/db/migration')
    $migrationExitCode = $LASTEXITCODE
} finally { $env:AUTOGESTAO_DB_PASSWORD = $previousPassword }
exit $migrationExitCode
