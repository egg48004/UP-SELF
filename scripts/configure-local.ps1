param([string]$JavaHome, [string]$MavenHome, [string]$NodeHome, [string]$ServiceHome)
$ErrorActionPreference = 'Stop'
. "$PSScriptRoot/dev-tools.ps1"
$tools = Get-UPSelfTools -JavaHome $JavaHome -MavenHome $MavenHome -NodeHome $NodeHome -ServiceHome $ServiceHome
$env:JAVA_HOME = $tools.javaHome
$javaVersion = Invoke-UPSelfNative -Executable (Join-Path $tools.javaHome 'bin/java.exe') -Arguments @('-version')
$mavenVersion = Invoke-UPSelfNative -Executable (Join-Path $tools.mavenHome 'bin/mvn.cmd') -Arguments @('-v')
$nodeVersion = Invoke-UPSelfNative -Executable (Join-Path $tools.nodeHome 'node.exe') -Arguments @('--version')
$npmVersion = Invoke-UPSelfNative -Executable (Join-Path $tools.nodeHome 'node.exe') -Arguments @((Join-Path $tools.nodeHome 'node_modules/npm/bin/npm-cli.js'), '--version')
if ($javaVersion -notmatch '1\.8\.0_231') { throw 'This baseline uses JDK 8u231. Select the matching installed JDK.' }
if ($mavenVersion -notmatch 'Apache Maven 3\.10\.0\b') { throw 'This baseline uses Maven 3.10.0.' }
if ($nodeVersion -ne 'v24.22.1' -or $npmVersion -ne '11.19.1') { throw 'This baseline uses Node.js 24.22.1 and npm 11.19.1.' }
$localDirectory = Join-Path $tools.projectRoot '.local'
New-Item -ItemType Directory -Path $localDirectory -Force | Out-Null
$tools | ConvertTo-Json | Set-Content -LiteralPath (Join-Path $localDirectory 'development-tools.json') -Encoding UTF8
$workspace = Get-Content -LiteralPath (Join-Path $tools.projectRoot 'up-self.code-workspace') -Raw -Encoding UTF8 | ConvertFrom-Json
$workspace.folders[0].path = '..'
$workspace.settings | Add-Member NoteProperty 'java.configuration.runtimes' @(@{name='JavaSE-1.8';path=$tools.javaHome;default=$true}) -Force
$workspace.settings | Add-Member NoteProperty 'java.configuration.maven.userSettings' (Join-Path $tools.projectRoot '.mvn/settings.xml') -Force
$workspace.settings | Add-Member NoteProperty 'maven.executable.path' (Join-Path $tools.mavenHome 'bin/mvn.cmd') -Force
$workspace.settings | Add-Member NoteProperty 'terminal.integrated.env.windows' @{JAVA_HOME=$tools.javaHome;PATH=($tools.nodeHome+';'+(Join-Path $tools.javaHome 'bin')+';'+(Join-Path $tools.mavenHome 'bin')+';${env:PATH}')} -Force
foreach ($launch in $workspace.launch.configurations) {
    if ($launch.type -eq 'node') { $launch.runtimeExecutable = Join-Path $tools.nodeHome 'node.exe' }
}
$workspaceFile = Join-Path $localDirectory 'up-self.code-workspace'
$workspace | ConvertTo-Json -Depth 30 | Set-Content -LiteralPath $workspaceFile -Encoding UTF8
Write-Host 'Verified JDK 8u231, Maven 3.10.0, Node.js 24.22.1 and npm 11.19.1.'
Write-Host "Local workspace: $workspaceFile"
Write-Host 'Open it with open-vscode.cmd. No system PATH or database password was changed.'
