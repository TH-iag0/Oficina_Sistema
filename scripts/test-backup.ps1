param([ValidateSet(55439)][int]$Port = 55439)
$ErrorActionPreference = 'Stop'
$pgBin = 'C:\Program Files\PostgreSQL\17\bin'
$source = 'autogestao_backup_source_test'
$restore = 'autogestao_backup_restore_test'
$directory = Join-Path $env:TEMP ('autogestao-backup-test-' + [guid]::NewGuid().ToString('N'))
$null = New-Item -ItemType Directory -Path $directory
$configPath = Join-Path $directory 'config.json'
$credential = Join-Path $directory 'credential.xml'
$destination = Join-Path $directory 'copies'
$null = New-Item -ItemType Directory -Path $destination
try {
    & "$pgBin\createdb.exe" -h 127.0.0.1 -p $Port -U postgres $source
    if ($LASTEXITCODE -ne 0) { throw 'Falha ao criar banco isolado.' }
    & "$pgBin\createdb.exe" -h 127.0.0.1 -p $Port -U postgres $restore
    if ($LASTEXITCODE -ne 0) { throw 'Falha ao criar banco de restauracao.' }
    & "$pgBin\psql.exe" -X -v ON_ERROR_STOP=1 -h 127.0.0.1 -p $Port -U postgres -d $source -c "CREATE TABLE prova (id integer primary key, valor numeric(15,2)); INSERT INTO prova VALUES (1,123.45)" | Out-Null
    if ($LASTEXITCODE -ne 0) { throw 'Falha nos dados de teste.' }
    $password = [guid]::NewGuid().ToString('N')
    $secure = ConvertTo-SecureString $password -AsPlainText -Force
    $secure | Export-Clixml -LiteralPath $credential
    $secure.Dispose()
    if ((Get-Content -LiteralPath $credential -Raw).Contains($password)) { throw 'Senha em texto puro.' }
    $config = @{ Host='127.0.0.1'; Port=$Port; Database=$source; User='postgres'; PgBin=$pgBin;
        Destination=$destination; RetentionDays=30; CredentialPath=$credential }
    $config | ConvertTo-Json | Set-Content -LiteralPath $configPath -Encoding UTF8
    $old = Join-Path $destination ('autogestao_' + $source + '_antigo.dump')
    $unrelated = Join-Path $destination 'outro_arquivo.dump'
    foreach ($file in @($old, $unrelated)) { Set-Content -LiteralPath $file -Value 'preservar'; (Get-Item -LiteralPath $file).LastWriteTimeUtc = [DateTime]::UtcNow.AddDays(-40) }
    & (Join-Path $PSScriptRoot 'backup-database.ps1') -ConfigPath $configPath
    $copies = @(Get-ChildItem -LiteralPath $destination -Filter ('autogestao_' + $source + '_*.dump'))
    if ($copies.Count -ne 1 -or (Test-Path -LiteralPath $old) -or !(Test-Path -LiteralPath $unrelated)) { throw 'Falha na retencao seletiva.' }
    & "$pgBin\pg_restore.exe" --no-password --exit-on-error --no-owner --no-acl -h 127.0.0.1 -p $Port -U postgres -d $restore $copies[0].FullName
    if ($LASTEXITCODE -ne 0) { throw 'Falha na restauracao.' }
    $value = & "$pgBin\psql.exe" -X -At -h 127.0.0.1 -p $Port -U postgres -d $restore -c 'SELECT valor FROM prova WHERE id=1'
    if ($LASTEXITCODE -ne 0 -or $value.Trim() -ne '123.45') { throw 'Dados restaurados diferentes.' }
    $config.Database = 'autogestao_backup_inexistente_test'
    $config | ConvertTo-Json | Set-Content -LiteralPath $configPath -Encoding UTF8
    $failed = $false
    try { & (Join-Path $PSScriptRoot 'backup-database.ps1') -ConfigPath $configPath }
    catch { $failed = $true }
    if (!$failed -or !(Test-Path -LiteralPath $copies[0].FullName) -or @(Get-ChildItem -LiteralPath $destination -Filter '*.partial').Count) { throw 'Falha ao preservar copias em erro.' }
    Write-Output 'BACKUP: geracao, restauracao, dados, retencao seletiva, credencial protegida e preservacao em erro validados.'
} finally {
    & "$pgBin\dropdb.exe" --if-exists -h 127.0.0.1 -p $Port -U postgres $source
    & "$pgBin\dropdb.exe" --if-exists -h 127.0.0.1 -p $Port -U postgres $restore
    $resolved = [IO.Path]::GetFullPath($directory)
    $parent = [IO.Path]::GetFullPath($env:TEMP) + [IO.Path]::DirectorySeparatorChar
    if ($resolved.StartsWith($parent) -and [IO.Path]::GetFileName($resolved).StartsWith('autogestao-backup-test-')) { Remove-Item -LiteralPath $resolved -Recurse -Force }
}
