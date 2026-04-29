# Bump version, build debug APK, publish to YuInseo/Empty releases.
# Users on the same app pick it up via AppUpdater on next launch.

$ErrorActionPreference = 'Stop'
$gradleFile = 'app\build.gradle.kts'
$repo       = 'YuInseo/Empty'

# Bump versionName patch + versionCode.
$content = Get-Content $gradleFile -Raw
$nameMatch = [regex]::Match($content, 'versionName\s*=\s*"([^"]+)"')
$codeMatch = [regex]::Match($content, 'versionCode\s*=\s*(\d+)')
if (-not $nameMatch.Success -or -not $codeMatch.Success) {
    Write-Host '[ERROR] versionName/versionCode not found in build.gradle.kts' -ForegroundColor Red
    exit 1
}

$parts = $nameMatch.Groups[1].Value -split '\.'
while ($parts.Count -lt 3) { $parts += '0' }
$parts[2] = [int]$parts[2] + 1
$newName = ($parts[0..2]) -join '.'
$newCode = [int]$codeMatch.Groups[1].Value + 1

$content = $content -replace 'versionName\s*=\s*"[^"]+"', "versionName = `"$newName`""
$content = $content -replace 'versionCode\s*=\s*\d+',   "versionCode = $newCode"
Set-Content -Path $gradleFile -Value $content -Encoding UTF8 -NoNewline
Write-Host "Bumped to $newName (code $newCode)"

# Build.
Write-Host 'Building debug APK...'
& .\gradlew.bat --daemon assembleDebug
if ($LASTEXITCODE -ne 0) {
    Write-Host '[ERROR] Build failed' -ForegroundColor Red
    exit 1
}

$apk = 'app\build\outputs\apk\debug\app-debug.apk'
if (-not (Test-Path $apk)) {
    Write-Host "[ERROR] APK not found: $apk" -ForegroundColor Red
    exit 1
}
$asset = "bettertick-$newName.apk"
$tag   = "v$newName"

# Probe release existence via the API exit code alone — never read its
# stderr. PS 5.1 wraps native stderr into an ErrorRecord which flips `$?`
# to false even on a clean exit, so redirecting here would break the flow.
& gh api "repos/$repo/releases/tags/$tag" --silent *> $null
if ($LASTEXITCODE -eq 0) {
    & gh release delete $tag --repo $repo --yes --cleanup-tag *> $null
}
$global:LASTEXITCODE = 0

& gh release create $tag "$apk#$asset" --repo $repo --title $tag --notes "auto-release"
if ($LASTEXITCODE -ne 0) {
    Write-Host '[ERROR] Release upload failed' -ForegroundColor Red
    exit 1
}

Write-Host "Released $tag to $repo" -ForegroundColor Green
