$ErrorActionPreference='Stop'
$projectRoot=Split-Path -Parent $PSScriptRoot
$code='C:\Users\eggma\AppData\Local\Programs\Microsoft VS Code\bin\code.cmd'
& $code --new-window "$projectRoot\up-self.code-workspace"
