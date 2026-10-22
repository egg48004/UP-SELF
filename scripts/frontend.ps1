param([ValidateSet('dev','build','typecheck','lint','test','install','verify')][string]$Action='dev')
$ErrorActionPreference='Stop'
. "$PSScriptRoot/dev-tools.ps1"
$tools=Get-UPSelfTools
$env:PATH=$tools.nodeHome+';'+$env:PATH
Push-Location -LiteralPath (Join-Path $tools.projectRoot 'frontend')
try {
    if ($Action -eq 'verify') {
        $python=Get-Command python.exe -ErrorAction SilentlyContinue
        $pythonExe=if ($python) { $python.Source } else { Join-Path $env:USERPROFILE '.cache/codex-runtimes/codex-primary-runtime/dependencies/python/python.exe' }
        if (-not (Test-Path -LiteralPath $pythonExe)) { throw 'Optional browser regression requires Python 3.12 and Chrome. Normal startup does not require Python.' }
        & $pythonExe (Join-Path $PSScriptRoot 'verify-frontend.py')
    } else {
        # Keep a one-item argument array: Windows PowerShell otherwise splats "ci" as c, i.
        [string[]]$npmArgs=if ($Action -eq 'install') { @('ci') } else { @('run',$Action) }
        & (Join-Path $tools.nodeHome 'node.exe') (Join-Path $tools.nodeHome 'node_modules/npm/bin/npm-cli.js') @npmArgs
    }
    if ($LASTEXITCODE -ne 0) { throw "Frontend action failed: $Action" }
} finally { Pop-Location }
