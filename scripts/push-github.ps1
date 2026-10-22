param([string]$Message = 'Publish assignments, weekly mood and portable setup')
$ErrorActionPreference = 'Stop'
$project = Split-Path -Parent $PSScriptRoot
$repository = 'https://github.com/egg48004/UP-SELF.git'

function Invoke-Git([string[]]$GitArguments) {
    # Git owns its progress/error streams; Windows PowerShell must not treat
    # successful stderr progress as a terminating PowerShell error.
    $savedPreference = $ErrorActionPreference
    $ErrorActionPreference = 'Continue'
    try {
        $output = & git -c "safe.directory=$project" -C $project -c http.sslBackend=openssl @GitArguments 2>&1
        $gitExit = $LASTEXITCODE
    } finally { $ErrorActionPreference = $savedPreference }
    $output | ForEach-Object { Write-Host $_.ToString() }
    if ($gitExit -ne 0) { throw "Git command failed (exit $gitExit). No force push was attempted." }
}

foreach ($required in @('config/application-local.yml', 'migration/local-snapshot/up_self_dev.sql', 'migration/local-snapshot/local-credentials.json', 'migration/local-snapshot/manifest.json')) {
    if (-not (Test-Path -LiteralPath (Join-Path $project $required))) { throw "Missing required snapshot file: $required" }
}
if (-not (Test-Path -LiteralPath (Join-Path $project '.git'))) { Invoke-Git -GitArguments @('init', '-b', 'main') }
$remotes = @(& git -c "safe.directory=$project" -C $project remote)
if ($remotes -notcontains 'origin') { Invoke-Git -GitArguments @('remote', 'add', 'origin', $repository) }
else {
    $origin = & git -c "safe.directory=$project" -C $project remote get-url origin
    if ($origin.TrimEnd('/') -ne $repository) { throw 'Origin is not the requested UP-SELF repository; refusing to change it.' }
}

Write-Host 'This upload includes the actual plaintext local application credentials, database snapshot and uploaded files.'
Write-Host 'GitHub may ask you to sign in through your browser. Complete the login yourself; do not paste a token here.'
Invoke-Git -GitArguments @('fetch', 'origin', 'main')
& git -c "safe.directory=$project" -C $project show-ref --verify --quiet refs/heads/main
if ($LASTEXITCODE -ne 0) {
    # Preserve the remote initial commit and all local working files.
    Invoke-Git -GitArguments @('reset', '--mixed', 'origin/main')
}
$branch = & git -c "safe.directory=$project" -C $project branch --show-current
if ($branch -ne 'main') { throw "Current branch is $branch. Switch deliberately before publishing to main." }
Invoke-Git -GitArguments @('add', '--all')
& git -c "safe.directory=$project" -C $project diff --cached --quiet
if ($LASTEXITCODE -eq 1) { Invoke-Git -GitArguments @('commit', '-m', $Message) }
elseif ($LASTEXITCODE -ne 0) { throw 'Cannot inspect staged changes.' }
Invoke-Git -GitArguments @('push', '-u', 'origin', 'HEAD:main')
Write-Host 'Push completed. Repository: https://github.com/egg48004/UP-SELF'
Invoke-Git -GitArguments @('log', '-1', '--format=%h %s')
