@echo off
echo ========================================
echo  Docker Infra 중지
echo ========================================
echo.

cd /d %~dp0

echo Docker Compose 서비스를 중지합니다...
docker-compose down

echo.
echo ========================================
echo  모든 서비스가 중지되었습니다!
echo ========================================
pause

