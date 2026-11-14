@echo off
chcp 65001 >nul
setlocal
powershell.exe -NoProfile -ExecutionPolicy Bypass -File "%~dp0课前准备数据库.ps1" %*
set "setupExitCode=%ERRORLEVEL%"
echo.
if "%setupExitCode%"=="0" (
    echo Database preparation finished. Review the output above.
) else (
    echo Database preparation failed. Review the error above.
)
pause
exit /b %setupExitCode%
