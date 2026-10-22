$ErrorActionPreference='Stop'
. "$PSScriptRoot/dev-tools.ps1"
$tools=Get-UPSelfTools
function Test-LocalPort([int]$Port) {
    $connection=New-Object Net.Sockets.TcpClient
    try { $connection.Connect('127.0.0.1',$Port);return $true } catch { return $false } finally { $connection.Dispose() }
}
if ((Test-LocalPort 3306) -and (Test-LocalPort 6379)) { Write-Host 'MySQL and Redis ports are available. Application startup will verify authentication.';return }
$serviceFile=Join-Path $tools.projectRoot '.local/service-tools.json'
if (Test-Path -LiteralPath $serviceFile) {
    $services=Get-Content -LiteralPath $serviceFile -Raw -Encoding UTF8 | ConvertFrom-Json
    if (-not (Test-LocalPort 3306)) {
        if (-not (Test-Path -LiteralPath (Join-Path $tools.projectRoot '.local/mysql-data/auto.cnf'))) { throw 'Initialize the new MySQL data directory first. See the startup guide.' }
        $myIni=Join-Path $tools.projectRoot '.local/my.ini'
        Start-Process -FilePath (Join-Path $services.mysqlHome 'bin/mysqld.exe') -ArgumentList @("--defaults-file=`"$myIni`"") -WindowStyle Hidden | Out-Null
    }
    if (-not (Test-LocalPort 6379)) {
        $redisConfig=Join-Path $tools.projectRoot '.local/redis.conf'
        Start-Process -FilePath (Join-Path $services.redisHome 'redis-server.exe') -ArgumentList @("`"$redisConfig`"") -WindowStyle Hidden | Out-Null
    }
    for ($attempt=0;$attempt -lt 20;$attempt++) {
        if ((Test-LocalPort 3306) -and (Test-LocalPort 6379)) { Write-Host 'Local MySQL and Redis started.';return }
        Start-Sleep -Milliseconds 250
    }
    throw 'A local service did not start. Inspect its .local data directory logs; no existing process was stopped.'
}
elseif ($tools.serviceHome) { & (Join-Path $tools.serviceHome 'scripts/start.ps1') }
else { throw 'Start installed MySQL 5.7.44 and Redis 5.0.14.1 on 127.0.0.1:3306/6379. See the cross-computer startup guide.' }
