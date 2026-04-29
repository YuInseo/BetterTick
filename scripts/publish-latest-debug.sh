#!/usr/bin/env bash
# PC 에서 debug APK 를 빌드해 GitHub Release 의 latest-debug 태그에 덮어쓴다.
# 결과적으로 CI 에서 푸시하든 여기서 푸시하든 앱은 동일한 매니페스트를 보고
# 자동 업데이트한다.
#
# 사전 요구:
#   - gh CLI 설치 후 `gh auth login` 으로 인증 완료 (push 권한 토큰 필요)
#   - 작업 디렉토리가 BetterTick 프로젝트 루트
#
# 사용:
#   ./scripts/publish-latest-debug.sh

set -euo pipefail

REPO="${REPO:-yuinseo/bettertick}"
TAG="latest-debug"

cd "$(dirname "$0")/.."

if ! command -v gh >/dev/null 2>&1; then
  echo "gh CLI 가 PATH 에 없음. https://cli.github.com 에서 설치 후 'gh auth login'." >&2
  exit 1
fi

# CI 와 동일 로직으로 버전 메타데이터 산출.
CODE="$(git rev-list --count HEAD)"
NAME="1.0.${CODE}"
SHA="$(git rev-parse --short HEAD)"
echo "[publish] versionCode=$CODE versionName=$NAME sha=$SHA repo=$REPO"

# Build. lint/test 는 시간 절약 차 스킵 — CI 가 PR 단계에서 별도로 돌린다.
./gradlew assembleDebug -x lint -x test \
  -PappVersionCode="$CODE" \
  -PappVersionName="$NAME" \
  -PappGitSha="$SHA"

APK_SRC="app/build/outputs/apk/debug/app-debug.apk"
if [ ! -f "$APK_SRC" ]; then
  echo "[ERROR] APK 가 안 보임: $APK_SRC" >&2
  exit 1
fi

mkdir -p .release-staging
APK_OUT=".release-staging/BetterTick.apk"
JSON_OUT=".release-staging/version.json"
cp -f "$APK_SRC" "$APK_OUT"
cat > "$JSON_OUT" <<EOF
{
  "versionCode": ${CODE},
  "versionName": "${NAME}",
  "sha": "${SHA}",
  "apkUrl": "https://github.com/${REPO}/releases/download/${TAG}/BetterTick.apk",
  "notes": "Local build ${SHA}"
}
EOF

# 동일 태그 릴리스를 깔끔히 덮어쓰려면 삭제 후 재생성. 어셋만 갈아끼우는
# 방법보다 안정적 (오래된 매니페스트가 남는 경우 방지).
if gh release view "$TAG" --repo "$REPO" >/dev/null 2>&1; then
  gh release delete "$TAG" --repo "$REPO" --yes
fi

gh release create "$TAG" \
  "$APK_OUT" \
  "$JSON_OUT" \
  --repo "$REPO" \
  --title "Latest Debug Build" \
  --notes "Local build ${SHA} (${NAME})" \
  --prerelease

echo "[publish] 완료 — ${REPO} 의 ${TAG} 에 BetterTick.apk + version.json 게시"
