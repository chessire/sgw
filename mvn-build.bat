@echo off
REM Maven 전체 빌드 스크립트

echo =====================================
echo Maven Multi-Module Build Script
echo =====================================
echo.

REM Maven 경로 설정
set MAVEN_HOME=C:\apache-maven-3.9.11
set PATH=%MAVEN_HOME%\bin;%PATH%

REM Java 경로 설정
set JAVA_HOME=C:\Program Files\Java\jdk-17
set PATH=%JAVA_HOME%\bin;%PATH%

echo Maven Version:
call mvn -version
echo.

echo Starting build...
echo.

REM 전체 빌드
call mvn clean install

echo.
echo =====================================
echo Build Complete!
echo =====================================
echo.

echo WAR files:
echo - app/api/target/api.war
echo - app/web-static/target/web.war
echo.

echo JAR files:
echo - app/batch/target/batch.jar
echo - app/worker/target/worker.jar
echo.

pause

