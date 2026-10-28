@echo off
powershell.exe -NoProfile -ExecutionPolicy Bypass -File "%~dp0start-infra.ps1" -DependenciesOnly
echo.
pause
