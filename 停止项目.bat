@echo off
powershell.exe -NoProfile -ExecutionPolicy Bypass -File "%~dp0stop-infra.ps1"
echo.
pause
