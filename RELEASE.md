# BetterTick — 릴리스 가이드

두 가지 릴리스 경로가 준비돼 있어요. **개발/공유용**은 자동, **공식 배포용**은 태그.

---

## 🟢 경로 A — Debug APK 자동 빌드 (개발/공유용, 권장)

`main`에 push하면 자동으로 debug APK가 빌드돼서 `latest-debug`라는 고정 GitHub Release에 덮어쓰기됩니다. **서명 키도, Secrets도, 태그도 필요 없어요.** 그냥 push만 하면 끝.

### 사용법

```bash
git push origin main
```

→ 약 5–8분 후 자동 완료.

### 결과 다운로드 (안정 URL)

```
https://github.com/YuInseo/BetterTick/releases/download/latest-debug/BetterTick.apk
```

이 URL은 **항상 최신 빌드**를 가리킵니다. 친구한테 공유하거나 자신의 안드로이드폰에서 바로 다운받기 좋아요.

### 부가 기능

- **빌드 로그**: Actions 탭 / 또는 `build-logs` 브랜치의 `runs/{sha}.log`에 모든 빌드 로그 자동 저장
- **빌드 실패 시**: GitHub Issue가 자동 생성됨 (마지막 50KB 로그 포함)
- **버전 메타**: `version.json`에 versionCode/versionName/SHA가 함께 첨부

### 워크플로우 파일

`.github/workflows/build-apk.yml`

---

## 🟠 경로 B — 서명된 Release APK (공식 배포용)

Play Store/공식 배포 시점이 오면 사용. 키스토어로 서명된 release APK를 만들고 `v0.1.2` 같은 semver 태그를 붙입니다.

### 최초 1회 — Secrets 등록

> Repo → **Settings → Secrets and variables → Actions → New repository secret**

| Name | Value |
|---|---|
| `KEYSTORE_BASE64` | keystore 파일을 base64 인코딩한 문자열 |
| `STORE_PASSWORD` | keystore의 store 비밀번호 |
| `KEY_ALIAS` | 키 alias |
| `KEY_PASSWORD` | 키 비밀번호 |

**keystore base64 인코딩 (PowerShell):**

```powershell
[Convert]::ToBase64String([IO.File]::ReadAllBytes("C:\path\to\bettertick.keystore")) | Set-Clipboard
```

### 사용법

```bash
# 1) 버전 올리기 (app/build.gradle.kts)
#    versionCode = 4
#    versionName = "0.1.3"
git commit -am "chore: bump to 0.1.3"

# 2) 태그 → 푸시 (한 줄)
git tag v0.1.3
git push origin main --follow-tags
```

→ 서명된 APK가 GitHub Releases에 자동 업로드됨.

### 워크플로우 파일

`.github/workflows/release.yml`

---

## 🔵 경로 C — PR 검증 (자동)

PR 올리거나 main에 push할 때 lint + debug 빌드가 검증됩니다.

### 워크플로우 파일

`.github/workflows/ci.yml`

---

## 워크플로우 한눈에 보기

| 파일 | 트리거 | 결과물 | 용도 |
|---|---|---|---|
| `build-apk.yml` | push to main | `latest-debug` Release에 APK | 일상 개발/공유 |
| `ci.yml` | PR / push | Lint 리포트 + Debug APK (artifact, 14일) | PR 검증 |
| `release.yml` | `v*.*.*` 태그 push | 서명된 Release APK | 공식 배포 |

---

## 트러블슈팅

| 증상 | 해결 |
|---|---|
| `latest-debug` Release에 빌드가 안 올라옴 | Actions 탭에서 빨간 X 클릭 → 어느 단계에서 실패했는지 확인 |
| 빌드 실패 issue가 자꾸 생성됨 | 빌드 깨진 채로 push 계속 중 — 고치고 push하면 자동으로 그칩니다 |
| `release.yml`에서 keystore 디코드 실패 | base64 문자열 앞뒤 공백/줄바꿈 제거. PowerShell `[Convert]::ToBase64String(...)` 권장 |
| `release.yml`에서 versionCode 충돌 | versionCode는 항상 이전 릴리스보다 큰 정수여야 함 |
| `latest-debug` 태그 / Release를 지우고 싶음 | Releases 페이지 → Delete release. 다음 push 때 자동 재생성 |

---

## 보안 체크

- ✅ `keystore.properties`, `*.keystore`, `*.jks` 모두 `.gitignore`로 차단
- ✅ `local.properties` 차단
- ⚠️ `app/google-services.json` 현재 커밋됨 — Firebase 보안 규칙으로 보호되므로 일반적으로 OK. 민감 판단 시 별도 Secret 권장
