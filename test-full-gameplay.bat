@echo off
echo.
echo ============================================================
echo   Money Inclusion Game - Full Test
echo   - User API (create, login, nickname, etc.)
echo   - Tutorial 6 rounds + Competition 12 rounds
echo   - Results, Achievements, Rankings
echo ============================================================
echo.

REM Server URL
set SERVER=http://localhost:8080/api

REM Parameter handling
if "%1"=="remote" (
    set SERVER=http://1.234.75.39:8080/api
    echo [Mode] Remote Server Test
) else (
    echo [Mode] Local Server Test
)

echo [Server] %SERVER%
echo.

REM Run PowerShell script
powershell -ExecutionPolicy Bypass -File "%~dp0test-full-gameplay.ps1" -Server "%SERVER%" %2 %3 %4

echo.
echo Test completed.
pause
