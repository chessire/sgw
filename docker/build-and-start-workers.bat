@echo off
chcp 65001 > nul
echo ========================================
echo   Building and Starting 3 Workers
echo ========================================
echo.

cd ..

echo Step 1: Building Worker JAR...
call mvn clean package -P development -DskipTests -q -pl app/worker -am
if %ERRORLEVEL% neq 0 (
    echo ❌ Build failed!
    pause
    exit /b 1
)
echo ✅ Build success!
echo.

echo Step 2: Building Docker images...
cd docker
docker-compose -f docker-compose.three-workers.yml build
if %ERRORLEVEL% neq 0 (
    echo ❌ Docker build failed!
    pause
    exit /b 1
)
echo ✅ Docker build success!
echo.

echo Step 3: Starting 3 Workers with Docker Compose...
docker-compose -f docker-compose.three-workers.yml up -d
if %ERRORLEVEL% neq 0 (
    echo ❌ Docker start failed!
    pause
    exit /b 1
)
echo.

echo ========================================
echo   3 Workers Started Successfully!
echo ========================================
echo.
echo Worker containers:
docker ps --filter "name=worker-" --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}"
echo.
echo View logs:
echo   docker logs -f worker-1
echo   docker logs -f worker-2
echo   docker logs -f worker-3
echo.
echo View all logs together:
echo   docker-compose -f docker-compose.three-workers.yml logs -f
echo.
echo Stop workers:
echo   docker-compose -f docker-compose.three-workers.yml down
echo.
pause

