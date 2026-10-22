$ErrorActionPreference='Stop'
$projectRoot=Split-Path -Parent $PSScriptRoot
& "$PSScriptRoot\services.ps1"
$jar="$projectRoot\up-self-admin\target\up-self-admin.jar"
if(-not(Test-Path -LiteralPath $jar)){& "$PSScriptRoot\build.ps1"}
Push-Location -LiteralPath $projectRoot
try { & 'C:\Program Files\Java\jdk1.8.0_231\bin\java.exe' '-Dfile.encoding=UTF-8' -jar $jar '--spring.config.additional-location=file:./config/';if($LASTEXITCODE -ne 0){throw 'Application stopped with error'} }
finally{Pop-Location}
