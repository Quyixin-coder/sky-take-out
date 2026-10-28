@echo off
powershell.exe -NoProfile -ExecutionPolicy Bypass -File "%~dp0start-infra.ps1" -DependenciesOnly
if errorlevel 1 echo Startup failed. Read the error above.
echo.
pause
