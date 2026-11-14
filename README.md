# BetterTick

**교대 근무자를 위한 할일 · 달력 · 가계부 앱.**
주간 · 야간 · 비번 · 휴무가 도는 근무표를 중심에 두고, 그 위에 할일과 수면 · 운동 같은 하루 루틴, 휴가, 월급까지 한곳에서 관리합니다.

Kotlin · Jetpack Compose로 만든 안드로이드 앱이고, Firebase에 동기화되면서도 오프라인에서 먼저 동작합니다.

```
최신 APK (로그인 없이 바로 설치)
https://github.com/YuInseo/BetterTick/releases/download/latest-debug/BetterTick.apk
```

설치한 뒤에는 앱이 새 빌드를 스스로 확인해 업데이트합니다.

---

## 목차

- [무엇을 할 수 있나](#무엇을-할-수-있나)
- [화면 구성](#화면-구성)
- [동기화와 데이터](#동기화와-데이터)
- [기술 스택](#기술-스택)
- [프로젝트 구조](#프로젝트-구조)
- [직접 빌드하기](#직접-빌드하기)
- [배포와 자동 업데이트](#배포와-자동-업데이트)
- [개발 규칙](#개발-규칙)
- [관련 문서](#관련-문서)

---

## 무엇을 할 수 있나

### 근무와 휴가

- **근무 블록**: 주간 · 야간 · 비번 · 휴무 · 당직 · 일근을 색 블록으로 표시합니다. 날짜마다 다른 근무로 바꿀 수 있고, 근무 블록은 그날 목록에서 항상 맨 위에 옵니다.
- **근무 패턴 반복**: 며칠 치 근무를 입력하면 패턴을 찾아 이어서 반복해 줍니다.
- **블록 색상**: 근무 종류마다 색을 고를 수 있고, 컬러 피커로 만든 색은 앱 전체의 색 목록에 함께 들어갑니다.
- **휴가 (지휴 · 특휴 · 연차 · 병가)**
  - 휴가마다 주기(매달 · 매년 · 근무 시작일 기준 · 리셋 없음)와 일수, 연도별 · 근속 연차별 일수를 정합니다.
  - 지휴는 빨간날(공휴일 · 대체공휴일, 선택으로 일요일 · 토요일)에 근무한 만큼 자동으로 쌓이고, 근무 종류별 가중치(예: 야간 2일)를 줄 수 있습니다.
  - 어느 기간이든 총 일수를 직접 입력해 자동 계산을 덮어쓸 수 있습니다.
  - 근무 블록에서 바로 지휴 · 병가 등을 켜고 끄며, 남은 일수가 함께 보입니다.
- **근무지(회사)별 규칙과 기록**: 회사마다 휴가 규칙과 근무 기간을 따로 두고, 날짜가 속한 회사의 규칙으로 계산합니다. 회사별로 쓴 휴가 기록을 볼 수 있습니다.

### 할일

- 목록 · 태그 · 우선순위 · 고정 · 알림 · 반복(매일 · 매주 · 매달 · 매년 · 사용자 지정 · 근무 패턴)을 지원합니다.
- **하위 할일**: 할일을 다른 할일 위로 끌어다 놓으면 하위 할일이 됩니다. 여러 단계로 넣을 수 있고, 하위 할일은 부모의 날짜 · 반복 · 건너뛴 날을 따르면서 자기 시간과 길이는 유지합니다. 그래서 근무 블록 아래 "수면 · 운동" 같은 루틴이 근무와 함께 반복됩니다. 밖으로 끌어내거나 메뉴에서 분리할 수 있습니다.
- **반복 할일은 날짜별 체크**: 그날 것만 완료됩니다. 할일마다 "반복 전체 한 번에" 방식으로 바꿀 수도 있습니다.
- **메모장**: 설명 아래에 + 버튼으로 메모를 쌓습니다. 반복 할일은 메모마다 공통(모든 반복)과 그날만을 고를 수 있습니다.
- 완료 · 취소됨(하지 않음) · 내일로 미루기, 삭제 후 실행 취소, CSV 가져오기 · 내보내기를 지원합니다.

### 달력

- 월 · 주 · 일 · 목록 · 연도 보기를 제공합니다. 월 보기는 위아래로 이어서 스크롤되고, 날짜를 누르면 그 주와 그날 할일 패널이 열립니다.
- 날짜 사이로 할일을 끌어 옮기고, 타임라인에서 끌어서 새 일정을 만듭니다.
- 한국 공휴일 · 대체공휴일을 표시하고, 빨간날을 직접 고칠 수 있습니다.
- 왼쪽 위 년 · 월을 누르면 오늘로 돌아옵니다.

### 가계부

- **월급 (근무지별)**: 월급날, 지급 · 공제 항목을 정합니다. 항목은 매달(기간 지정 가능) · 분기마다 · 매년 정한 달 · 한 번만 중에서 고르고, 달별 총액을 직접 넣을 수도 있습니다. 분기 · 연간 합계를 미리 볼 수 있습니다.
- **군 복무 월급 (선택)**: 입대일만 넣으면 계급별 월급을 달마다 계산합니다. 복무 형태(육군 18 · 해군 20 · 공군 · 사회복무요원 21개월)를 고를 수 있고, 계급별 기간과 월급은 모두 직접 고칠 수 있습니다. 입대한 달과 전역한 달은 일할로 계산합니다.
- **수입 · 지출**: 용돈 · 부수입 · 식비 등을 한 번 또는 매달 반복으로 빠르게 추가합니다. 달마다 "월급 + 추가 수입 − 지출 = 남은 돈", 지출 분류별 막대, 내역을 보여 줍니다.

### 그 밖의 탭

- **습관**: 매일 체크하는 습관, 기록 히트맵과 통계.
- **뽀모도로**: 포모 타이머 · 스톱워치, 집중 기록과 분포.
- **일기**: 날짜별 일기.
- **동선**: 하루 위치 기록, 카카오맵 표시.
- **아이젠하워 매트릭스**: 중요도 · 긴급도 4분면.

### 위젯 · 알림 · 기타

- **위젯**: 일정 · 할일 위젯(주간 날짜, 체크박스, 하위 할일 들여쓰기), 주간 달력, 빠른 추가, 일기, 집중 분포.
- **알림**: 할일 · 습관 알림과 하루 요약. 알림에서 바로 완료하거나 10분 뒤 다시 알림을 받을 수 있습니다.
- 잠금화면 빠른 입력, 플로팅 빠른 추가, 시간 형식(12시간 · 24시간 · 24시간 두 자리 08:00), 주 시작 요일, 테마와 강조색, 탭바 구성(가계부 포함)을 고를 수 있습니다.

---

## 화면 구성

하단 탭바는 아이콘만 표시하고, 설정 › 탭바에서 탭 순서와 개수를 바꿀 수 있습니다.

| 탭 | 내용 |
| --- | --- |
| 할 일 | 목록 · 필터 · 스마트 목록, 오늘 · 기한 지남 |
| 달력 | 월 · 주 · 일 · 목록 · 연도 보기, 날짜 패널, 스케줄 보기(남은 휴가) |
| 가계부 | 월급 · 군 월급 · 수입 · 지출 · 남은 돈 |
| 설정 | 일반 · 근무지 · 휴가 · 월급 · 날짜 & 시간 · 알림 · 테마 · 탭바 · 계정 |
| (선택) | 습관 · 뽀모도로 · 일기 · 동선 · 매트릭스 |

---

## 동기화와 데이터

BetterTick은 **기기 안 캐시를 먼저 쓰는** 방식으로 동작합니다.

- **읽기**: 화면은 Firestore의 로컬 캐시만 봅니다(`ListenSource.CACHE`). 화면을 보는 동안에는 서버 읽기가 생기지 않고, 네트워크가 없어도 바로 열립니다.
- **쓰기**: 먼저 로컬 캐시에 반영되어 화면이 즉시 바뀌고, Firestore가 온라인일 때 서버로 보냅니다.
- **다른 기기에서 고친 내용 가져오기**: 15분마다 도는 백그라운드 작업이 처리합니다(`WidgetUpdateWorker` → `TaskRepository.refreshFromServer`). 평소에는 `updatedAt`이 바뀐 할일만 서버에서 읽고, 하루 한 번만 전체를 다시 읽어 지워진 항목을 정리합니다. 기기 간 시계 차이를 감안해 10분 여유를 둡니다.
- **기기에만 저장되는 설정**: 휴가 규칙 · 근무지 · 월급 · 가계부 · 화면 설정 등은 SharedPreferences에 둡니다.

Firestore 구조는 [`FIRESTORE_SCHEMA.md`](FIRESTORE_SCHEMA.md)에 있습니다. 할일은 `users/{uid}/tasks/{taskId}` 아래에 있고, 주요 필드는 다음과 같습니다.

| 필드 | 뜻 |
| --- | --- |
| `parentId` | 하위 할일이면 부모 할일 id |
| `completedDates` | 반복 할일의 날짜별 완료 (`yyyy-MM-dd`) |
| `checkWholeSeries` | 체크 한 번에 반복 전체를 완료할지 |
| `memos` | 메모장 메모 (`text`, `day`: 비면 공통) |
| `leaves` / `dayShifts` | 근무 할일의 날짜별 휴가 · 근무 바꿈 |

---

## 기술 스택

| 영역 | 사용 |
| --- | --- |
| 언어 · UI | Kotlin 2.0, Jetpack Compose (Material 3) |
| 구조 | MVVM, Hilt(DI), Kotlin Coroutines / Flow |
| 데이터 | Firebase Auth · Firestore(영구 캐시 무제한), SharedPreferences + org.json |
| 위젯 · 백그라운드 | Glance AppWidget, WorkManager, AlarmManager |
| 지도 | Kakao Map SDK, Kakao Mobility 길찾기 API(선택) |
| 빌드 | AGP 8.5, Gradle Version Catalog, KSP |
| 최소 · 대상 SDK | minSdk 28 (Android 9) · targetSdk 34 |

---

## 프로젝트 구조

```
app/src/main/java/com/bettertick/
├── data/
│   ├── model/          # Task, 반복 · 하위 할일 · 근무 · 공휴일 계산 (TaskHierarchy, ShiftRotation, KoreanHolidays …)
│   ├── repository/     # Firestore 저장소 (캐시 우선 읽기, 낙관적 쓰기)
│   └── firebase/       # FirestoreProvider, 초기 데이터
├── ui/
│   ├── navigation/     # 하단 탭바, NavHost, 플로팅 버튼
│   ├── screens/
│   │   ├── tasks/      # 할일 목록 · 상세 · 입력, LeavePrefs · SalaryPrefs · MoneyPrefs
│   │   ├── calendar/   # 월 · 주 · 일 보기, 날짜 패널, 스케줄 보기
│   │   ├── money/      # 가계부 탭
│   │   ├── more/       # 설정 화면들 (근무지 · 휴가 · 월급 · 탭바 …)
│   │   └── habits/ focus/ diary/ location/ matrix/ …
│   └── theme/          # 색 · 글꼴 · 앱 설정(AppPrefs, UserColors)
├── widget/             # Glance 위젯, 위젯 갱신 작업
├── reminder/           # 알림 예약 · 수신
├── update/             # 앱 자동 업데이트
└── overlay/ location/  # 플로팅 빠른 추가, 위치 기록
```

단위 테스트는 `app/src/test/`에 있습니다. 하위 할일 트리, 휴가 계산, 월급 · 군 월급, 가계부, 날짜별 체크, 메모를 검사합니다.

---

## 직접 빌드하기

### 준비물

- Android Studio (JDK 17)
- Android SDK 34

### 실행

```bash
git clone https://github.com/YuInseo/BetterTick.git
cd BetterTick
./gradlew assembleDebug          # app/build/outputs/apk/debug/app-debug.apk
./gradlew testDebugUnitTest      # 단위 테스트
```

Android Studio에서 열고 `app`을 실행해도 됩니다.

### 선택 설정

- **Kakao 길찾기 키**: `local.properties`에 `KAKAO_REST_API_KEY=...`를 넣습니다([`local.properties.example`](local.properties.example)). 없어도 앱은 정상 동작하고, 동선의 지하철 구간만 직선으로 그립니다.
- **서명된 release 빌드**: [`keystore.properties.example`](keystore.properties.example)을 참고해 `keystore.properties`를 만듭니다. 자세한 절차는 [`RELEASE.md`](RELEASE.md)에 있습니다.
- **작은 테스트 APK**: `./gradlew assembleSmall`로 만듭니다. R8로 줄인 빌드이고, debug와 같은 서명을 씁니다.

---

## 배포와 자동 업데이트

```
PR ──▶ CI (Build & Lint: assembleDebug · lintDebug · testDebugUnitTest)
          │ 통과
          ▼
main 머지 ──▶ build-apk.yml ──▶ latest-debug 릴리즈 (BetterTick.apk + version.json)
                                         │
                                         ▼
                              앱이 version.json의 versionCode를 확인해 업데이트
```

- **CI** (`.github/workflows/ci.yml`): 모든 PR에서 빌드 · lint · 단위 테스트를 돌립니다. 새 커밋이 오면 이전 실행을 취소합니다.
- **배포** (`.github/workflows/build-apk.yml`): `main`에 머지되면 debug APK를 빌드해 `latest-debug` 릴리즈를 새로 만듭니다. APK를 다 올린 다음에 `version.json`을 올리므로, 앱은 다운로드할 수 있는 빌드만 보게 됩니다. 실패하면 로그와 함께 이슈가 생깁니다.
- **버전**: `versionCode`는 커밋 수이고, 표시 이름은 `0.<커밋 수 ÷ 100>.<나머지>`입니다(예: 203 → 0.2.3).

---

## 개발 규칙

이 저장소는 Claude Code가 함께 작업합니다. 규칙은 [`CLAUDE.md`](CLAUDE.md)에 있고, 요약하면 다음과 같습니다.

1. 작업 브랜치에 커밋하고 `main`으로 PR을 엽니다.
2. PR의 CI가 **현재 head 커밋에서 통과**하고 충돌이 없을 때만 squash 머지합니다. CI가 실패하면 고친 뒤 다시 확인합니다.
3. CI가 도는 PR에는 새 요청을 덧붙이지 않습니다. 앞 PR이 머지된 뒤 새 PR로 올립니다.

---

## 관련 문서

- [`RELEASE.md`](RELEASE.md): debug 자동 배포와 서명된 release 배포 절차
- [`FIRESTORE_SCHEMA.md`](FIRESTORE_SCHEMA.md): Firestore 컬렉션 구조
- [`CLAUDE.md`](CLAUDE.md): 작업 · 머지 규칙
- [`history/`](history): 기능별 구현 기록
