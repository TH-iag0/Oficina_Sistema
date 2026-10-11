param(
    [string]$JdkHome = $env:JAVA_HOME,
    [switch]$CheckConnectionOnly,
    [string]$PsqlPath = 'C:\Program Files\PostgreSQL\17\bin\psql.exe'
)
$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path $PSScriptRoot -Parent
if (-not $CheckConnectionOnly -and (-not $JdkHome -or -not (Test-Path -LiteralPath (Join-Path $JdkHome 'bin/java.exe')))) {
    throw 'Informe -JdkHome com o caminho do JDK 21.'
}
if ($CheckConnectionOnly -and -not (Test-Path -LiteralPath $PsqlPath)) {
    throw 'psql não encontrado. Informe -PsqlPath com o caminho correto.'
}
$previousPassword = $env:AUTOGESTAO_DB_PASSWORD
$previousPgPassword = $env:PGPASSWORD
$previousJavaHome = $env:JAVA_HOME
try {
    if (-not $env:AUTOGESTAO_DB_PASSWORD) {
        $securePassword = Read-Host 'Senha do usuário PostgreSQL (não será salva)' -AsSecureString
        $passwordPointer = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($securePassword)
        try { $env:AUTOGESTAO_DB_PASSWORD = [Runtime.InteropServices.Marshal]::PtrToStringBSTR($passwordPointer) }
        finally { [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($passwordPointer); $securePassword.Dispose() }
    }
    if (-not $env:AUTOGESTAO_DB_PASSWORD) { throw 'A senha não pode estar vazia.' }
    if ($CheckConnectionOnly) {
        $dbHost = if ($env:AUTOGESTAO_DB_HOST) { $env:AUTOGESTAO_DB_HOST } else { '127.0.0.1' }
        $dbPort = if ($env:AUTOGESTAO_DB_PORT) { $env:AUTOGESTAO_DB_PORT } else { '5433' }
        $dbName = if ($env:AUTOGESTAO_DB_NAME) { $env:AUTOGESTAO_DB_NAME } else { 'Oficina_Felipe' }
        $dbUser = if ($env:AUTOGESTAO_DB_USER) { $env:AUTOGESTAO_DB_USER } else { 'postgres' }
        $env:PGPASSWORD = $env:AUTOGESTAO_DB_PASSWORD
        & $PsqlPath -X -w -h $dbHost -p $dbPort -U $dbUser -d $dbName -c 'SELECT current_database(), current_user, version();'
        $commandExitCode = $LASTEXITCODE
    } else {
        $env:JAVA_HOME = $JdkHome
        Push-Location -LiteralPath $projectRoot
        try {
            & (Join-Path $projectRoot 'mvnw.cmd') spring-boot:run
            $commandExitCode = $LASTEXITCODE
        } finally { Pop-Location }
    }
} finally {
    $env:AUTOGESTAO_DB_PASSWORD = $previousPassword
    $env:PGPASSWORD = $previousPgPassword
    $env:JAVA_HOME = $previousJavaHome
}
exit $commandExitCode
