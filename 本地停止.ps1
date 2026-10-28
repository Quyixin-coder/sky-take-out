param(
    [switch]$KeepBackend
)

$ErrorActionPreference = "Continue"

$ProjectRoot = $PSScriptRoot
$WorkspaceRoot = Split-Path -Parent (Split-Path -Parent $ProjectRoot)
$LocalRoot = Join-Path $WorkspaceRoot "work"
$RedisRoot = Join-Path $LocalRoot "redis-local\Redis"
$NginxRoot = Join-Path $ProjectRoot "Web\nginx-1.20.2"

$NginxProcesses = Get-Process nginx -ErrorAction SilentlyContinue |
    Where-Object { $_.Path -like "$NginxRoot*" }
if ($NginxProcesses) {
    Push-Location $NginxRoot
    try {
        & ".\nginx.exe" -s quit
    }
    finally {
        Pop-Location
    }
    Write-Host "[OK] Nginx stopped"
}

if (-not $KeepBackend) {
    $BackendProcesses = Get-CimInstance Win32_Process |
        Where-Object { $_.Name -eq "java.exe" -and $_.CommandLine -like "*sky-server-1.0-SNAPSHOT.jar*" }
    foreach ($Process in $BackendProcesses) {
        Stop-Process -Id $Process.ProcessId -Force -ErrorAction SilentlyContinue
        Write-Host "[OK] Spring Boot backend stopped"
    }
}

$RedisExecutable = Join-Path $RedisRoot "redis-server.exe"
if (Get-Process redis-server -ErrorAction SilentlyContinue | Where-Object { $_.Path -eq $RedisExecutable }) {
    & (Join-Path $RedisRoot "redis-cli.exe") -h 127.0.0.1 -p 6379 shutdown
    Write-Host "[OK] Redis stopped"
}

$MySqlRuntime = Get-CimInstance Win32_Process |
    Where-Object { $_.Name -eq "mysqld.exe" -and $_.CommandLine -like "*mysql-local\my.ini*" }
if ($MySqlRuntime) {
    & "D:\Program Files\MySQL\MySQL Server 8.0\bin\mysqladmin.exe" `
        -u root --protocol=TCP -h 127.0.0.1 -P 3306 shutdown
    Write-Host "[OK] Local MySQL stopped"
}
