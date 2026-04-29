@echo off
echo ========================================
echo   BetterTick - Clean Build and Install
echo ========================================
echo.
echo [1/2] Clean building...
call gradlew.bat clean assembleDebug
if %errorlevel% neq 0 (
    echo [ERROR] Build failed!
    pause
    exit /b 1
)
echo.
echo [2/2] Installing...
adb install -r app\build\outputs\apk\debug\app-debug.apk
if %errorlevel% neq 0 (
    echo [ERROR] Install failed!
    pause
    exit /b 1
)
adb shell am start -n com.bettertick/.MainActivity
echo.
echo   Done!
pause
