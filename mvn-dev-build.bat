@echo off
REM ========================================
REM Development Build Script
REM ========================================

echo.
echo ========================================
echo   Development Environment Build
echo ========================================
echo.

REM Maven 빌드 (development 프로필)
call mvn clean package -Pdevelopment -DskipTests

if %ERRORLEVEL% EQU 0 (
    echo.
    echo ========================================
    echo   Build SUCCESS!
    echo ========================================
    echo.
    echo WAR Files:
    echo   - API Server: app\api\target\api.war
    echo   - Web Server: app\web-static\target\web.war
    echo.
    echo JAR Files:
    echo   - Batch: app\batch\target\batch.jar
    echo   - Worker: app\worker\target\worker.jar
    echo.
    echo Profile: DEVELOPMENT
    echo ========================================
) else (
    echo.
    echo ========================================
    echo   Build FAILED!
    echo ========================================
    exit /b 1
)

pause

