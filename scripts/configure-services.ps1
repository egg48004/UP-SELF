param([Parameter(Mandatory=$true)][string]$MySqlHome,[Parameter(Mandatory=$true)][string]$RedisHome)
$ErrorActionPreference='Stop'
$project=Split-Path -Parent $PSScriptRoot
$MySqlHome=(Resolve-Path -LiteralPath $MySqlHome).Path
$RedisHome=(Resolve-Path -LiteralPath $RedisHome).Path
if (-not (Test-Path -LiteralPath (Join-Path $MySqlHome 'bin/mysqld.exe'))) { throw 'MySQL server executable not found.' }
if (-not (Test-Path -LiteralPath (Join-Path $RedisHome 'redis-server.exe'))) { throw 'Redis server executable not found.' }
. "$PSScriptRoot/dev-tools.ps1"
$mysqlVersion=Invoke-UPSelfNative -Executable (Join-Path $MySqlHome 'bin/mysqld.exe') -Arguments @('--version')
$redisVersion=Invoke-UPSelfNative -Executable (Join-Path $RedisHome 'redis-server.exe') -Arguments @('--version')
if ($mysqlVersion -notmatch '5\.7\.44\b' -or $redisVersion -notmatch 'v=5\.0\.14\.1\b') { throw 'Expected the established MySQL 5.7.44 / Redis Windows 5.0.14.1 service versions.' }
$local=Join-Path $project '.local'
$mysqlData=Join-Path $local 'mysql-data'
$redisData=Join-Path $local 'redis-data'
if ((Test-Path -LiteralPath (Join-Path $local 'service-tools.json')) -or (Test-Path -LiteralPath $mysqlData)) { throw 'Local service configuration/data already exists. It was not overwritten.' }
New-Item -ItemType Directory -Path $local -Force | Out-Null
New-Item -ItemType Directory -Path $redisData -Force | Out-Null
$credentials=Get-Content -LiteralPath (Join-Path $project 'migration/local-snapshot/local-credentials.json') -Raw -Encoding UTF8 | ConvertFrom-Json
$mysqlBase=$MySqlHome.Replace('\','/')
$mysqlDataPath=$mysqlData.Replace('\','/')
$mysqlConfig="[mysqld]`nbasedir=`"$mysqlBase`"`ndatadir=`"$mysqlDataPath`"`nbind-address=127.0.0.1`nport=3306`ncharacter-set-server=utf8`ncollation-server=utf8_general_ci`nlower_case_table_names=1`nsql_mode=STRICT_TRANS_TABLES`ndefault-time-zone=+08:00`nexplicit_defaults_for_timestamp=0`n"
$redisDataPath=$redisData.Replace('\','/')
$redisPassword=$credentials.redis_password.Replace('\','\\').Replace('"','\"')
$redisConfig="bind 127.0.0.1`nprotected-mode yes`nport 6379`ndatabases 16`nrequirepass `"$redisPassword`"`ndir `"$redisDataPath`"`nappendonly yes`nappendfsync everysec`nsave 900 1`nsave 300 10`nsave 60 10000`nmaxmemory 256mb`nmaxmemory-policy noeviction`n"
[IO.File]::WriteAllText((Join-Path $local 'my.ini'),$mysqlConfig,[Text.UTF8Encoding]::new($false))
[IO.File]::WriteAllText((Join-Path $local 'redis.conf'),$redisConfig,[Text.UTF8Encoding]::new($false))
@{mysqlHome=$MySqlHome;redisHome=$RedisHome} | ConvertTo-Json | Set-Content -LiteralPath (Join-Path $local 'service-tools.json') -Encoding UTF8
Write-Host 'Service configs generated under .local. No server was initialized or started; no existing root password was changed.'
Write-Host 'Initialize the NEW MySQL data directory with --initialize --console as described in the startup guide.'
