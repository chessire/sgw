@echo off
echo ========================================
echo  Docker Infra 시작
echo ========================================
echo.

cd /d %~dp0

echo Docker Compose로 서비스를 시작합니다...
docker-compose up -d

echo.
echo 서비스 상태 확인 중...
timeout /t 5 /nobreak > nul
docker-compose ps

echo.
echo ========================================
echo  서비스가 시작되었습니다!
echo ========================================
echo.
echo Redis:             localhost:6379
echo Kafka:             localhost:9092
echo Kafka UI:          http://localhost:8090
echo Redis Commander:   http://localhost:8081
echo.
echo 로그 확인: docker-compose logs -f
echo 중지:     docker-compose down
echo ========================================
pause

