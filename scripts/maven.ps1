param([Parameter(ValueFromRemainingArguments=$true)][string[]]$MavenArgs)
$ErrorActionPreference='Stop'
$projectRoot=Split-Path -Parent $PSScriptRoot
$jdkHome='C:\Program Files\Java\jdk1.8.0_231'
$mavenHome='C:\Program Files\Apache\apache-maven-3.10.0'
$env:JAVA_HOME=$jdkHome
$bootJar=Get-ChildItem -LiteralPath "$mavenHome\boot" -Filter 'plexus-classworlds-*.jar' | Select-Object -First 1
if (-not $bootJar) {throw 'Maven launcher not found'}
Push-Location -LiteralPath $projectRoot
try { & "$jdkHome\bin\java.exe" "-Dmaven.home=$mavenHome" "-Dclassworlds.conf=$mavenHome\bin\m2.conf" "-Dmaven.multiModuleProjectDirectory=$projectRoot" '-Dfile.encoding=UTF-8' -classpath $bootJar.FullName org.codehaus.plexus.classworlds.launcher.Launcher -B @MavenArgs; if($LASTEXITCODE -ne 0){throw "Maven failed ($LASTEXITCODE)"} }
finally {Pop-Location}
