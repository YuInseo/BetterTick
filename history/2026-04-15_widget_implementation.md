# 2026-04-15 — Widget Implementation + Profile Bug Fix

## 세션 요약

### 🐛 버그 수정
1. **Google 프로필 이미지 버그** — 설정 화면에서 Google 프로필 사진이 표시되지 않고 주황색 원만 나오는 버그
   - `User.photoUrl` 필드 추가
   - `AuthRepository`에서 Google 로그인 시 `photoUrl` 캡처
   - Coil 2.6.0 의존성 추가
   - `AuthViewModel.userPhotoUrl` 노출
   - `MoreScreen`에서 `AsyncImage`로 프로필 이미지 표시
   - `BetterTickNavHost`에서 `userPhotoUrl` 전달

2. **AppearanceScreen 뒤로가기 버튼 위치** — 탭(테마/앱 아이콘/표시) 같은 줄 왼쪽으로 이동
   - `TopAppBar` 제거 후 `Row`로 재구성

### 🚀 새 기능: 위젯 17개 구현

#### Phase 1: 인프라 세팅
- **의존성 추가**: Jetpack Glance 1.1.1, WorkManager 2.9.1
- **`widget/WidgetServiceLocator.kt`** — Hilt 우회용 싱글턴 (AppWidget에서 Repository 접근)
- **`widget/theme/WidgetTheme.kt`** — `WidgetColors` (ColorProvider), `WidgetColorValues` (raw Color)
- **`widget/util/WidgetDateUtils.kt`** — 날짜/주간/월간 유틸 + Korean day labels
- **`widget/WidgetUpdateWorker.kt`** — WorkManager로 30분마다 `updateAll` 호출
- **`BetterTickApplication`** — `WidgetServiceLocator.init()` + `WidgetUpdateWorker.enqueue()`
- **`AndroidManifest.xml`** — 17개 receiver 등록
- **`res/xml/widget_*_info.xml`** — 17개 widget 메타데이터

#### Phase 2: 17개 Glance AppWidgets (병렬 서브에이전트)

**캘린더 (3):**
- MonthCalendarWidget — 월간 6주 그리드
- WeekCalendarWidget — 주간 스트립
- CalendarTasksWidget — 미니 캘린더 + 할일 영역

**일정 (3):**
- WeekScheduleWidget — 주간 스트립 + 우선순위 컬러 할일 리스트
- MonthScheduleWidget — 월간 + 이벤트 라벨 칩
- ThreeDayTimelineWidget — 시간 블록 타임라인

**할일 (3):**
- TodoListWidget (참조 구현) — 오늘 할일 체크박스 리스트
- QuickAddWidget — 빠른 추가 + 버튼
- EisenhowerMatrixWidget — 4분면 우선순위 매트릭스

**습관 (4):**
- HabitWeekTrackerWidget — 원형 프로그레스 + 요일별 상태
- TodayHabitWidget — 연속 일수 카드 (연녹색)
- HabitHeatmapWidget — 월간 히트맵 그리드
- WeeklyHabitsWidget — 다중 습관 요일별 매트릭스

**포커스/기타 (4):**
- FocusTimerWidget — 🍅 뽀모도로 + 시작 버튼
- TimeDistributionWidget — 주간 스택 바 차트 + 카테고리 범례
- TaskCompletionWidget — 일별 완료 바 차트
- DDayWidget — 신정까지 D-Day 카운트다운

#### Phase 3: 인앱 위젯 갤러리
- **`ui/screens/more/WidgetGalleryScreen.kt`** (1327 lines) — 17개 위젯 Compose 프리뷰
- **`MoreScreen`** — 위젯 메뉴 클릭시 갤러리 네비게이션
- **`BetterTickNavHost`** — `"widgets"` 라우트 추가

### 🔧 빌드 이슈 해결
- `Alignment.Bottom` → `Alignment.BottomCenter` (TaskCompletionWidget, TimeDistributionWidget)
  - `contentAlignment` 파라미터는 `Alignment` 타입 요구, `Alignment.Bottom`은 `Alignment.Vertical`

### ✅ 최종 상태
- **`./gradlew assembleDebug`** — BUILD SUCCESSFUL in 26s
- 17개 위젯 클래스 + 17개 리시버 클래스 + 17개 XML 메타 + 1개 갤러리 화면 + 5개 인프라 파일

### 📁 파일 구조
```
app/src/main/
├── AndroidManifest.xml (17 receiver 등록)
├── java/com/bettertick/
│   ├── BetterTickApplication.kt (ServiceLocator 초기화)
│   ├── widget/
│   │   ├── WidgetServiceLocator.kt
│   │   ├── WidgetUpdateWorker.kt
│   │   ├── theme/WidgetTheme.kt
│   │   ├── util/WidgetDateUtils.kt
│   │   ├── calendar/ (6 files)
│   │   ├── schedule/ (6 files)
│   │   ├── todo/ (6 files)
│   │   ├── habit/ (8 files)
│   │   └── focus/ (8 files)
│   └── ui/screens/more/WidgetGalleryScreen.kt
└── res/xml/widget_*_info.xml (17 files)
```

### 🎯 다음 스텝 후보
- 실제 기기에서 위젯 추가 후 렌더링 확인
- 위젯에서 Flow 구독하는 대신 WorkManager 배치로 스냅샷 업데이트 검증
- 위젯 크기별 레이아웃 분기 (compact/medium/large)
- 다크/라이트 테마 대응
- 위젯 설정 액티비티 (사용자가 어떤 목록/습관을 표시할지 선택)
