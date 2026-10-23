param([switch]$SkipBuild)
$ErrorActionPreference = 'Stop'
. "$PSScriptRoot/dev-tools.ps1"
$tools = Get-UPSelfTools
# Build with existing tools and locked dependencies. No npm install, server upload or system install.
if (-not $SkipBuild) {
    & "$PSScriptRoot/maven.ps1" '-o' 'package'
    & "$PSScriptRoot/frontend.ps1" build
}
$jar = Join-Path $tools.projectRoot 'up-self-admin/target/up-self-admin.jar'
$web = Join-Path $tools.projectRoot 'frontend/dist'
if (-not (Test-Path -LiteralPath $jar) -or -not (Test-Path -LiteralPath (Join-Path $web 'index.html'))) {
    throw 'Build the backend and frontend before packaging.'
}
$tar = Get-Command tar.exe -ErrorAction Stop
$releaseId = 'up-self-' + (Get-Date -Format 'yyyyMMdd-HHmmss')
$outputRoot = Join-Path $tools.projectRoot '.local/linux-release'
$stage = Join-Path $outputRoot $releaseId
if (Test-Path -LiteralPath $stage) { throw 'Release directory already exists. Run again with a new timestamp.' }
New-Item -ItemType Directory -Path (Join-Path $stage 'app'),(Join-Path $stage 'web'),(Join-Path $stage 'linux') -Force | Out-Null
Copy-Item -LiteralPath $jar -Destination (Join-Path $stage 'app/up-self-admin.jar')
Get-ChildItem -LiteralPath $web -Force | Copy-Item -Destination (Join-Path $stage 'web') -Recurse
# Explicit allowlist: never include local configs, database snapshots, uploads or node_modules.
foreach ($name in @('run.sh','preflight.sh','backup.sh','application-prod.yml.example','up-self.env.example','up-self.service.example','nginx-http.conf.example','nginx-https.conf.example','mysql-backup.cnf.example')) {
    Copy-Item -LiteralPath (Join-Path $tools.projectRoot "deploy/linux/$name") -Destination (Join-Path $stage "linux/$name")
}
$utf8 = New-Object Text.UTF8Encoding($false)
$manifest = [ordered]@{
    releaseId=$releaseId
    preparedOn=(Get-Date -Format 'o')
    status='prepared-not-linux-integration-tested'
    schemaVersion='6'
    jdk='1.8.0_231 (current Oracle baseline; Linux license/OS review pending)'
    mysql='5.7.44'
    redis='Linux 5.0.14 candidate; Windows baseline is port 5.0.14.1; review pending'
    sourceVersions='environment/versions.json and locked POM/package-lock.json'
    buildSkipped=[bool]$SkipBuild
    includesData=$false
    includesCredentials=$false
}
[IO.File]::WriteAllText((Join-Path $stage 'release.json'), ($manifest | ConvertTo-Json -Depth 5), $utf8)
function Get-ReleaseSha256([string]$File) {
    $stream = [IO.File]::OpenRead($File)
    $hasher = [Security.Cryptography.SHA256]::Create()
    try { return [BitConverter]::ToString($hasher.ComputeHash($stream)).Replace('-','').ToLowerInvariant() }
    finally { $hasher.Dispose(); $stream.Dispose() }
}
$lines = foreach ($file in (Get-ChildItem -LiteralPath $stage -Recurse -File | Sort-Object FullName)) {
    $relative = $file.FullName.Substring($stage.Length + 1).Replace('\','/')
    (Get-ReleaseSha256 $file.FullName) + '  ' + $relative
}
[IO.File]::WriteAllText((Join-Path $stage 'SHA256SUMS'), (($lines -join "`n") + "`n"), $utf8)
$archive = Join-Path $outputRoot ($releaseId + '.tar.gz')
& $tar.Source -czf $archive -C $stage .
if ($LASTEXITCODE -ne 0) { throw 'Release archive creation failed.' }
[IO.File]::WriteAllText(($archive + '.sha256'), ((Get-ReleaseSha256 $archive) + '  ' + (Split-Path -Leaf $archive) + "`n"), $utf8)
Write-Host "Prepared local release: $archive"
Write-Host 'No server was contacted. Secrets, database and uploads must be transferred separately during an approved deployment.'
