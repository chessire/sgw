@echo off
setlocal

REM ========================================
REM   Batch Application Launcher
REM ========================================

set BATCH_JAR=app\batch\target\batch.jar

if not exist "%BATCH_JAR%" (
    echo ERROR: Batch JAR not found: %BATCH_JAR%
    echo Please build first: mvn clean package
    pause
    exit /b 1
)

echo Batch JAR: %BATCH_JAR%
echo.

REM Set Spring Profile (default: development)
set SPRING_PROFILES_ACTIVE=%1
if "%SPRING_PROFILES_ACTIVE%"=="" set SPRING_PROFILES_ACTIVE=development

echo Active Profile: %SPRING_PROFILES_ACTIVE%
echo.

REM JVM Options
set JVM_OPTS=-Xms512m -Xmx1024m

echo ========================================
echo   Starting Batch Application...
echo ========================================
echo.

REM JDK Configuration
set JAVA_HOME=C:\Program Files\Java\jdk-17
set PATH=%JAVA_HOME%\bin;%PATH%

"%JAVA_HOME%\bin\java" %JVM_OPTS% -Dspring.profiles.active=%SPRING_PROFILES_ACTIVE% -jar "%BATCH_JAR%"

echo.
echo ========================================
echo   Batch Application Stopped
echo ========================================

endlocal
