param(
    [ValidateSet('Check','Init','Restore','Export')][string]$Action='Check',
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
$migrationDirectory=Join-Path $project 'up-self-admin/src/main/resources/db/draft'
$migrationVersions=@(Get-ChildItem -LiteralPath $migrationDirectory -Filter 'V*__*.sql' -File | ForEach-Object { if ($_.BaseName -match '^V([0-9]+)__') { [int]$Matches[1] } } | Sort-Object)
$migrationLabel='V'+($migrationVersions -join ', V')
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

function Get-SnapshotHash([string]$Path) {
    $stream=[IO.File]::OpenRead($Path)
    $algorithm=[Security.Cryptography.SHA256]::Create()
    try { return [BitConverter]::ToString($algorithm.ComputeHash($stream)).Replace('-','').ToLowerInvariant() }
    finally { $algorithm.Dispose(); $stream.Dispose() }
}

function Invoke-Database([string]$Sql,[string]$Database,[string]$InputFile) {
    $start=New-Object Diagnostics.ProcessStartInfo
    $start.FileName=$mysql
    $start.Arguments='--defaults-extra-file="'+$RootClientFile+'" --connect-timeout=5 --default-character-set=utf8mb4 --batch --skip-column-names'
    $start.StandardOutputEncoding=[Text.Encoding]::UTF8
    $start.StandardErrorEncoding=[Text.Encoding]::UTF8
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
    if ($tableCount -gt 0) {
        $hasHistory=Invoke-Database -Sql "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema='$DatabaseName' AND table_name='flyway_schema_history';"
        if ($hasHistory -eq '1') {
            $versions=Invoke-Database -Sql 'SELECT version FROM flyway_schema_history WHERE success=1 ORDER BY installed_rank;' -Database $DatabaseName
            Write-Host "Applied Flyway versions: $($versions -replace '\s+', ', '). Source migrations: $migrationLabel."
        }
    }
    return
}
if ($Action -eq 'Export') {
    if ($DatabaseName -ne 'up_self_dev') { throw 'Export only accepts the independent application database up_self_dev.' }
    if ($tableCount -eq 0) { throw 'Cannot export an empty database.' }
    $versions=@((Invoke-Database -Sql 'SELECT version FROM flyway_schema_history WHERE success=1 ORDER BY installed_rank;' -Database $DatabaseName) -split '\s+')
    if (($versions -join ',') -ne ($migrationVersions -join ',')) { throw 'Apply all source migrations successfully before refreshing the snapshot.' }
    $uploadSource=Join-Path $project '.local/uploads'
    $stage=Join-Path $localDirectory ('snapshot-build-'+[Guid]::NewGuid().ToString('N'))
    New-Item -ItemType Directory -Path (Join-Path $stage 'uploads') -Force | Out-Null
    Copy-Item -LiteralPath (Join-Path $snapshot 'local-credentials.json') -Destination $stage
    # Copy only database-referenced files. Scratch uploads and abandoned drafts are not published.
    $referenceSql="SELECT 'learning',storage_key FROM learning_resource WHERE storage_key IS NOT NULL AND storage_key<>'' UNION SELECT 'assignment',storage_key FROM assignment_file WHERE deleted=0 ORDER BY 1,2;"
    $references=Invoke-Database -Sql $referenceSql -Database $DatabaseName
    foreach ($line in ($references -split '\r?\n')) {
        if (-not $line) { continue }
        $parts=$line -split "`t",2
        if ($parts.Count -ne 2 -or $parts[1] -notmatch '^[a-f0-9]{32}\.[a-zA-Z0-9]+$') { throw 'Unrecognized database storage key; snapshot was not published.' }
        $relative=if ($parts[0] -eq 'assignment') { 'assignments/'+$parts[1] } else { $parts[1] }
        $source=Join-Path $uploadSource $relative
        if (-not (Test-Path -LiteralPath $source -PathType Leaf)) { throw "A referenced upload is missing: $relative" }
        $destination=Join-Path (Join-Path $stage 'uploads') $relative
        New-Item -ItemType Directory -Path (Split-Path -Parent $destination) -Force | Out-Null
        Copy-Item -LiteralPath $source -Destination $destination
    }
    $dump=Join-Path $MySqlHome 'bin/mysqldump.exe'
    if (-not (Test-Path -LiteralPath $dump)) { throw 'mysqldump.exe is required for Export.' }
    $start=New-Object Diagnostics.ProcessStartInfo
    $start.FileName=$dump
    $start.Arguments='--defaults-extra-file="'+$RootClientFile+'" --single-transaction --quick --routines --triggers --events --hex-blob --set-gtid-purged=OFF --no-tablespaces --default-character-set=utf8mb4 '+$DatabaseName
    $start.UseShellExecute=$false
    $start.CreateNoWindow=$true
    $start.RedirectStandardOutput=$true
    $start.RedirectStandardError=$true
    $process=New-Object Diagnostics.Process
    $process.StartInfo=$start
    [void]$process.Start()
    $errorTask=$process.StandardError.ReadToEndAsync()
    $dumpFile=[IO.File]::Create((Join-Path $stage 'up_self_dev.sql'))
    try { $process.StandardOutput.BaseStream.CopyTo($dumpFile) } finally { $dumpFile.Dispose() }
    $process.WaitForExit()
    $dumpExit=$process.ExitCode
    $process.Dispose()
    if ($dumpExit -ne 0) { throw 'Database export failed. Credentials and database content were not logged; the previous snapshot is unchanged.' }
    if ((Invoke-Database -Sql $referenceSql -Database $DatabaseName) -ne $references) { throw 'Uploads changed during export. Pause application writes and retry; the previous snapshot is unchanged.' }
    foreach ($line in ($references -split '\r?\n')) {
        if (-not $line) { continue }
        $parts=$line -split "`t",2
        $relative=if ($parts[0] -eq 'assignment') { 'assignments/'+$parts[1] } else { $parts[1] }
        $original=Join-Path $uploadSource $relative
        $copied=Join-Path (Join-Path $stage 'uploads') $relative
        if (-not (Test-Path -LiteralPath $original) -or (Get-SnapshotHash $original) -ne (Get-SnapshotHash $copied)) { throw 'A referenced upload changed during export. Pause application writes and retry.' }
    }
    $counts=[ordered]@{}
    foreach ($table in @('sys_user','learning_course','learning_resource','assignment','assignment_submission','assignment_file','mood_notice_ack','mood_entry','learning_chapter_progress')) {
        $counts[$table]=[int](Invoke-Database -Sql "SELECT COUNT(*) FROM $table;" -Database $DatabaseName)
    }
    $files=[ordered]@{}
    $stagePrefix=[IO.Path]::GetFullPath($stage).TrimEnd('\')+'\'
    foreach ($file in (Get-ChildItem -LiteralPath $stage -File -Recurse | Sort-Object FullName)) {
        $relative=$file.FullName.Substring($stagePrefix.Length).Replace('\','/')
        $files[$relative]=[ordered]@{bytes=$file.Length;sha256=(Get-SnapshotHash $file.FullName)}
    }
    $manifest=[ordered]@{createdAt=[DateTimeOffset]::Now.ToString('o');database=$DatabaseName;source='Independent UP-SELF local database; reference database excluded';mysql='5.7.44';redis='5.0.14.1';applicationConfiguration='../../config/application-local.yml';containsPlaintextCredentials=$true;flywayVersions=$versions;rowCounts=$counts;uploadFileCount=@($files.Keys | Where-Object { $_ -like 'uploads/*' }).Count;files=$files}
    [IO.File]::WriteAllText((Join-Path $stage 'manifest.json'),($manifest | ConvertTo-Json -Depth 8),[Text.UTF8Encoding]::new($false))
    # Remove only obsolete files named by the previous manifest, after checking their exact resolved scope.
    $snapshotPrefix=[IO.Path]::GetFullPath($snapshot).TrimEnd('\')+'\'
    $oldManifest=Get-Content -LiteralPath (Join-Path $snapshot 'manifest.json') -Raw -Encoding UTF8 | ConvertFrom-Json
    foreach ($old in $oldManifest.files.PSObject.Properties) {
        if (-not $files.Contains($old.Name)) {
            $oldPath=[IO.Path]::GetFullPath((Join-Path $snapshot $old.Name))
            if (-not $oldPath.StartsWith($snapshotPrefix,[StringComparison]::OrdinalIgnoreCase)) { throw 'Snapshot path escaped the snapshot directory.' }
            if (Test-Path -LiteralPath $oldPath -PathType Leaf) { Remove-Item -LiteralPath $oldPath }
        }
    }
    foreach ($file in (Get-ChildItem -LiteralPath $stage -File -Recurse | Where-Object { $_.Name -ne 'manifest.json' })) {
        $destination=Join-Path $snapshot $file.FullName.Substring($stagePrefix.Length)
        New-Item -ItemType Directory -Path (Split-Path -Parent $destination) -Force | Out-Null
        Copy-Item -LiteralPath $file.FullName -Destination $destination -Force
    }
    Copy-Item -LiteralPath (Join-Path $stage 'manifest.json') -Destination (Join-Path $snapshot 'manifest.json') -Force
    Write-Host "Snapshot refreshed: $tableCount tables, $($manifest.uploadFileCount) referenced uploads, Flyway $migrationLabel. Source database and passwords were not changed."
    return
}
if ($tableCount -ne 0) { throw "Refusing $Action into non-empty database $DatabaseName. Use a new empty database; existing data was not removed." }
if ($Action -eq 'Restore') {
    $manifest=Get-Content -LiteralPath (Join-Path $snapshot 'manifest.json') -Raw -Encoding UTF8 | ConvertFrom-Json
    foreach ($entry in $manifest.files.PSObject.Properties) {
        $file=[IO.Path]::GetFullPath((Join-Path $snapshot $entry.Name))
        $snapshotPrefix=[IO.Path]::GetFullPath($snapshot).TrimEnd('\')+'\'
        if (-not $file.StartsWith($snapshotPrefix,[StringComparison]::OrdinalIgnoreCase)) { throw 'Snapshot path escaped the snapshot directory.' }
        if (-not (Test-Path -LiteralPath $file)) { throw "Snapshot file is missing: $($entry.Name)" }
        if ((Get-SnapshotHash $file) -ne $entry.Value.sha256) { throw "Snapshot checksum mismatch: $($entry.Name)" }
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
} else { Write-Host "Empty $DatabaseName and scoped users are ready. Starting the backend will execute Flyway $migrationLabel and initialize accounts." }
Write-Host 'Existing root passwords and existing application-user passwords were not reset.'
