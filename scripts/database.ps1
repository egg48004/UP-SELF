param(
    [ValidateSet('Check','Init','Restore')][string]$Action='Check',
    [string]$MySqlHome,
    [string]$RootClientFile,
    [ValidatePattern('^up_self_[a-z0-9_]{1,40}$')][string]$DatabaseName='up_self_dev',
    [switch]$SkipAccountSetup,
    [switch]$SkipUploads
)
$ErrorActionPreference='Stop'
. "$PSScriptRoot/dev-tools.ps1"
$project=Split-Path -Parent $PSScriptRoot
$snapshot=Join-Path $project 'migration/local-snapshot'
$credentials=Get-Content -LiteralPath (Join-Path $snapshot 'local-credentials.json') -Raw -Encoding UTF8 | ConvertFrom-Json
if (-not $MySqlHome) {
    $serviceFile=Join-Path $project '.local/service-tools.json'
    $mysqlCommand=Get-Command mysql.exe -ErrorAction SilentlyContinue
    if (Test-Path -LiteralPath $serviceFile) { $MySqlHome=(Get-Content -LiteralPath $serviceFile -Raw -Encoding UTF8 | ConvertFrom-Json).mysqlHome }
    elseif ($mysqlCommand) { $MySqlHome=Split-Path -Parent (Split-Path -Parent $mysqlCommand.Source) }
    else {
        $tools=Get-UPSelfTools
        if ($tools.serviceHome) { $MySqlHome=Join-Path $tools.serviceHome 'mysql' }
    }
}
if (-not $MySqlHome -or -not (Test-Path -LiteralPath (Join-Path $MySqlHome 'bin/mysql.exe'))) { throw 'Specify -MySqlHome with the MySQL 5.7.44 installation directory.' }
$mysql=Join-Path $MySqlHome 'bin/mysql.exe'
$version=Invoke-UPSelfNative -Executable $mysql -Arguments @('--version')
if ($version -notmatch '5\.7\.44\b') { throw 'This snapshot requires the established MySQL 5.7.44 baseline.' }
$localDirectory=Join-Path $project '.local'
New-Item -ItemType Directory -Path $localDirectory -Force | Out-Null
if (-not $RootClientFile) {
    # Reuse the owner's actual local root credential, never ALTER USER root.
    $RootClientFile=Join-Path $localDirectory 'migration-root-client.cnf'
    $rootPassword=$credentials.mysql_root_password.Replace('\','\\').Replace('"','\"')
    $clientText="[client]`nuser=root`npassword=`"$rootPassword`"`nhost=127.0.0.1`nport=3306`nprotocol=tcp`n"
    [IO.File]::WriteAllText($RootClientFile,$clientText,[Text.UTF8Encoding]::new($false))
}
$RootClientFile=(Resolve-Path -LiteralPath $RootClientFile).Path

function Invoke-Database([string]$Sql,[string]$Database,[string]$InputFile) {
    $start=New-Object Diagnostics.ProcessStartInfo
    $start.FileName=$mysql
    $start.Arguments='--defaults-extra-file="'+$RootClientFile+'" --connect-timeout=5 --default-character-set=utf8 --batch --skip-column-names'
    if ($Database) { $start.Arguments+=' --database='+$Database }
    $start.UseShellExecute=$false
    $start.CreateNoWindow=$true
    $start.RedirectStandardInput=$true
    $start.RedirectStandardOutput=$true
    $start.RedirectStandardError=$true
    $process=New-Object Diagnostics.Process
    $process.StartInfo=$start
    [void]$process.Start()
    $outputTask=$process.StandardOutput.ReadToEndAsync()
    $errorTask=$process.StandardError.ReadToEndAsync()
    $bytes=if ($InputFile) { [IO.File]::ReadAllBytes($InputFile) } else { [Text.Encoding]::UTF8.GetBytes($Sql) }
    $process.StandardInput.BaseStream.Write($bytes,0,$bytes.Length)
    $process.StandardInput.Close()
    $process.WaitForExit()
    $output=$outputTask.Result
    $errorText=$errorTask.Result
    $exitCode=$process.ExitCode
    $process.Dispose()
    if ($exitCode -ne 0) {
        foreach ($property in $credentials.PSObject.Properties) {
            if ($property.Name -like '*password*' -and $property.Value) { $errorText=$errorText.Replace([string]$property.Value,'[REDACTED]') }
        }
        throw "MySQL action failed: $errorText"
    }
    return $output.Trim()
}

$tableCount=[int](Invoke-Database -Sql "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema='$DatabaseName';")
if ($Action -eq 'Check') {
    Write-Host "MySQL connection OK; $DatabaseName has $tableCount tables. No database changes were made."
    return
}
if ($tableCount -ne 0) { throw "Refusing $Action into non-empty database $DatabaseName. Use a new empty database; existing data was not removed." }
if ($Action -eq 'Restore') {
    $manifest=Get-Content -LiteralPath (Join-Path $snapshot 'manifest.json') -Raw -Encoding UTF8 | ConvertFrom-Json
    foreach ($entry in $manifest.files.PSObject.Properties) {
        $file=Join-Path $snapshot $entry.Name
        if (-not (Test-Path -LiteralPath $file)) { throw "Snapshot file is missing: $($entry.Name)" }
        if ((Get-FileHash -LiteralPath $file -Algorithm SHA256).Hash.ToLowerInvariant() -ne $entry.Value.sha256) { throw "Snapshot checksum mismatch: $($entry.Name)" }
    }
    if (-not $SkipUploads) {
        $uploadTarget=Join-Path $project '.local/uploads'
        if ((Test-Path -LiteralPath $uploadTarget) -and @(Get-ChildItem -LiteralPath $uploadTarget -File -Recurse).Count -ne 0) {
            throw 'Refusing to merge uploads into an existing non-empty upload directory. Preserve it and restore into a fresh project clone.'
        }
    }
}
[void](Invoke-Database -Sql "CREATE DATABASE IF NOT EXISTS $DatabaseName CHARACTER SET utf8 COLLATE utf8_general_ci;")
if (-not $SkipAccountSetup) {
    foreach ($role in @('app','migration')) {
        $account=[string]$credentials.('mysql_'+$role+'_user')
        $password=[string]$credentials.('mysql_'+$role+'_password')
        if ($account -notmatch '^[a-zA-Z][a-zA-Z0-9_]{0,31}$') { throw 'Invalid snapshot account name.' }
        $sqlPassword=$password.Replace('\','\\').Replace("'","\'")
        $privileges=if ($role -eq 'app') { 'SELECT,INSERT,UPDATE,DELETE' } else { 'ALL PRIVILEGES' }
        foreach ($hostName in @('127.0.0.1','localhost')) {
            [void](Invoke-Database -Sql "CREATE USER IF NOT EXISTS '$account'@'$hostName' IDENTIFIED BY '$sqlPassword';GRANT $privileges ON $DatabaseName.* TO '$account'@'$hostName';")
        }
    }
}
if ($Action -eq 'Restore') {
    [void](Invoke-Database -InputFile (Join-Path $snapshot 'up_self_dev.sql') -Database $DatabaseName)
    if (-not $SkipUploads) {
        $uploadTarget=Join-Path $project '.local/uploads'
        New-Item -ItemType Directory -Path $uploadTarget -Force | Out-Null
        Get-ChildItem -LiteralPath (Join-Path $snapshot 'uploads') -Force | Copy-Item -Destination $uploadTarget -Recurse
    }
    $versions=Invoke-Database -Sql 'SELECT version FROM flyway_schema_history WHERE success=1 ORDER BY installed_rank;' -Database $DatabaseName
    Write-Host "Restore completed for $DatabaseName. Flyway versions: $($versions -replace '\s+',', ')."
} else { Write-Host "Empty $DatabaseName and scoped users are ready. Starting the backend will execute Flyway V1-V3 and initialize accounts." }
Write-Host 'Existing root passwords and existing application-user passwords were not reset.'
