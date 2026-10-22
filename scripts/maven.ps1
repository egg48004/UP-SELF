param([Parameter(ValueFromRemainingArguments=$true)][string[]]$MavenArgs)
$ErrorActionPreference='Stop'
. "$PSScriptRoot/dev-tools.ps1"
$tools=Get-UPSelfTools
$env:JAVA_HOME=$tools.javaHome
$bootJar=Get-ChildItem -LiteralPath (Join-Path $tools.mavenHome 'boot') -Filter 'plexus-classworlds-*.jar' | Select-Object -First 1
if (-not $bootJar) { throw 'Maven launcher not found' }
Push-Location -LiteralPath $tools.projectRoot
try {
    & (Join-Path $tools.javaHome 'bin/java.exe') "-Dmaven.home=$($tools.mavenHome)" "-Dclassworlds.conf=$($tools.mavenHome)/bin/m2.conf" "-Dmaven.multiModuleProjectDirectory=$($tools.projectRoot)" "-Dmaven.repo.local=$($tools.projectRoot)/.local/maven-repository" '-Dfile.encoding=UTF-8' -classpath $bootJar.FullName org.codehaus.plexus.classworlds.launcher.Launcher -B @MavenArgs
    if ($LASTEXITCODE -ne 0) { throw "Maven failed ($LASTEXITCODE)" }
} finally { Pop-Location }
