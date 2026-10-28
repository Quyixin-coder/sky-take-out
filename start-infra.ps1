param(
    [switch]$DependenciesOnly
)

$ErrorActionPreference = "Stop"

$ProjectRoot = $PSScriptRoot
$WorkspaceRoot = Split-Path -Parent (Split-Path -Parent $ProjectRoot)
$LocalRoot = Join-Path $WorkspaceRoot "work"
$LogRoot = Join-Path $LocalRoot "logs"
$MySqlBase = "D:\Program Files\MySQL\MySQL Server 8.0"
$MySqlIni = Join-Path $LocalRoot "mysql-local\my.ini"
$RedisRoot = Join-Path $LocalRoot "redis-local\Redis"
$NginxRoot = Join-Path $ProjectRoot "Web\nginx-1.20.2"
$JarPath = Join-Path $ProjectRoot "sky-server\target\sky-server-1.0-SNAPSHOT.jar"

New-Item -ItemType Directory -Path $LogRoot -Force | Out-Null

function Test-Port([int]$Port) {
    $Client = [System.Net.Sockets.TcpClient]::new()
    try {
        return $Client.ConnectAsync("127.0.0.1", $Port).Wait(500)
    }
    catch {
        return $false
    }
    finally {
        $Client.Dispose()
    }
}

function Wait-Port([int]$Port, [string]$Name) {
    for ($i = 0; $i -lt 30; $i++) {
        if (Test-Port $Port) {
            Write-Host "[OK] $Name is listening on port $Port" -ForegroundColor Green
            return
        }
        Start-Sleep -Seconds 1
    }
    throw "$Name did not start within 30 seconds. Check logs in $LogRoot."
}

if (-not (Test-Path -LiteralPath $MySqlIni)) {
    throw "Local MySQL config not found: $MySqlIni"
}
if (-not (Test-Path -LiteralPath (Join-Path $RedisRoot "redis-server.exe"))) {
    throw "Local Redis not found: $RedisRoot"
}

if (-not (Test-Port 3306)) {
    Start-Process `
        -FilePath (Join-Path $MySqlBase "bin\mysqld.exe") `
        -ArgumentList @("--defaults-file=$MySqlIni") `
        -WorkingDirectory (Split-Path -Parent $MySqlIni) `
        -WindowStyle Hidden `
        -RedirectStandardOutput (Join-Path $LogRoot "mysql.out.log") `
        -RedirectStandardError (Join-Path $LogRoot "mysql.err.log")
}
Wait-Port 3306 "MySQL"

if (-not (Test-Port 6379)) {
    Start-Process `
        -FilePath (Join-Path $RedisRoot "redis-server.exe") `
        -ArgumentList @((Join-Path $RedisRoot "redis.windows.conf")) `
        -WorkingDirectory $RedisRoot `
        -WindowStyle Hidden `
        -RedirectStandardOutput (Join-Path $LogRoot "redis.out.log") `
        -RedirectStandardError (Join-Path $LogRoot "redis.err.log")
}
Wait-Port 6379 "Redis"

New-Item -ItemType Directory -Path (Join-Path $NginxRoot "temp") -Force | Out-Null
if (-not (Get-Process nginx -ErrorAction SilentlyContinue | Where-Object { $_.Path -like "$NginxRoot*" })) {
    Start-Process -FilePath (Join-Path $NginxRoot "nginx.exe") -WorkingDirectory $NginxRoot -WindowStyle Hidden
}
Wait-Port 80 "Nginx"

if (-not $DependenciesOnly) {
    if (-not (Test-Path -LiteralPath $JarPath)) {
        Write-Host "First run: compiling the backend..." -ForegroundColor Yellow
        Push-Location $ProjectRoot
        try {
            & mvn -DskipTests package
            if ($LASTEXITCODE -ne 0) {
                throw "Maven build failed. Check the output above."
            }
        }
        finally {
            Pop-Location
        }
    }

    if (-not (Test-Port 8080)) {
        Start-Process `
            -FilePath "java.exe" `
            -ArgumentList @("-jar", "`"$JarPath`"") `
            -WorkingDirectory $ProjectRoot `
            -WindowStyle Hidden `
            -RedirectStandardOutput (Join-Path $LogRoot "backend.out.log") `
            -RedirectStandardError (Join-Path $LogRoot "backend.err.log")
    }
    Wait-Port 8080 "Spring Boot backend"
}

Write-Host ""
if ($DependenciesOnly) {
    Write-Host "Infrastructure is ready. Start SkyApplication from your IDE." -ForegroundColor Cyan
}
else {
    Write-Host "The project is ready." -ForegroundColor Cyan
}
Write-Host "Admin URL: http://localhost/"
Write-Host "Username: admin"
Write-Host "Password: 123456"
Write-Host "API docs: http://localhost:8080/doc.html"
Write-Host "Run the stop script when you are finished."
