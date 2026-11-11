# BetterTick Desktop

Rust + Tauri 2 + React/TypeScript 기반 Windows 데스크톱 클라이언트.

## 개발 실행
1. Node.js 20+, Rust stable, Windows WebView2 및 Tauri Windows 빌드 도구 설치
2. `npm install`
3. `.env.example`을 `.env`로 복사하고 Firebase **웹 앱** 설정 입력 (Android와 같은 Firebase 프로젝트)
4. Firebase Console Authentication에서 Google 로그인 활성화
5. `npm run tauri dev`

## 빌드
`npm run tauri build` — Windows NSIS/MSI 설치 파일 생성

## 현재 구현
- Rust/Tauri 네이티브 창 및 트레이 메뉴
- Firebase Google 로그인 (웹 팝업 방식; Tauri WebView 환경에서 제공업체 제한 가능)
- Firestore 실시간 할 일 및 목록 동기화
- 오늘, 예정, 받은 편지함, 전체, 완료 보기
- 할 일 생성/수정/완료/삭제, 날짜/우선순위/목록/메모
- 월간 캘린더, 우선순위별 칸반, 기본 집중 타이머
- 검색, Ctrl+K, Ctrl+N, 반응형 3단 레이아웃

## 미완성
- Google OAuth의 Tauri 시스템 브라우저 딥링크 인증
- 습관, 반복 규칙 편집, 태그 편집, 캘린더 드래그, 집중 세션 저장
- SQLite 오프라인 큐, OS 알림 예약, 글로벌 단축키, 자동 업데이트
- CI 빌드 검증 및 Windows 설치 파일 확인

Firebase 보안 규칙은 사용자 본인의 `users/{uid}` 경로만 접근 가능하도록 설정하세요.
