$ErrorActionPreference='Stop'
. "$PSScriptRoot/dev-tools.ps1"
$tools=Get-UPSelfTools
& "$PSScriptRoot/services.ps1"
$jar=Join-Path $tools.projectRoot 'up-self-admin/target/up-self-admin.jar'
if (-not (Test-Path -LiteralPath $jar)) { & "$PSScriptRoot/build.ps1" }
Push-Location -LiteralPath $tools.projectRoot
try {
    & (Join-Path $tools.javaHome 'bin/java.exe') '-Dfile.encoding=UTF-8' -jar $jar '--spring.config.additional-location=file:./config/'
    if ($LASTEXITCODE -ne 0) { throw 'Application stopped with error' }
} finally { Pop-Location }
