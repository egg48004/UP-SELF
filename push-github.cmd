@echo off
powershell.exe -NoProfile -ExecutionPolicy Bypass -File "%~dp0scripts\push-github.ps1"
if errorlevel 1 (
  echo Push failed. Read the error above; no force push was used.
  pause
  exit /b 1
)
pause
