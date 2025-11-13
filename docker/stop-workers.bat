@echo off
chcp 65001 > nul
echo ========================================
echo   Stopping 3 Workers
echo ========================================
echo.

docker-compose -f docker-compose.three-workers.yml down

echo.
echo ========================================
echo   Workers Stopped Successfully!
echo ========================================
echo.
pause

