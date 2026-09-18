@echo off
cd /d "%~dp0"
title BLEACH x NARUTO 登录页 - 启动器

echo ============================================
echo   BLEACH x NARUTO 登录页  启动器
echo ============================================
echo.

where node >nul 2>nul
if errorlevel 1 (
    echo [错误] 未检测到 Node.js，请先安装：https://nodejs.org/
    echo.
    pause
    exit /b 1
)

for /f "tokens=*" %%v in ('node -v') do set NODE_VER=%%v
echo [OK] Node.js 版本：%NODE_VER%

if not exist "node_modules\" (
    echo.
    echo 首次启动，正在安装依赖（约需 1-2 分钟）...
    call npm install --no-fund --no-audit
    if errorlevel 1 (
        echo.
        echo [错误] 依赖安装失败，请检查网络后重试。
        pause
        exit /b 1
    )
    echo [OK] 依赖安装完成
)

echo.
echo 正在启动开发服务器...
echo 浏览器将自动打开 http://localhost:5173
echo 关闭此窗口即可停止服务。
echo.

call npm run dev

echo.
echo 服务已停止。
pause
