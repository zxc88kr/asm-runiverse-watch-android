# Code Convention

## 기본 원칙

- 포맷팅은 루트 `.editorconfig`를 기준으로 한다.
- `.editorconfig`와 프로젝트 문서에 없는 규칙은 [Kotlin 코딩 컨벤션](https://kotlinlang.org/docs/coding-conventions.html)을 따른다.
- 커밋·브랜치·PR 규칙은 [git-convention.md](git-convention.md)를 따른다.

## 네이밍

- 클래스·인터페이스·object·enum: `PascalCase`. `@Composable` 함수도 `PascalCase`로 짓는다.
- 함수·변수·파라미터: `camelCase`
- 상수(`const val`·최상위 불변 값): `UPPER_SNAKE_CASE`
- 패키지: 소문자, 밑줄 없이 쓴다.
- 이름은 동작·역할이 드러나도록 짓고, 축약어·의미 없는 이름(`data`, `info`, `temp`)은 피한다.

## 주석

- 코드만 봐서는 알 수 없는 이유만 짧게 적는다. 코드가 하는 일을 다시 설명하지 않는다.
- 명세 문서를 인용하지 않는다 — 절 번호·문서명 모두. 명세가 개편되면 낡으므로 인용 없이 자립하는 문장으로 적는다.
- 보류·미완성은 `// TODO: 파티원 목데이터 — 폰 연결을 만들 때 실제 값으로 교체한다`처럼 적는다.

## 테스트

- 새로운 로직에는 단위 테스트를 작성한다.
- 버그 수정 시 실패를 재현하는 테스트를 먼저 추가한다.
- 테스트는 given-when-then 구조로 작성하고, 구분 주석을 `// given`, `// when`, `// then`으로 통일한다. 실행과 검증이 한 문장인 경우에는 `// when & then`으로 합칠 수 있다. 토큰 뒤에 `-> 설명`으로 의도를 덧붙일 수 있다.
- 테스트 이름은 행위가 드러나도록 작성한다.
- 내부 구현보다 외부에서 관찰 가능한 결과를 검증한다.
- 불필요한 mock과 과도한 `verify()`를 사용하지 않는다.
- 핵심 조건은 본문에 두고, 나머지 반복 준비는 헬퍼로 뺄 수 있다.
