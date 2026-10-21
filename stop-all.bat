@echo off
cd /d "%~dp0"
title 7h 博客 - 停止服务

echo.
echo   正在停止 7h 博客服务...

for /f "tokens=5" %%P in ('netstat -ano -p TCP ^| findstr ":8081" ^| findstr "LISTENING"') do (
    echo   关闭后端 PID %%P
    taskkill /F /PID %%P >nul 2>nul
)

for /f "tokens=5" %%P in ('netstat -ano -p TCP ^| findstr ":4173" ^| findstr "LISTENING"') do (
    echo   关闭前端 PID %%P
    taskkill /F /PID %%P >nul 2>nul
)

echo   完成。
echo.
pause
