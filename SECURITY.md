# Firebase 보안과 배포

## 적용 기록 — 2026-11-14

- BetterTick 운영 Firestore 콘솔에 `firestore.rules`를 게시했고, 게시 성공을 확인했습니다. 게시 직전 규칙은 [`security/backups/firestore-before-20261114.rules`](security/backups/firestore-before-20261114.rules)에 보관합니다.
- 게시 전 규칙도 본인 UID로 접근을 제한하고 있었습니다. 이번 변경은 컬렉션 허용 목록과 쓰기 스키마 검증을 추가합니다.
- 에뮬레이터 보안 테스트 17개와 Android 빌드·Lint·단위 테스트가 통과했습니다. 실제 Android 기기에서 게시 후 로그인·동기화 검증은 아직 수행하지 않았습니다.
- Play Integrity에 현재 배포 인증서와 외부 배포 설정을 등록했습니다. 사용자 동의 후 약관에 동의하고 저장했으며 새로고침 후 `등록됨` 상태를 확인했습니다. 무료 Spark 요금제를 유지합니다. **App Check 강제 적용과 실제 기기 토큰 검증은 아직 완료되지 않았습니다.**
- API 키 제한 변경, 개발·운영 프로젝트 분리와 Release 서명 전환은 아직 수행하지 않았습니다.

## 동선 기록 기능 제거

앱의 동선 탭·설정·위치 권한·백그라운드 서비스·재부팅 자동 시작·지도 SDK를 제거했습니다. 기존 `location_tracking` 설정과 동선 시작 화면 설정도 정리합니다. 서버 규칙은 구버전 앱의 `locationRecords`·`favoritePlaces` 생성과 수정을 거부합니다. 기존 데이터는 자동 삭제하지 않으며 본인만 읽기·삭제할 수 있습니다. 새 규칙의 서버 적용은 별도로 게시 후 확인해야 합니다.

## 현재 코드에 포함된 보호 장치

- `firestore.rules`: 본인 UID의 명시된 컬렉션만 허용합니다. 다른 사용자·비로그인 접근, 전체 사용자 목록 조회, 알 수 없는 컬렉션과 중첩 경로는 거부합니다.
- 쓰기는 허용된 필드·타입·주요 문자열/목록/맵 크기·우선순위·좌표를 검사합니다. 예전 문서의 누락 필드, 기본 데이터의 빈 ID, Kotlin Boolean 필드명 별칭을 지원합니다.
- 가변 길이 목록·맵은 컨테이너 타입과 개수를 검사합니다. 모든 내부 원소의 스키마를 검사하는 것은 아니며, 이를 관리자 권한 판단에 사용하면 안 됩니다.
- 앱 시작 시 Play Integrity App Check를 등록합니다. 배포되는 Debug APK에도 우회용 Debug Provider를 포함하지 않습니다.
- PR과 main에서 격리된 에뮬레이터 보안 테스트와 서버 비밀키 패턴 검사를 실행합니다. 패턴 검사는 알려진 형식에 대한 회귀 방지 장치이며, 모든 비밀 정보를 발견한다고 보장하지 않습니다.
- Release 워크플로는 필수 서명 Secret이 없거나 공개 Debug 인증서를 사용하면 실패합니다. 복원한 키는 작업 종료 시 삭제합니다.

**저장소에 코드가 병합되어도 Firebase 서버의 보안 규칙·API 제한·App Check 강제 적용은 자동으로 바뀌지 않습니다.** 이 저장소에는 운영 프로젝트를 수정할 관리자 자격 증명이 없습니다.

## 1. 규칙 검증과 배포

Node.js 22와 Java 21 이상이 필요합니다.

```bash
cd security-tests
npm ci --ignore-scripts
npm test
```

테스트는 `demo-bettertick-security`만 사용하며 실제 프로젝트 데이터에 접근하지 않습니다. 익명·타 사용자 접근, 삭제와 쿼리, 권한 필드 주입, 잘못된 데이터, 기존 반복·휴가·메모 갱신을 검증합니다.

