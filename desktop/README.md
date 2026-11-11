# BetterTick Desktop (Tauri v2 + Rust)

키보드 중심 초경량 데스크톱 생산성 앱. 안드로이드 앱과 같은 저장소의 `pc` 브랜치, `desktop/` 폴더에 있어요.

## 실행

```bash
cd desktop
npm install
npm run tauri dev      # 개발 모드
npm run tauri build    # 설치 파일 (Windows: NSIS .exe)
```

Windows 설치 파일은 `pc` 브랜치에 push할 때마다 GitHub Actions(**Desktop (Tauri)**)가 만들어
`BetterTick-windows-setup` 아티팩트로 올려요.

## 단축키

| 키 | 동작 |
|---|---|
| `Alt+Space` (안 되면 `Ctrl+Alt+Space`) | 어디서든 빠른 추가 창 열기/닫기 |
| `Ctrl+Alt+P` | PIP 포모도로 위젯 클릭 허용 ↔ 클릭 통과 |
| `1`–`6` | 오늘 / 다음 7일 / 전체 / 백로그 / 완료 / N일 주기 |
| `j` `k` / `↑` `↓` | 이동 |
| `x` / `Space` | 완료 |
| `c` | 취소 |
| `t` | 내일로 미루기 |
| `f` | 선택한 할 일로 포모도로 시작 |
| `Delete` | 삭제 |
| `n` | 빠른 추가 |

창을 닫으면 트레이로 숨어요. 종료는 트레이 메뉴 → 종료.

## 빠른 추가 문법

`내일 15:00 백엔드 API 리팩토링 #개발 !1 ~2h @고집중`

- 날짜: `오늘` `내일` `모레` `글피`, `3일 후`, `금요일`, `다음주 월요일`, `12/25`, `3월 1일`, `2026-12-01`
- 시간: `15:00`, `15시 30분`, `오후 3시`, `8시반`, `3pm`
- `#태그`, `!1` 높음 `!2` 중간 `!3` 낮음, `~2h` `~30m` `~1h30m` 예상 시간
- 에너지: `@고집중` `@단순` `@체력`

## 구조

```
src-tauri/src/
  lib.rs       트레이 상주, 전역 단축키, 창 이벤트, 백로그 배치(1시간마다)
  db.rs        SQLite(WAL) 스키마·마이그레이션·쿼리 — 주기, DAG 의존성, 에너지, 포모도로, 마크다운 동기화 테이블
  cycle.rs     N일 주기 엔진: (날짜 − 기준일) mod N, O(1) + 날짜별 바꾸기
  parser.rs    빠른 추가 자연어 파서
  pomodoro.rs  타이머(Rust), PIP 창, 활성 창 샘플링 → 집중도 점수
  commands.rs  프론트엔드에서 부르는 IPC 명령
src/
  windows/     MainWindow · QuickAdd · Pip (창마다 #/route)
  components/  TaskRow · CycleView
```

`cargo test`로 파서·주기·DB(의존성 순환 차단, 백로그 격리, 주기 바인딩, 집중도) 테스트를 돌릴 수 있어요.

## 다음 단계

- Step 2: `TODO.md` 양방향 동기화 엔진(`notify` 감시 + `md_sources`/`md_task_links` 테이블), 습관·통계 화면
