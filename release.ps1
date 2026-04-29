# PC 에서 debug APK 를 빌드해 GitHub Release 의 latest-debug 태그에 덮어쓴다.
# CI 의 auto-update.yml 과 동일한 매니페스트 형식을 사용해 앱 내 자동 업데이트
# 흐름과 호환된다.
#
# 사전 요구: gh CLI 설치 + `gh auth login` 으로 인증.

$ErrorActionPreference = 'Stop'

$Repo = if ($env:REPO) { $env:REPO } else { 'yuinseo/bettertick' }
$Tag  = 'latest-debug'

if (-not (Get-Command gh -ErrorAction SilentlyContinue)) {
    Write-Host '[ERROR] gh CLI 가 PATH 에 없음. https://cli.github.com 에서 설치 후 gh auth login.' -ForegroundColor Red
    exit 1
}

# CI 와 동일 로직으로 버전 메타데이터 산출.
$Code = (& git rev-list --count HEAD).Trim()
$Name = "1.0.$Code"
$Sha  = (& git rev-parse --short HEAD).Trim()
Write-Host "[publish] versionCode=$Code versionName=$Name sha=$Sha repo=$Repo"

& .\gradlew.bat --daemon assembleDebug -x lint -x test `
    "-PappVersionCode=$Code" `
    "-PappVersionName=$Name" `
    "-PappGitSha=$Sha"
if ($LASTEXITCODE -ne 0) { Write-Host '[ERROR] Build failed' -ForegroundColor Red; exit 1 }

$ApkSrc = 'app\build\outputs\apk\debug\app-debug.apk'
if (-not (Test-Path $ApkSrc)) {
    Write-Host "[ERROR] APK 가 안 보임: $ApkSrc" -ForegroundColor Red
    exit 1
}

$Stage   = '.release-staging'
$ApkOut  = Join-Path $Stage 'BetterTick.apk'
$JsonOut = Join-Path $Stage 'version.json'
New-Item -ItemType Directory -Force -Path $Stage | Out-Null
Copy-Item -Force $ApkSrc $ApkOut

$json = @"
{
  "versionCode": $Code,
  "versionName": "$Name",
  "sha": "$Sha",
  "apkUrl": "https://github.com/$Repo/releases/download/$Tag/BetterTick.apk",
  "notes": "Local build $Sha"
}
"@
Set-Content -Path $JsonOut -Value $json -Encoding UTF8 -NoNewline

# 같은 태그 릴리스가 있으면 삭제 후 재생성. 자산만 교체하면 매니페스트의
# 옛 버전이 남는 경우가 있어 더 안정적.
& gh api "repos/$Repo/releases/tags/$Tag" --silent *> $null
if ($LASTEXITCODE -eq 0) {
    & gh release delete $Tag --repo $Repo --yes *> $null
}
$global:LASTEXITCODE = 0

& gh release create $Tag $ApkOut $JsonOut `
    --repo $Repo `
    --title 'Latest Debug Build' `
    --notes "Local build $Sha ($Name)" `
    --prerelease
if ($LASTEXITCODE -ne 0) {
    Write-Host '[ERROR] Release upload failed' -ForegroundColor Red
    exit 1
}

Write-Host "[publish] 완료 — $Repo 의 $Tag 에 BetterTick.apk + version.json 게시" -ForegroundColor Green