실제 프로젝트의 규칙을 배포하기 전 Firebase 콘솔의 현재 규칙을 별도 백업하고, 기존 문서에 허용 목록 밖의 필드나 제한을 넘는 데이터가 있는지 확인하세요. 읽기·삭제에는 스키마 제한을 걸지 않지만, 기존 문서를 수정할 때는 최종 문서 전체가 검증됩니다. 새 앱 버전에서 필드가 추가되면 앱을 배포하기 전에 규칙과 테스트를 함께 갱신해야 합니다.

인증된 개발 환경에서 저장소 루트 기준으로 배포합니다.

```bash
security-tests/node_modules/.bin/firebase login
security-tests/node_modules/.bin/firebase deploy --only firestore:rules --project YOUR_PROJECT_ID
```

대상 프로젝트는 명시적으로 선택합니다. 운영 프로젝트 ID를 기본값으로 저장하지 않습니다. 테스트 계정 두 개로 본인 데이터 생성·수정·조회와 상대방 데이터 거부를 확인한 뒤 운영에 적용하세요. 문제 시 백업한 기존 규칙으로 복구할 수 있습니다.

## 2. App Check 도입 순서

1. Play Console에서 Play Integrity API와 Firebase 프로젝트를 연결합니다.
2. Firebase App Check에 해당 Android 앱과 실제 배포 인증서의 SHA-256을 등록합니다.
3. GitHub APK만 배포하는 경우 외부 배포 설정을 사용합니다. 공식 안내의 `PLAY_RECOGNIZED: Not required`, `LICENSED: Not required`, `Device integrity` 조합을 확인하세요. 이 설정은 Play 배포의 앱 진위 검증과 같은 보호 수준을 의미하지 않습니다.
4. 처음에는 서버 강제를 켜지 않고 업데이트 앱의 요청 지표와 실제 기기 로그인·동기화를 확인합니다.
5. 기존 사용자가 업데이트하고 정상 토큰 비율이 확인된 뒤 Firestore 강제를 활성화합니다. 등록하지 않은 기존 앱은 강제 적용 후 서버 동기화가 차단됩니다.
6. 루팅 기기·에뮬레이터 등 지원 범위를 검토합니다. 개발 테스트는 별도 Firebase 프로젝트에서만 Debug Provider를 사용하고 Debug 토큰을 코드·로그·릴리즈에 공유하지 않습니다.

현재 공개 Debug 서명은 공식 앱을 신뢰하는 비밀 근거로 사용할 수 없습니다. App Check가 사용자별 데이터 권한 검사를 대체하지도 않습니다.

## 3. Firebase 프로젝트 분리

운영 데이터를 유지한 채 개발용 Firebase 프로젝트를 새로 만드세요. Android Google Services 플러그인은 source set별 설정을 지원하므로 개발 설정은 로컬 `app/src/debug/google-services.json`, 운영 설정은 `app/src/release/google-services.json`에 둘 수 있습니다.

Release CI에서는 `FIREBASE_RELEASE_CONFIG_BASE64` Secret을 설정하면 `app/src/release/google-services.json`로 복원합니다. 미설정 시 기존 `app/google-services.json`을 사용하므로 프로젝트 분리가 자동으로 완료되는 것은 아닙니다. 프로젝트별 로그인 공급자·OAuth 클라이언트·SHA 지문도 맞춰야 합니다.

현재 GitHub의 `latest-debug` APK는 기존 사용자 호환성을 위해 기존 프로젝트와 서명을 유지합니다. 별도 개발 프로젝트 설정을 넣은 빌드를 이 채널로 배포하지 마세요.

## 4. 비공개 Release 서명으로 전환

기존 `app/debug.keystore`는 공개 개발 키입니다. 이를 지워도 Git 기록과 기존 APK에서 회수할 수 없습니다. 파일을 삭제하는 것만으로 해결되지 않습니다.

개인 환경에서 비공개 Release 키를 만들고 안전한 장소에 백업한 뒤 GitHub Secrets에 `KEYSTORE_BASE64`, `STORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD`를 등록합니다. 키를 생성·백업하지 않은 채 CI에서 매번 새로 만들면 업데이트가 불가능해집니다.

