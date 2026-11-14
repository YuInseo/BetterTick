<div align="center">

# BetterTick

### 근무에 맞춰, 일상을 더 편하게.

교대 근무부터 할 일, 휴가, 가계부까지.<br>
바뀌는 근무 일정에 맞춰 하루를 계획하는 Android 앱입니다.

<br>

![Android](https://img.shields.io/badge/Android_9%2B-3DDC84?style=flat-square&logo=android&logoColor=white)
![Kotlin](https://img.shields.io/badge/Kotlin-7F52FF?style=flat-square&logo=kotlin&logoColor=white)
![Jetpack Compose](https://img.shields.io/badge/Jetpack_Compose-4285F4?style=flat-square&logo=jetpackcompose&logoColor=white)
[![CI](https://github.com/YuInseo/BetterTick/actions/workflows/ci.yml/badge.svg)](https://github.com/YuInseo/BetterTick/actions/workflows/ci.yml)

<br>

**[APK 다운로드](https://github.com/YuInseo/BetterTick/releases/download/latest-debug/BetterTick.apk)** &nbsp; · &nbsp;
[릴리즈 보기](https://github.com/YuInseo/BetterTick/releases/tag/latest-debug) &nbsp; · &nbsp;
[버그 신고 · 기능 제안](https://github.com/YuInseo/BetterTick/issues)

</div>

<br>

## 나의 근무표에서 시작하는 하루

주간 근무가 있는 날과 야간 근무가 있는 날은 하루의 모습도 다릅니다. BetterTick은 근무표를 중심으로 일정과 생활을 함께 관리할 수 있도록 만들었습니다.

반복되는 근무 패턴을 등록하고, 근무에 맞는 수면이나 운동을 연결해 보세요. 남은 휴가와 이번 달 수입·지출도 같은 앱에서 확인할 수 있습니다.

| 근무와 휴가 | 할 일과 일정 | 생활과 기록 |
| :--- | :--- | :--- |
| 근무 패턴 반복 · 날짜별 변경 | 반복 할 일 · 하위 할 일 · 메모 | 회사별 월급 · 수입·지출 |
| 회사별 휴가 규칙 · 잔여 일수 | 월·주·일 달력 · 알림 · 위젯 | 습관 · 집중 타이머 · 일기 |

## 다운로드

[**최신 APK 받기 →**](https://github.com/YuInseo/BetterTick/releases/download/latest-debug/BetterTick.apk)

GitHub 로그인 없이 다운로드할 수 있습니다. 현재 배포 파일은 **Android 9 이상, ARM64 기기용 개발 빌드(Debug)**입니다.

1. Android 기기에서 APK를 다운로드합니다.
2. 파일을 열고, 필요한 경우 해당 브라우저나 파일 앱의 **알 수 없는 앱 설치**를 허용합니다.
3. 설치 후 앱을 실행합니다.

앱에 업데이트 확인 기능이 포함되어 있습니다. 설정에서 직접 확인할 수도 있으며, 기기 환경과 권한에 따라 설치 확인 화면이 나타날 수 있습니다.

## 주요 기능

### 🗓️ 근무표와 휴가를 함께

- **근무 유형** — 주간·야간·비번·휴무·당직·일근을 색상으로 구분합니다. 특정 날짜의 근무만 따로 변경할 수도 있습니다.
- **패턴 반복** — 등록한 근무를 바탕으로 반복 패턴을 적용해 이후 일정을 채웁니다.
- **근무별 루틴** — 근무 아래에 수면이나 운동을 하위 할 일로 연결하면 근무 패턴에 맞춰 함께 반복됩니다.
- **휴가 잔여량** — 지휴·특휴·연차·병가의 부여 일수와 사용 내역을 관리합니다.
- **유연한 부여 기준** — 매월·매년·입사일 기준·초기화 없음 중에서 선택하고, 연도나 근속 연차에 따라 부여 일수를 다르게 설정할 수 있습니다.
- **지휴 자동 계산** — 공휴일 근무와 근무 유형별 인정 일수를 기준으로 계산합니다. 토요일·일요일 포함 여부를 정하거나 총 일수를 직접 보정할 수도 있습니다.
- **회사별 관리** — 근무지와 근무 기간마다 휴가 규칙을 따로 적용하고 사용 기록을 확인합니다.

### ✅ 작은 할 일부터 반복되는 루틴까지

목록·태그·우선순위·고정으로 할 일을 정리하고, 필요한 시간에 알림을 받을 수 있습니다.

- 매일·매주·매월·매년 반복, 사용자 지정 반복, 근무 패턴 반복
- 드래그로 구성하는 여러 단계의 하위 할 일
- 날짜마다 따로 체크하는 반복 할 일과 반복 전체 완료 옵션
- 모든 반복에 남기는 메모와 특정 날짜에만 남기는 메모
- 완료·하지 않음·내일로 미루기, 삭제 직후 실행 취소
- CSV 가져오기·내보내기

### 📆 원하는 방식으로 보는 달력

월·주·일·목록·연도 보기로 일정을 살펴볼 수 있습니다. 날짜를 선택해 그날의 할 일을 확인하고, 할 일을 다른 날짜로 끌어 옮기거나 타임라인을 드래그해 새 일정을 만듭니다.

한국 공휴일과 대체공휴일을 표시하며, 휴일 정보는 직접 수정할 수도 있습니다.

### 💰 월급부터 생활비까지

| 기능 | 할 수 있는 일 |
| :--- | :--- |
| 회사별 월급 | 월급날, 지급·공제 항목, 적용 기간 설정 |
| 정기 지급 항목 | 매월·분기·연간·일회성 항목과 월별 실제 수령액 관리 |
| 수입·지출 | 일회성·매월 반복 내역 기록, 월별 잔액과 지출 분포 확인 |
| 군 복무 월급 | 필요할 때 활성화하고 입대일·복무 유형에 맞춰 계산 |

군 복무 월급은 계급별 기간과 금액을 직접 수정할 수 있으며, 입대·전역한 달은 일할 계산을 지원합니다.

### 🌱 일상을 채우는 도구

- **습관** — 매일 체크하고 기록 그래프와 통계로 돌아보기
- **집중** — 뽀모도로 타이머·스톱워치와 집중 기록
- **일기** — 날짜별로 하루의 기록 남기기
- **동선** — 하루 동안 방문한 장소를 기록하고 카카오맵으로 확인
- **아이젠하워 매트릭스** — 중요도와 긴급도에 따라 할 일 분류

### 🎨 나에게 맞는 화면

하단 탭의 순서와 개수를 바꾸고, 테마·강조색·근무 색상을 설정할 수 있습니다. 시간 표시 형식과 한 주의 시작 요일도 선택할 수 있습니다.

홈 화면에서는 일정·할 일·주간 달력·빠른 추가·일기·집중 분포 위젯을 사용할 수 있습니다. 할 일 알림에서는 바로 완료하거나 10분 뒤로 미루는 동작을 지원합니다.

## 데이터 저장과 동기화

BetterTick은 기기에 저장된 데이터를 먼저 보여 주는 방식으로 동작합니다. 캐시된 할 일은 오프라인에서도 확인할 수 있고, 변경 사항은 연결이 복구되면 서버에 반영됩니다.

| 데이터 | 저장 방식 |
| :--- | :--- |
| 할 일 | Firestore 로컬 캐시 우선 사용, 서버 동기화 지원 |
| 휴가 규칙·회사·월급·가계부·화면 설정 | 기기의 SharedPreferences에 저장 |

**기기에만 저장되는 항목은 Firestore를 통한 기기 간 동기화 대상에 포함되지 않습니다.**

<details>
<summary><strong>동기화 구조 자세히 보기</strong></summary>

화면의 할 일 조회에는 `ListenSource.CACHE`를 사용합니다. 다른 기기의 변경 사항은 `WidgetUpdateWorker`가 `TaskRepository.refreshFromServer()`를 호출해 받아옵니다.

- 백그라운드 작업은 15분 주기로 예약되며, 실제 실행 시점은 Android의 배터리·백그라운드 제한에 따라 달라집니다.
- 평소에는 `updatedAt` 기준으로 변경된 할 일을 조회합니다.
- 하루에 한 번 전체 데이터를 다시 조회해 다른 기기에서 삭제된 항목을 정리합니다.
- 기기 간 시간 차이를 고려해 변경 조회 구간에 10분의 여유를 둡니다.

컬렉션과 필드 구성은 [Firestore 스키마](FIRESTORE_SCHEMA.md)를 참고하세요.

</details>

## 개발 환경

| 구분 | 기술 |
| :--- | :--- |
| 언어·UI | Kotlin 2.0 · Jetpack Compose · Material 3 |
| 앱 구조 | MVVM · Hilt · Coroutines · Flow |
| 인증·데이터 | Firebase Auth · Cloud Firestore · SharedPreferences |
| 위젯·백그라운드 | Glance · WorkManager · AlarmManager |
| 지도 | Kakao Map SDK · 카카오모빌리티 길찾기 API |
| 빌드 | Gradle · AGP 8.5 · KSP · Version Catalog |
| Android | minSdk 28 · compileSdk / targetSdk 34 · JDK 17 |

### 로컬 실행

Android Studio와 JDK 17, Android SDK 34가 필요합니다.

```bash
git clone https://github.com/YuInseo/BetterTick.git
cd BetterTick
```

Android Studio에서 프로젝트를 열고 Gradle 동기화를 마친 뒤 `app`을 실행합니다. 터미널에서는 다음 명령을 사용할 수 있습니다.

```bash
# Debug APK 빌드
./gradlew assembleDebug

# 단위 테스트
./gradlew testDebugUnitTest

# Android Lint
./gradlew lintDebug
```

APK 생성 경로: `app/build/outputs/apk/debug/app-debug.apk`

Windows에서는 `./gradlew` 대신 `gradlew.bat`을 사용합니다. Firebase 프로젝트를 별도로 사용할 경우 `app/google-services.json`과 인증 설정도 해당 프로젝트에 맞게 구성해야 합니다.

<details>
<summary><strong>선택 설정과 추가 빌드</strong></summary>

**카카오 길찾기 API**

[local.properties.example](local.properties.example)을 참고해 `local.properties`에 키를 추가합니다.

```properties
KAKAO_REST_API_KEY=your_rest_api_key
```

키가 없으면 동선의 지하철 구간을 직선으로 표시합니다.

**용량을 줄인 테스트 APK**

```bash
./gradlew assembleSmall
```

R8을 적용한 테스트 빌드입니다. Debug와 같은 패키지·서명을 사용하며, 현재 자동 업데이트용 APK와는 별도로 배포됩니다.

**서명된 Release 빌드**

[keystore.properties.example](keystore.properties.example)과 [릴리즈 안내](RELEASE.md)를 참고하세요.

</details>

### 소스 구성

기본 경로는 `app/src/main/java/com/bettertick/`입니다.

| 경로 | 역할 |
| :--- | :--- |
| `data/model/` | 할 일·반복·근무·공휴일 등 데이터 모델과 계산 |
| `data/repository/` | 데이터 조회·저장과 서버 동기화 |
| `ui/navigation/` | 탭과 화면 이동 |
| `ui/screens/` | 할 일·달력·가계부·설정 등 기능별 화면 |
| `ui/theme/` | 테마·색상·화면 설정 |
| `widget/` · `reminder/` | 홈 화면 위젯과 알림 |
| `update/` | 앱 업데이트 |
| `overlay/` · `location/` | 빠른 추가 오버레이와 위치 기록 |

단위 테스트는 `app/src/test/`에 있습니다. 하위 할 일, 날짜별 완료, 메모, 휴가, 월급·군 복무 월급, 가계부 등의 계산을 검증합니다.

### CI와 배포

- **Pull Request** — [CI](.github/workflows/ci.yml)에서 Debug 빌드·Lint·단위 테스트를 실행합니다.
- **main 반영** — [APK 배포 워크플로](.github/workflows/build-apk.yml)가 최신 Debug APK를 `latest-debug` 릴리즈에 게시합니다. Markdown 문서만 변경한 경우에는 APK 배포를 실행하지 않습니다.
- **업데이트 배포** — APK 업로드가 끝난 뒤 `version.json`을 게시해 앱에서 새 버전을 확인할 수 있게 합니다.
- **버전 관리** — CI의 `versionCode`는 커밋 수를 사용하고, 표시 버전은 `0.<커밋 수 / 100의 몫>.<나머지>` 형식입니다.

## 함께 개선하기

사용 중 발견한 문제나 필요한 기능은 [Issues](https://github.com/YuInseo/BetterTick/issues)에 남겨 주세요. 버그를 제보할 때 앱 버전, 기기·Android 버전, 재현 방법을 함께 적어 주시면 도움이 됩니다.

코드 변경은 작업 브랜치에서 진행하고 PR로 올립니다. 현재 PR 커밋의 CI가 통과하고 충돌이 없는지 확인한 뒤 병합합니다. 자세한 작업 규칙은 [CLAUDE.md](CLAUDE.md)에 정리되어 있습니다.

## 관련 문서

[릴리즈 안내](RELEASE.md) &nbsp; · &nbsp;
[Firestore 스키마](FIRESTORE_SCHEMA.md) &nbsp; · &nbsp;
[작업 규칙](CLAUDE.md) &nbsp; · &nbsp;
[개발 기록](history/)

---

<div align="center">
매일 다른 근무에도, 나의 일상은 이어지도록.<br>
<strong>BetterTick</strong>
</div>
