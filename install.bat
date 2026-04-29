@echo off
setlocal enabledelayedexpansion

:START
cls
echo ========================================
echo   BetterTick - USB Build and Install
echo ========================================
echo.

where adb >nul 2>&1
if %errorlevel% neq 0 (
    echo [ERROR] ADB not found. Add Android SDK platform-tools to PATH.
    goto RETRY
)

echo [1/4] Detecting USB device...
set DEVICE_ADDR=
for /f "skip=1 tokens=1,2" %%A in ('adb devices 2^>nul') do (
    if "%%B"=="device" if "!DEVICE_ADDR!"=="" (
        echo %%A | findstr ":" >nul 2>&1
        rem Skip wireless entries (ip:port or adb-*), pick only USB serials.
        if errorlevel 1 (
            echo %%A | findstr /b "adb-" >nul 2>&1
            if errorlevel 1 set DEVICE_ADDR=%%A
        )
    )
)

if "!DEVICE_ADDR!"=="" (
    echo.
    echo [ERROR] No USB device found.
    echo   1. Plug phone in with USB cable
    echo   2. Enable Developer options ^> USB debugging
    echo   3. Tap "Allow" on the phone when prompted
    goto RETRY
)
echo       Connected: !DEVICE_ADDR!
echo.

echo [2/4] Building... (first build may take a while)
call gradlew.bat --daemon assembleDebug
if %errorlevel% neq 0 (
    echo.
    echo [ERROR] Build failed! Check the error messages above.
    goto RETRY
)
echo       Build success!
echo.

set APK_PATH=app\build\outputs\apk\debug\app-debug.apk
if not exist "%APK_PATH%" (
    echo [ERROR] APK not found: %APK_PATH%
    goto RETRY
)

echo [3/4] Installing APK...
adb -s !DEVICE_ADDR! install -r "%APK_PATH%"
if %errorlevel% neq 0 (
    echo.
    echo [ERROR] Install failed!
    goto RETRY
)
echo       Install success!
echo.

echo [4/4] Launching app...
adb -s !DEVICE_ADDR! shell am start -n com.bettertick/.MainActivity
echo.
echo ========================================
echo   BetterTick installed and launched!
echo ========================================

:RETRY
echo.
echo Press any key to rebuild and reinstall...
pause >nul
goto START
