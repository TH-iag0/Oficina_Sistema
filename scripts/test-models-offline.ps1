param(
    [string]$JdkHome = $env:JAVA_HOME,
    [string]$Repository = (Join-Path $env:USERPROFILE '.m2/repository'),
    [switch]$IncludeRepositories
)
$ErrorActionPreference = 'Stop'
if (-not $JdkHome -or -not (Test-Path -LiteralPath (Join-Path $JdkHome 'bin/java.exe'))) {
    throw 'Informe -JdkHome com o caminho do JDK 21.'
}
$projectRoot = Split-Path $PSScriptRoot -Parent
$previousJavaHome = $env:JAVA_HOME
$testSelector = 'br.com.example.autogestao_backend.model.*Test,br.com.example.autogestao_backend.config.*Test'
if ($IncludeRepositories) {
    $testSelector += ',br.com.example.autogestao_backend.repository.*Test'
}
try {
    $env:JAVA_HOME = $JdkHome
    Push-Location -LiteralPath $projectRoot
    try {
        & (Join-Path $projectRoot 'mvnw.cmd') -o --batch-mode --no-transfer-progress "-Dmaven.repo.local=$Repository" "-Dtest=$testSelector" test
        $testExitCode = $LASTEXITCODE
    } finally {
        Pop-Location
    }
} finally {
    $env:JAVA_HOME = $previousJavaHome
}
exit $testExitCode
