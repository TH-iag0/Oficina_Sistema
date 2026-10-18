param(
    [string]$Destination = (Join-Path $env:LOCALAPPDATA 'AutoGestao\backups'),
    [string]$Database = 'Oficina_Felipe',
    [string]$DbHost = '127.0.0.1',
    [ValidateRange(1,65535)][int]$Port = 5433,
    [string]$DbUser = 'postgres',
    [string]$PgBin = 'C:\Program Files\PostgreSQL\17\bin',
    [ValidateRange(1,3650)][int]$RetentionDays = 30,
    [ValidatePattern('^([01]\d|2[0-3]):[0-5]\d$')][string]$Time = '18:00'
)
$ErrorActionPreference = 'Stop'
if ($Database -notmatch '^[\p{L}\p{N}_-]+$') { throw 'Nome de banco invalido.' }
if (!(Test-Path -LiteralPath (Join-Path $PgBin 'pg_dump.exe'))) { throw 'pg_dump nao encontrado.' }
$directory = Join-Path $env:LOCALAPPDATA 'AutoGestao\backup'
$null = New-Item -ItemType Directory -Path $directory -Force
$configPath = Join-Path $directory 'config.json'
$secretPath = Join-Path $directory 'credential.xml'
$runner = Join-Path $directory 'backup-database.ps1'
$taskName = 'AutoGestao-Backup-' + $Database
if (Test-Path -LiteralPath $configPath) { throw 'Backup ja configurado. Preserve a configuracao existente antes de reconfigurar.' }
$password = Read-Host 'Senha do usuario PostgreSQL (protegida pelo Windows para este usuario)' -AsSecureString
try {
    $password | Export-Clixml -LiteralPath $secretPath
    Copy-Item -LiteralPath (Join-Path $PSScriptRoot 'backup-database.ps1') -Destination $runner -Force
    [ordered]@{ Host=$DbHost; Port=$Port; Database=$Database; User=$DbUser; PgBin=$PgBin;
        Destination=[IO.Path]::GetFullPath($Destination); RetentionDays=$RetentionDays; CredentialPath=$secretPath
    } | ConvertTo-Json | Set-Content -LiteralPath $configPath -Encoding UTF8
    # Valida a conexao e gera a primeira copia antes de instalar o agendamento.
    & $runner -ConfigPath $configPath
    $action = New-ScheduledTaskAction -Execute "$env:SystemRoot\System32\WindowsPowerShell\v1.0\powershell.exe" -Argument (
        '-NoProfile -NonInteractive -WindowStyle Hidden -ExecutionPolicy Bypass -File "' + $runner + '" -ConfigPath "' + $configPath + '"')
    $trigger = New-ScheduledTaskTrigger -Daily -At $Time
    $identity = [Security.Principal.WindowsIdentity]::GetCurrent().Name
    $principal = New-ScheduledTaskPrincipal -UserId $identity -LogonType Interactive -RunLevel Limited
    $settings = New-ScheduledTaskSettingsSet -StartWhenAvailable -MultipleInstances IgnoreNew -ExecutionTimeLimit (New-TimeSpan -Hours 1)
    Register-ScheduledTask -TaskName $taskName -Action $action -Trigger $trigger -Principal $principal -Settings $settings -Force | Out-Null
    Write-Output "Backup diario configurado para $Time. Retencao: $RetentionDays dias."
    Write-Output 'O computador precisa estar ligado e este usuario conectado ao Windows. A senha nao e armazenada em texto puro.'
    Write-Output ('Destino: ' + [IO.Path]::GetFullPath($Destination))
} catch {
    # Permite tentar novamente quando a validacao inicial falhar; nao apaga backups existentes.
    Remove-Item -LiteralPath $configPath -ErrorAction SilentlyContinue
    throw
} finally { $password.Dispose() }
