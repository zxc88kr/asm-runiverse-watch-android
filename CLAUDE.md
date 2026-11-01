# CLAUDE.md

Runiverse 갤럭시워치(Wear OS) 앱. 폰 앱(runiverse-frontend)과 Data Layer로 붙어, 폰이 기록·판단하고 워치는 표시·입력만 한다.

## 빌드

```bash
./gradlew :app:assembleDevDebug    # 빌드
./gradlew :app:lintDevDebug        # lint
./gradlew :app:installDevDebug     # 연결된 워치에 설치
```

- Gradle 데몬은 JDK 21로 돈다(`gradle/gradle-daemon-jvm.properties`).

## 문서 인덱스 — 구현 전 해당 문서를 읽을 것

- `docs/code-convention.md` — 네이밍·주석·테스트 규칙
- `docs/git-convention.md` — 커밋·브랜치·PR 규칙

## 구조

- 모듈은 `app` 하나, 코드는 `app/src/main/java/com/runiverse/app/`. 기능 패키지를 만들면 여기에 적는다.
- 라이브러리·플러그인 버전은 `gradle/libs.versions.toml`에만 적는다.
- flavor는 폰 앱과 같다 — `dev`(`com.runiverse.app.dev`)·`prod`(`com.runiverse.app`). 폰 dev 빌드와 붙여 볼 땐 워치도 `dev`.

## 범위

- 1차는 폰 경유 러닝 화면과 끝난 직후 결과 요약뿐이다. 워치가 서버를 직접 부르는 기능(매칭·기록 보기)은 넣지 않는다.

## 주의사항

- 폰과 통신하려면 applicationId와 서명이 폰 앱과 같아야 한다. Play에서 받은 폰 앱과 로컬 빌드 워치 앱은 서명이 달라 안 붙는다.
- `versionCode`는 폰 앱이 쓴 값과 겹치면 안 된다. 첫 업로드 전에 규칙을 README에 정한다.
- 키스토어·`key.properties`는 커밋하지 않는다.
- 요청 없이 라이브러리를 추가하지 않는다.
- 문제를 우회하는 해결 금지 — 테스트 약화·빈 catch·기능 삭제로 오류를 없애지 않는다.
