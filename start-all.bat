@echo off
cd /d "%~dp0"
title 7h 博客启动器

rem cmd 本地启动法：按顺序探测真实可用的 Python
rem 每个候选都会实际运行 --version 验证，自动过滤微软商店的占位 python
set "PY="

where py >nul 2>nul
if not errorlevel 1 (
    py -3 --version >nul 2>nul
    if not errorlevel 1 set "PY=py -3"
)

if not defined PY if exist "%USERPROFILE%\.workbuddy\binaries\python\versions\3.13.12\python.exe" set "PY=%USERPROFILE%\.workbuddy\binaries\python\versions\3.13.12\python.exe"

if not defined PY if exist "%LOCALAPPDATA%\Programs\Python\Python312\python.exe" set "PY=%LOCALAPPDATA%\Programs\Python\Python312\python.exe"

if not defined PY (
    where python >nul 2>nul
    if not errorlevel 1 (
        python --version >nul 2>nul
        if not errorlevel 1 set "PY=python"
    )
)

if not defined PY (
    echo.
    echo   [错误] 没有找到可用的 Python 3，请先安装并把 python 加入 PATH
    echo.
    pause
    exit /b 1
)

echo   使用 Python: %PY%
%PY% "%~dp0start.py"
echo.
echo   启动器已退出。
pause
