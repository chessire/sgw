@echo off
REM Tomcat 배포 스크립트

echo =====================================
echo Deploy to Tomcat Script
echo =====================================
echo.

REM Tomcat 경로 설정
set TOMCAT_HOME=C:\apache-tomcat-9.0.112
set TOMCAT_WEBAPPS=%TOMCAT_HOME%\webapps

echo Checking Tomcat directory...
if not exist "%TOMCAT_HOME%" (
    echo ERROR: Tomcat directory not found: %TOMCAT_HOME%
    pause
    exit /b 1
)

echo.
echo Copying WAR files to Tomcat webapps...
echo.

REM API WAR 복사
if exist "app\api\target\api.war" (
    echo Copying api.war...
    REM 기존 API 제거
    if exist "%TOMCAT_WEBAPPS%\api" (
        rmdir /S /Q "%TOMCAT_WEBAPPS%\api"
        echo Removed existing API directory
    )
    if exist "%TOMCAT_WEBAPPS%\api.war" (
        del /F /Q "%TOMCAT_WEBAPPS%\api.war"
        echo Removed existing api.war
    )
    copy /Y "app\api\target\api.war" "%TOMCAT_WEBAPPS%\"
    echo api.war deployed successfully!
) else (
    echo WARNING: app/api/target/api.war not found!
)

echo.

REM Web Static WAR 복사 (ROOT 컨텍스트)
if exist "app\web-static\target\ROOT.war" (
    echo Copying ROOT.war...
    REM 기존 ROOT 제거
    if exist "%TOMCAT_WEBAPPS%\ROOT" (
        rmdir /S /Q "%TOMCAT_WEBAPPS%\ROOT"
        echo Removed existing ROOT directory
    )
    if exist "%TOMCAT_WEBAPPS%\ROOT.war" (
        del /F /Q "%TOMCAT_WEBAPPS%\ROOT.war"
        echo Removed existing ROOT.war
    )
    copy /Y "app\web-static\target\ROOT.war" "%TOMCAT_WEBAPPS%\"
    echo ROOT.war deployed successfully!
) else (
    echo WARNING: app/web-static/target/ROOT.war not found!
)

echo.
echo =====================================
echo Deployment Complete!
echo =====================================
echo.

echo Start Tomcat with:
echo %TOMCAT_HOME%\bin\startup.bat
echo.

echo =====================================
echo Starting Tomcat Server...
echo =====================================
echo.

REM Tomcat 시작
set CATALINA_HOME=%TOMCAT_HOME%
cmd /c %TOMCAT_HOME%\bin\startup.bat

echo.
echo =====================================
echo Tomcat Started!
echo =====================================
echo.

echo Access URLs:
echo - Web Server (ROOT): http://localhost:8080/
echo - About Page: http://localhost:8080/about
echo - Cocostudio Example: http://localhost:8080/cocostudio-example
echo - API Server: http://localhost:8080/api/
echo - Swagger UI: http://localhost:8080/api/swagger-ui.html
echo.
echo Test API:
echo curl http://localhost:8080/api/test/hello
echo.
echo Check logs at: %TOMCAT_HOME%\logs
echo.

pause