현재 Debug 설치본과 새 Release APK는 서명이 달라 일반 덮어쓰기 설치가 안 될 수 있습니다. **현재 자동 업데이트 채널을 Release APK로 즉시 교체하지 마세요.** 전환 전에는 다음 항목을 처리해야 합니다.

- 기존 계정으로 할 일 등의 서버 동기화가 끝났는지 확인합니다.
- 회사·휴가·월급·가계부·로컬 첨부파일은 현재 서버 동기화 대상이 아니므로 별도 백업·복원 구현과 검증이 필요합니다. 이 작업 없이 삭제/재설치를 안내하지 마세요.
- 새 서명의 Firebase SHA-1/SHA-256과 Google 로그인 설정을 등록합니다.
- 서명 전환 및 로컬 데이터 이전을 실제 기기에서 검증한 뒤 배포 채널을 전환합니다. 지금 변경은 이를 자동 실행하지 않습니다.

## 5. 콘솔에서 확인할 설정

- Google Cloud의 API 키 허용 목록을 필요한 Firebase API로 제한합니다. Gemini 등 별도 유료 API와 키를 공유하지 않습니다.
- Firestore는 위의 사용자별 규칙을 실제로 게시했는지 확인합니다. Storage를 추가한다면 별도의 Storage 규칙도 필요합니다.
- 개발·운영 프로젝트의 관리자 권한은 필요한 계정에만 부여하고 관리자 계정에 2단계 인증을 적용합니다.
- 사용량과 비용 알림을 설정합니다. 예산 알림 자체는 비용 상한이나 자동 중단 기능이 아닙니다.

`google-services.json`과 Firebase 클라이언트 API 키는 공개 식별 정보입니다. APK에서 숨길 수 없으며, 서비스 계정 JSON·Admin SDK 비밀키·서명키와 구분해야 합니다. 관리자 자격 증명은 앱이나 Git에 포함하지 않습니다.

공식 문서:
- https://firebase.google.com/docs/projects/api-keys
- https://firebase.google.com/docs/app-check/android/play-integrity-provider
- https://firebase.google.com/docs/firestore/security/rules-conditions
- https://firebase.google.com/docs/rules/unit-tests

## 이전 APK 정리

- PR CI는 APK를 Actions 산출물로 업로드하지 않습니다. 빌드·Lint·테스트와 최종 위치 권한 검사는 유지합니다. 최신 설치 파일은 `latest-debug` 릴리즈에서만 제공합니다.
- `check-secrets.py`는 APK/AAB가 새 커밋에 들어오는 것도 거부합니다.
- `Purge historical APKs`는 소유자가 승인한 1회성 정리용입니다. 정확히 `maintenance/purge-historical-apks` 브랜치가 만들어질 때만 실행되며, 일반 푸시·PR에서는 실행하지 않습니다. 같은 이름의 브랜치를 임의로 만들지 마세요.
- 정리는 모든 브랜치·태그의 APK/AAB 파일을 기록에서 제거하고, 현재 코드 트리와 커밋 수가 보존되는지 검사합니다. 원격 참조가 작업 중 바뀌면 중단하고, 변경한 참조는 lease와 atomic push로 함께 갱신합니다. 강제 푸시 제한을 자동 해제하지 않습니다.
- 최신 Debug APK와 업데이트 정보를 보존하고, 다른 릴리즈의 APK/AAB 자산·APK Actions 산출물·옛 패키징 결과가 있을 수 있는 재생성 가능한 빌드 캐시를 정리합니다. 일반 코드·문서·Lint 보고서·빌드 로그는 삭제 대상이 아닙니다.
- 기록 재작성 후에는 기존 작업 복사본을 새로 clone하거나 정리된 기록 위로 작업을 옮겨야 합니다. 옛 기록을 merge/push하면 제거한 파일이 다시 들어올 수 있습니다.
- **브랜치·태그 기록에서 지우더라도 GitHub의 읽기 전용 PR 참조·캐시된 커밋 화면, 다른 사람의 포크·clone·다운로드는 완전히 회수할 수 없습니다.** Firebase 공개 클라이언트 키가 비밀이 되는 것도 아닙니다.
