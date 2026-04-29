@echo off
chcp 65001 > nul
setlocal enabledelayedexpansion

REM =====================================================
REM  BetterTick - Push to main → 자동 debug APK 빌드 → latest-debug Release
REM
REM  사용: 더블클릭, 또는 cmd창에서 release.bat
REM  (레거시 로컬빌드+Empty업로드 흐름은 release-local.bat 사용)
REM =====================================================

cd /d "%~dp0"
echo.
echo ==================================================
echo  BetterTick — Push to main (auto debug release)
echo ==================================================
echo.

REM ---- 1. stale lock 정리 ----
if exist ".git\index.lock" (
    echo [1/5] Removing stale .git\index.lock ...
    del /F /Q ".git\index.lock" 2>nul
) else (
    echo [1/5] No stale lock - OK
)
echo.

REM ---- 2. 변경사항 확인 ----
echo [2/5] Current git status:
git status --short
echo.

REM ---- 3. 파일 스테이징 ----
echo [3/5] Staging files...
git add .github/workflows/build-apk.yml 2>nul
git add RELEASE.md 2>nul
git add release.bat 2>nul
git add release-local.bat 2>nul
echo.

REM ---- 4. 커밋 ----
echo [4/5] Committing...
git diff --cached --quiet
if errorlevel 1 (
    git commit -m "ci: add auto debug APK workflow (latest-debug release)"
    if errorlevel 1 (
        echo.
        echo [ERROR] git commit failed.
        pause
        exit /b 1
    )
) else (
    echo Nothing new to commit - skipping commit.
)
echo.

REM ---- 5. 푸시 ----
echo [5/5] Pushing to origin/main ...
git push origin main
if errorlevel 1 (
    echo.
    echo [ERROR] git push failed. Check credentials / network.
    pause
    exit /b 1
)

echo.
echo ==================================================
echo  Push 완료! GitHub Actions가 빌드를 시작했습니다.
echo ==================================================
echo.
echo  진행 상황:
echo    https://github.com/YuInseo/BetterTick/actions
echo.
echo  APK 다운로드 (5~8분 후):
echo    https://github.com/YuInseo/BetterTick/releases/download/latest-debug/BetterTick.apk
echo.
echo  *이 링크는 항상 최신 빌드를 가리킵니다 — 즐겨찾기 추천*
echo.
pause
