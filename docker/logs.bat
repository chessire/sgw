@echo off
echo ========================================
echo  Docker Infra 로그 확인
echo ========================================
echo.

cd /d %~dp0

docker-compose logs -f

