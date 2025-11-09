@echo off
setlocal enabledelayedexpansion

pushd "%~dp0" >nul

rem run maven build from project root
pushd ..
start /wait "maven-build" cmd /c ".\mvnw.cmd clean package -DskipTests"
if errorlevel 1 goto :error
popd

set "NETWORK=sgw-backend-internal"
set "PROJECT_NAME=sgw-backend"
set "COMPOSE_FILES=-f docker-compose.yml -f docker-compose.mysql.yml -f docker-compose.rabbitmq.yml"
set "CONTAINERS=sgw-app-suite sgw-mysql-db sgw-rabbitmq sgw-web-static"

docker network inspect %NETWORK% >nul 2>&1
if errorlevel 1 (
    echo Creating network %NETWORK% ...
    docker network create %NETWORK%
    if errorlevel 1 (
        echo Failed to create network.
        goto :error
    )
)

echo Stopping services ...
docker compose -p %PROJECT_NAME% %COMPOSE_FILES% down

echo Removing lingering containers ...
for %%C in (%CONTAINERS%) do (
    docker rm -f %%C >nul 2>&1
)

echo Starting services (build if needed) ...
docker compose -p %PROJECT_NAME% %COMPOSE_FILES% up --build -d
if errorlevel 1 goto :error

echo Done.
goto :end

:error
echo Failed.

:end
popd >nul
endlocal

