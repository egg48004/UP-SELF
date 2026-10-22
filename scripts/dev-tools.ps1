# Tool paths stay local; no system PATH changes or software installation.
function Get-UPSelfTools {
    param([string]$JavaHome, [string]$MavenHome, [string]$NodeHome, [string]$ServiceHome)
    $projectRoot = Split-Path -Parent $PSScriptRoot
    $savedFile = Join-Path $projectRoot '.local/development-tools.json'
    $saved = @{}
    if (Test-Path -LiteralPath $savedFile) { $saved = Get-Content -LiteralPath $savedFile -Raw -Encoding UTF8 | ConvertFrom-Json }
    function Find-ToolHome($candidates, $relativeExecutable, $label) {
        foreach ($candidate in $candidates) {
            if ($candidate -and (Test-Path -LiteralPath (Join-Path $candidate $relativeExecutable))) { return (Resolve-Path -LiteralPath $candidate).Path }
        }
        throw "$label was not found. Run scripts/configure-local.ps1 with its installation directory."
    }
    $javaCommand = Get-Command javac.exe -ErrorAction SilentlyContinue
    $mavenCommand = Get-Command mvn.cmd -ErrorAction SilentlyContinue
    $nodeCommand = Get-Command node.exe -ErrorAction SilentlyContinue
    $javaFromPath = if ($javaCommand) { Split-Path -Parent (Split-Path -Parent $javaCommand.Source) }
    $mavenFromPath = if ($mavenCommand) { Split-Path -Parent (Split-Path -Parent $mavenCommand.Source) }
    $nodeFromPath = if ($nodeCommand) { Split-Path -Parent $nodeCommand.Source }
    $legacyRoot = Join-Path $env:USERPROFILE 'Documents/Codex/local-env'
    $nodeRuntimeFile = Join-Path $legacyRoot 'nodejs/runtime-path.txt'
    $legacyNode = if (Test-Path -LiteralPath $nodeRuntimeFile) { (Get-Content -LiteralPath $nodeRuntimeFile -Raw).Trim() }
    $javaToolHome = Find-ToolHome @($JavaHome, $saved.javaHome, $env:JAVA_HOME, $javaFromPath, (Join-Path $env:ProgramFiles 'Java/jdk1.8.0_231')) 'bin/java.exe' 'Java 8 JDK'
    $mavenToolHome = Find-ToolHome @($MavenHome, $saved.mavenHome, $env:MAVEN_HOME, $env:M2_HOME, $mavenFromPath, (Join-Path $env:ProgramFiles 'Apache/apache-maven-3.10.0')) 'bin/mvn.cmd' 'Maven'
    $nodeToolHome = Find-ToolHome @($NodeHome, $saved.nodeHome, $env:UPSELF_NODE_HOME, $legacyNode, $nodeFromPath, (Join-Path $env:ProgramFiles 'nodejs')) 'node.exe' 'Node.js'
    $services = @($ServiceHome, $saved.serviceHome, $env:UPSELF_SERVICE_HOME, (Join-Path $legacyRoot 'up-self')) | Where-Object { $_ -and (Test-Path -LiteralPath (Join-Path $_ 'scripts/start.ps1')) } | Select-Object -First 1
    [PSCustomObject]@{ projectRoot=$projectRoot; javaHome=$javaToolHome; mavenHome=$mavenToolHome; nodeHome=$nodeToolHome; serviceHome=$services }
}

function Invoke-UPSelfNative {
    param([string]$Executable, [string[]]$Arguments)
    $savedPreference = $ErrorActionPreference
    $ErrorActionPreference = 'Continue'
    try { $result = & $Executable @Arguments 2>&1; $nativeExit = $LASTEXITCODE }
    finally { $ErrorActionPreference = $savedPreference }
    if ($nativeExit -ne 0) { throw "Tool failed: $Executable (exit $nativeExit)" }
    return ($result | Out-String).Trim()
}
