@echo off
chcp 65001 > nul
setlocal

echo ========================================
echo   프로필별 빌드 및 테스트
echo ========================================
echo.

echo === 1단계: Development 환경 빌드 ===
call mvn-dev-build.bat
if %ERRORLEVEL% NEQ 0 (
    echo Development 빌드 실패!
    exit /b 1
)
echo.

echo === 2단계: Development 환경 Tomcat 배포 ===
call deploy-tomcat.bat
timeout /t 25 /nobreak > nul
echo.

echo === 3단계: Development 환경 테스트 ===
curl -s -o nul -w "API Health: %%{http_code}\n" http://localhost:8080/api/health
curl -s -o nul -w "Swagger UI: %%{http_code}\n" http://localhost:8080/api/swagger-ui.html
curl -s -o nul -w "Swagger API Docs: %%{http_code} (Should be 200)\n" http://localhost:8080/api/v2/api-docs
echo.
echo Development 환경: Swagger가 활성화되어야 합니다.
echo.
pause

echo === 4단계: Production 환경 빌드 ===
call mvn-prod-build.bat
if %ERRORLEVEL% NEQ 0 (
    echo Production 빌드 실패!
    exit /b 1
)
echo.

echo === 5단계: Production 환경 Tomcat 배포 ===
call deploy-tomcat.bat
timeout /t 25 /nobreak > nul
echo.

echo === 6단계: Production 환경 테스트 ===
curl -s -o nul -w "API Health: %%{http_code}\n" http://localhost:8080/api/health
curl -s -o nul -w "Swagger UI: %%{http_code}\n" http://localhost:8080/api/swagger-ui.html
curl -s -o nul -w "Swagger API Docs: %%{http_code} (Should be 404)\n" http://localhost:8080/api/v2/api-docs
echo.
echo Production 환경: Swagger가 비활성화되어야 합니다.
echo.

echo ========================================
echo   테스트 완료!
echo ========================================
echo.
echo Development 환경:
echo   - mvn-dev-build.bat로 빌드
echo   - Swagger 활성화 (v2/api-docs = 200)
echo.
echo Production 환경:
echo   - mvn-prod-build.bat로 빌드
echo   - Swagger 비활성화 (v2/api-docs = 404)
echo.
echo ========================================

endlocal

