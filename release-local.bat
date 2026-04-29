@echo off
REM 레거시 - 기존의 로컬 빌드 + YuInseo/Empty repo로 APK 업로드 스크립트
REM (release.ps1 호출). AppUpdater 배포 흐름이 필요하면 이걸 사용.
powershell -ExecutionPolicy Bypass -File "%~dp0release.ps1" %*
pause
