@echo off

:MONITOR
timeout /nobreak /t 1 >nul
tasklist /FI "WINDOWTITLE eq start.bat" | find /i "cmd.exe" >nul

if %errorlevel% equ 0 (
    echo Server is running.
) else (
    echo Server is not running. Restarting...
    call start.bat
)

goto MONITOR
