@echo off
cd /d "%~dp0"
title 7h 博客启动器

set "PY="
if exist "%USERPROFILE%\.workbuddyinaries\pythonersions.13.12\python.exe" set "PY=%USERPROFILE%\.workbuddyinaries\pythonersions.13.12\python.exe"
if not defined PY if exist "%LOCALAPPDATA%\Programs\Python\Python312\python.exe" set "PY=%LOCALAPPDATA%\Programs\Python\Python312\python.exe"
if not defined PY (
    where python >nul 2>nul
    if not errorlevel 1 set "PY=python"
)

if not defined PY (
    echo.
    echo   [错误] 没有找到 Python 3，请先安装并把 python 加入 PATH
    echo.
    pause
    exit /b 1
)

echo   使用 Python: %PY%
"%PY%" "%~dp0start.py"
echo.
pause
