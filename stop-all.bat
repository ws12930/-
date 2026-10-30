@echo off
cd /d "%~dp0"
title 7h 博客 - 停止服务

echo.
echo   正在停止 7h 博客服务...

rem findstr 用 /c:":端口 " 带尾随空格精确匹配，避免误杀 18081/80810 等相近端口
for /f "tokens=5" %%P in ('netstat -ano -p TCP ^| findstr /c:":8081 " ^| findstr "LISTENING"') do (
    echo   关闭后端 PID %%P
    taskkill /F /PID %%P >nul 2>nul
)

for /f "tokens=5" %%P in ('netstat -ano -p TCP ^| findstr /c:":4173 " ^| findstr "LISTENING"') do (
    echo   关闭前端 PID %%P
    taskkill /F /PID %%P >nul 2>nul
)

echo   完成。
echo.
pause
