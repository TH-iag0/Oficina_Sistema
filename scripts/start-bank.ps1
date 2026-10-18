param([string]$JdkHome = $env:JAVA_HOME)
$ErrorActionPreference = 'Stop'
$names = @('PLUGGY_CLIENT_ID', 'PLUGGY_CLIENT_SECRET', 'PLUGGY_ITEM_ID')
$previous = @{}
foreach ($name in $names) { $previous[$name] = [Environment]::GetEnvironmentVariable($name, 'Process') }
try {
    foreach ($name in $names) {
        if ($previous[$name]) { continue }
        $secure = Read-Host "$name (não será salvo)" -AsSecureString
        $pointer = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($secure)
        try { [Environment]::SetEnvironmentVariable($name, [Runtime.InteropServices.Marshal]::PtrToStringBSTR($pointer), 'Process') }
        finally { [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($pointer); $secure.Dispose() }
        if (-not [Environment]::GetEnvironmentVariable($name, 'Process')) { throw "$name não pode estar vazio." }
    }
    & (Join-Path $PSScriptRoot 'start-backend.ps1') -JdkHome $JdkHome
} finally {
    foreach ($name in $names) { [Environment]::SetEnvironmentVariable($name, $previous[$name], 'Process') }
}
