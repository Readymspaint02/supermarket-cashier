@echo off
echo ========================================
echo SmartMarket MCP Services
echo ========================================

cd /d "%~dp0"

echo Loading environment from .env.secrets...
for /f "tokens=1,2 delims==" %%a in (.env.secrets) do (
    set "%%a=%%b"
)

echo.
echo [1/4] Starting Agent Proxy (DeepSeek) - Port 9000
start "Agent-Proxy-9000" cmd /k "python Agent_HTTP_Proxy.py"
timeout /t 2 >nul

echo [2/4] Starting Member Service - Port 8201
start "Member-8201" cmd /k "python Member_Service_HTTP_test.py"
timeout /t 2 >nul

echo [3/4] Starting Order Service - Port 8202
start "Order-8202" cmd /k "python Order_Service_HTTP_test.py"
timeout /t 2 >nul

echo [4/4] Starting TTS Service - Port 8101
start "TTS-8101" cmd /k "python TTS_HTTP_Proxy.py"
timeout /t 3 >nul

echo.
echo ========================================
echo Checking services...
echo ========================================

curl -s http://localhost:9000/health >nul 2>&1
if %errorlevel%==0 (
    echo [OK] Agent Proxy:    http://localhost:9000
) else (
    echo [FAIL] Agent Proxy not started
)

curl -s http://localhost:8201/health >nul 2>&1
if %errorlevel%==0 (
    echo [OK] Member Service: http://localhost:8201
) else (
    echo [FAIL] Member Service not started
)

curl -s http://localhost:8202/health >nul 2>&1
if %errorlevel%==0 (
    echo [OK] Order Service:  http://localhost:8202
) else (
    echo [FAIL] Order Service not started
)

curl -s http://localhost:8101/health >nul 2>&1
if %errorlevel%==0 (
    echo [OK] TTS Service:    http://localhost:8101
) else (
    echo [FAIL] TTS Service not started
)

echo.
echo ========================================
echo MCP Services Started!
echo ========================================
echo.
echo Ports:
echo   9000  - Agent Proxy (DeepSeek)
echo   8201  - Member Service
echo   8202  - Order Service
echo   8101  - TTS Service
echo.
echo Test:
echo   curl -X POST http://localhost:9000/agent_query -H "Content-Type: application/json" -d "{\"query\":\"test\"}"
echo.
echo Frontend: http://localhost:5173
echo ========================================
pause
