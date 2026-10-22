$ErrorActionPreference='Stop'
$projectRoot=Split-Path -Parent $PSScriptRoot
& "$PSScriptRoot/configure-local.ps1"
$codeCommand=Get-Command code.cmd -ErrorAction SilentlyContinue
$codeExe=if ($codeCommand) { $codeCommand.Source } else { Join-Path $env:LOCALAPPDATA 'Programs/Microsoft VS Code/bin/code.cmd' }
if (-not (Test-Path -LiteralPath $codeExe)) { throw 'Install VS Code and its code command, then reopen this script.' }
& $codeExe --new-window (Join-Path $projectRoot '.local/up-self.code-workspace')
