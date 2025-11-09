@echo off
setlocal enabledelayedexpansion

pushd "%~dp0" >nul

set "PROJECT_NAME=sgw-backend"
set "COMPOSE_FILES=-f docker-compose.yml -f docker-compose.mysql.yml -f docker-compose.rabbitmq.yml -f docker-compose.redis.yml"

echo Stopping services ...
docker compose -p %PROJECT_NAME% %COMPOSE_FILES% down
if errorlevel 1 goto :error

echo Done.
goto :end

:error
echo Failed.

:end
popd >nul
endlocal

