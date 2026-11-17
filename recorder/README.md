# Daydrift 녹음 (백그라운드 녹음 앱)

Daydrift와 따로 설치되는 녹음 앱 (`com.bettertick.recorder`). `recorder` 브랜치에서만 개발하고 `main`에는 합치지 않는다.

## 기능
- **앱을 꺼도 녹음 유지** — 마이크 포그라운드 서비스 + `stopWithTask=false` + 부분 웨이크락. 최근 앱에서 지워도, 화면을 꺼도 계속 녹음한다. 시스템이 서비스를 죽였다 살리면(START_STICKY) 이어서 새 파일로 녹음을 시도한다.
- **알림에서 조작** — 경과 시간 표시, 일시정지/이어서/정지.
- **파일 나누기** — 15분/30분/1시간마다 새 파일(안 나눔도 가능). 중간에 문제가 생겨도 끝난 조각은 안전하다.
- **구글 드라이브 자동 백업** — 파일 하나가 끝날 때마다 WorkManager로 드라이브 `Daydrift 녹음` 폴더에 이어받기 업로드. 와이파이에서만 옵션, 올린 뒤 휴대폰에서 지우기 옵션, 6시간마다 놓친 파일 재확인. 권한은 `drive.file`(이 앱이 만든 파일만 접근).
- 목록에서 재생·공유·삭제, 백업 여부 표시, 음질(64/128/192kbps) 선택, 배터리 최적화 제외 바로가기.

## 다운로드
`recorder` 브랜치에 push하면 `.github/workflows/build-recorder.yml`이 빌드해서 올린다:
https://github.com/YuInseo/BetterTick/releases/download/recorder-latest/DaydriftRecorder.apk

## 드라이브 연결을 위한 1회 설정 (Google Cloud 콘솔)
Daydrift와 같은 Firebase/GCP 프로젝트에서:
1. **API 및 서비스 → 라이브러리 → Google Drive API 사용 설정**
2. **사용자 인증 정보 → OAuth 클라이언트 ID 만들기 → Android**
   - 패키지 이름: `com.bettertick.recorder`
   - SHA-1: Daydrift와 같은 `app/debug.keystore`의 SHA-1 (`keytool -list -v -keystore app/debug.keystore -storepass android`)
3. OAuth 동의 화면이 "테스트" 상태면 본인 계정을 테스트 사용자에 추가하고, 범위에 `.../auth/drive.file`을 추가.

이걸 안 하면 "연결" 버튼에서 오류(10: DEVELOPER_ERROR 등)가 난다. 녹음 자체는 설정 없이 동작한다.
