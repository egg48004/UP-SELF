# VS Code's incremental Java builder may not copy newly added XML/SQL resources.
# Use the existing Maven lifecycle before debugging; no dependency/version changes.
$ErrorActionPreference = 'Stop'
& "$PSScriptRoot/services.ps1"
& "$PSScriptRoot/maven.ps1" process-resources
if ($LASTEXITCODE -ne 0) { throw 'Backend resource preparation failed.' }
Write-Host 'Backend XML, SQL and application resources are ready for VS Code debugging.'
