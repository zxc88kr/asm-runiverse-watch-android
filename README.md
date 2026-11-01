# Runiverse Watch Android

> 멀리 떨어져 있어도 함께 달리는 원격 동반 러닝 플랫폼, **Runiverse**의 갤럭시워치(Wear OS) 앱입니다.

폰 앱은 [runiverse-frontend](https://github.com/SWM-TeamBruteForce/runiverse-frontend), 서버는 [runiverse-backend](https://github.com/SWM-TeamBruteForce/runiverse-backend)입니다.
애플워치 앱은 [runiverse-watch-ios](https://github.com/SWM-TeamBruteForce/runiverse-watch-ios)입니다.

## 기술 스택

| 분류 | 사용 기술 |
|---|---|
| Language | Kotlin (Wear OS) |
| 폰 연동 | Wearable Data Layer API |
| 배포 | Google Play Wear OS 트랙 (폰 앱과 같은 등록 정보) |

## 구성

폰 앱과 따로 빌드하고 따로 설치하는 별개 앱입니다. 갤럭시워치 말고도 Wear OS 워치(픽셀워치 등)에서 돌아갑니다.

- 폰 앱과 데이터를 주고받으려면 최종 **applicationId(`com.runiverse.app`)와 설치된 앱의 서명 인증서**가 같아야 합니다. 같아도 연결 상태·수신 처리가 갖춰져야 실제로 주고받습니다.
- 폰 쪽 변경이 없는 워치 수정은 워치 앱만 Wear OS 트랙에 올립니다. 폰 앱 버전은 그대로입니다.
- `versionCode`는 폰 앱과 겹치지 않고, 각 기기의 이전 배포본보다 커야 합니다. 첫 업로드 전에 할당 규칙을 정해 여기에 적습니다.

## 개발

- **폰과 같이 볼 때**: 폰 앱과 워치 앱을 둘 다 같은 flavor(`dev`면 `com.runiverse.app.dev`)의 debug로 빌드해 설치합니다. 두 앱이 같은 debug 키로 서명돼야 합니다.
  기본 설정이면 같은 사용자 계정의 `~/.android/debug.keystore`를 쓰지만, 실제 서명 설정을 확인합니다.
  Play에서 받은 폰 앱과 debug 키로 서명한 워치 앱은 서명이 달라 통신할 수 없습니다.
- **출시 전 검증**: 둘 다 Play 테스트 트랙에서 받아 확인합니다.

- **PR**: 기능 PR은 `dev`로, 릴리스 PR은 `main`으로 올립니다. GitHub 기본 대상이 `main`이니 확인합니다.

## 상태

준비 중입니다. 이 문서는 폰 연동 구성을 기준으로 합니다. 폰 없이 쓸 기능 범위는 아직 정하지 않았습니다.
