@echo off
echo ========================================
echo Stopping MCP Services...
echo ========================================

taskkill /F /FI "WINDOWTITLE eq Agent-Proxy-9000*" 2>nul
taskkill /F /FI "WINDOWTITLE eq Member-8201*" 2>nul
taskkill /F /FI "WINDOWTITLE eq Order-8202*" 2>nul
taskkill /F /FI "WINDOWTITLE eq TTS-8101*" 2>nul

echo.
echo All MCP services stopped.
echo ========================================
pause
