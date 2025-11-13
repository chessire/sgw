@echo off
setlocal

REM ========================================
REM   Worker Application Launcher
REM ========================================

set WORKER_JAR=app\worker\target\worker.jar

if not exist "%WORKER_JAR%" (
    echo ERROR: Worker JAR not found: %WORKER_JAR%
    echo Please build first: mvn clean package
    pause
    exit /b 1
)

echo Worker JAR: %WORKER_JAR%
echo.

REM Set Spring Profile (default: development)
set SPRING_PROFILES_ACTIVE=%1
if "%SPRING_PROFILES_ACTIVE%"=="" set SPRING_PROFILES_ACTIVE=development

echo Active Profile: %SPRING_PROFILES_ACTIVE%
echo.

REM JVM Options
set JVM_OPTS=-Xms512m -Xmx2048m

echo ========================================
echo   Starting Worker Application...
echo ========================================
echo.

REM JDK Configuration
set JAVA_HOME=C:\Program Files\Java\jdk-17
set PATH=%JAVA_HOME%\bin;%PATH%

"%JAVA_HOME%\bin\java" %JVM_OPTS% -Dspring.profiles.active=%SPRING_PROFILES_ACTIVE% -jar "%WORKER_JAR%"

echo.
echo ========================================
echo   Worker Application Stopped
echo ========================================

endlocal
