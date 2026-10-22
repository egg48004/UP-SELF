param([string]$Action='dev')
$ErrorActionPreference='Stop'
$project=Split-Path $PSScriptRoot -Parent
$runtimeFile='C:\Users\eggma\Documents\Codex\local-env\nodejs\runtime-path.txt'
$runtime=(Get-Content -LiteralPath $runtimeFile -Raw).Trim()
$env:PATH="$runtime;$env:PATH"
Push-Location (Join-Path $project 'frontend')
try { if($Action -eq 'verify'){ & 'C:\Users\eggma\.cache\codex-runtimes\codex-primary-runtime\dependencies\python\python.exe' (Join-Path $PSScriptRoot 'verify-frontend.py') } elseif($Action -eq 'install'){ & (Join-Path $runtime 'node.exe') (Join-Path $runtime 'node_modules/npm/bin/npm-cli.js') ci } else { & (Join-Path $runtime 'node.exe') (Join-Path $runtime 'node_modules/npm/bin/npm-cli.js') run $Action };exit $LASTEXITCODE } finally {Pop-Location}
