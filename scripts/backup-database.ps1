param([string]$ConfigPath = (Join-Path $env:LOCALAPPDATA 'AutoGestao\backup\config.json'))
$ErrorActionPreference = 'Stop'
$config = Get-Content -LiteralPath $ConfigPath -Raw | ConvertFrom-Json
if ($config.Database -notmatch '^[\p{L}\p{N}_-]+$') { throw 'Nome de banco invalido.' }
if ([int]$config.Port -lt 1 -or [int]$config.Port -gt 65535) { throw 'Porta invalida.' }
if ([int]$config.RetentionDays -lt 1 -or [int]$config.RetentionDays -gt 3650) { throw 'Retencao invalida.' }
$pgDump = Join-Path $config.PgBin 'pg_dump.exe'
$pgRestore = Join-Path $config.PgBin 'pg_restore.exe'
if (!(Test-Path -LiteralPath $pgDump) -or !(Test-Path -LiteralPath $pgRestore)) { throw 'Ferramentas PostgreSQL nao encontradas.' }
$destination = [IO.Path]::GetFullPath($config.Destination)
$null = New-Item -ItemType Directory -Path $destination -Force
$prefix = 'autogestao_' + $config.Database + '_'
$name = $prefix + [DateTime]::UtcNow.ToString('yyyyMMdd_HHmmss') + '_' + [guid]::NewGuid().ToString('N') + '.dump'
$final = Join-Path $destination $name
$partial = $final + '.partial'
$previousPassword = $env:PGPASSWORD
$pointer = [IntPtr]::Zero
try {
    $secret = Import-Clixml -LiteralPath $config.CredentialPath
    if ($secret -isnot [Security.SecureString]) { throw 'Credencial de backup invalida.' }
    $pointer = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($secret)
    $env:PGPASSWORD = [Runtime.InteropServices.Marshal]::PtrToStringBSTR($pointer)
    & $pgDump --no-password --host $config.Host --port $config.Port --username $config.User --dbname $config.Database --format=custom --file $partial
    if ($LASTEXITCODE -ne 0) { throw 'Falha ao gerar backup. As copias anteriores foram preservadas.' }
    & $pgRestore --list $partial | Out-Null
    if ($LASTEXITCODE -ne 0) { throw 'Arquivo de backup invalido. As copias anteriores foram preservadas.' }
    Move-Item -LiteralPath $partial -Destination $final
    $cutoff = [DateTime]::UtcNow.AddDays(-[int]$config.RetentionDays)
    Get-ChildItem -LiteralPath $destination -File | Where-Object {
        $_.Name.StartsWith($prefix, [StringComparison]::Ordinal) -and $_.Extension -eq '.dump' -and $_.LastWriteTimeUtc -lt $cutoff
    } | ForEach-Object { Remove-Item -LiteralPath $_.FullName }
    Write-Output ('Backup concluido: ' + $final)
} finally {
    $env:PGPASSWORD = $previousPassword
    if ($pointer -ne [IntPtr]::Zero) { [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($pointer) }
    if ($secret -is [Security.SecureString]) { $secret.Dispose() }
    if (Test-Path -LiteralPath $partial) { Remove-Item -LiteralPath $partial }
}
