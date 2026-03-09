# ADR-001 인증 전략

## 1. 상태

승인됨

## 2. 문맥

fromVillage는 구매자, 판매자, 관리자의 역할이 분리된 커머스 백엔드다.
보호된 API가 많고, 역할 기반 접근 제어와 본인 리소스 접근 제어가 모두 필요하다.
포트폴리오 핵심 주제 중 하나가 인증/인가/보안이므로, 설계 문서에서도 인증 전략을 명시적으로 설명할 필요가 있다.

## 3. 결정

인증은 Spring Security + Spring Session + Redis 기반의 stateful 세션 구조를 채택한다.

- 로그인은 커스텀 JSON 로그인 API로 처리한다.
- 인증 상태는 HttpSession으로 유지하고, Spring Session을 통해 Redis에 저장한다.
- 세션 저장소는 indexed Redis repository를 사용해 이후 principal 기반 세션 조회와 동시 세션 제한 정책에 재사용한다.
- 세션 식별자는 HttpOnly 쿠키로만 전달한다.
- 세션 쿠키는 `HttpOnly=true`, `Secure=true`, `SameSite=Lax`를 기본값으로 사용한다.
- 로컬 개발 프로필에서는 HTTP 개발 환경을 위해 세션 쿠키의 `Secure`만 `false`로 완화한다.
- 세션은 비활성 기준 30분 만료를 기본값으로 사용하고, 활동이 이어지면 만료 시간을 연장한다.
- 세션 추적은 쿠키만 사용하고, URL 기반 세션 추적은 사용하지 않는다.
- 로그인 성공 시 `changeSessionId()` 기반 세션 ID 재발급으로 세션 고정 공격을 방어한다.
- 동일 계정의 활성 세션은 1개로 제한하고, 새 로그인 시 기존 세션은 만료한다.
- 로그인 실패가 5회 연속 누적되면 10분 동안 로그인을 잠근다.
- 로그인 실패 횟수와 잠금 만료 시각은 Redis에 저장한다.
- 5번째 자격 증명 실패 요청부터 즉시 `AUTH_LOGIN_TEMPORARILY_LOCKED`를 반환한다.
- 로그인 성공 시 실패 횟수와 잠금 상태를 초기화한다.
- 로그인 요청 본문 형식 오류, 필수값 누락, CSRF 실패는 로그인 실패 횟수에 포함하지 않는다.
- CSRF 보호를 활성화하고 `/api/v1/csrf` 엔드포인트를 제공한다.
- 인가는 URL 보안과 메서드 보안을 함께 사용한다.
- 회원 비밀번호는 Spring Security `PasswordEncoder`를 통해 해시 저장하며, 구현은 `BCryptPasswordEncoder`를 기본값으로 사용한다.
- MVP에서는 remember-me 자동 로그인 기능을 포함하지 않는다.
- ADMIN 계정은 공개 회원가입이 아니라 초기 시드 또는 운영 초기화 절차로만 생성한다.

## 4. 근거

Spring Security 공식 문서는 세션 관리, 세션 고정 보호, 동시 세션 제어, CSRF 방어를 서블릿 애플리케이션의 핵심 보안 요소로 안내한다.
또한 로그인 이후에는 `changeSessionId()` 기반 세션 고정 보호를 기본 전략으로 사용하고, 필요 시 동시 세션 수를 제한할 수 있다.

이 프로젝트는 브라우저 기반 이커머스 시나리오를 전제로 하므로, 세션 쿠키 기반 인증과 CSRF 보호를 함께 설명하는 편이 도메인과 더 잘 맞는다.
또한 Redis를 이미 쿠폰 동시성 처리에 사용하므로, Spring Session과 결합해 서버 세션 저장소를 Redis로 두는 구조도 자연스럽다.

## 5. 대안

### JWT 기반 토큰 인증

장점:
- 모바일/서드파티 API까지 확장할 때 유연하다.

단점:
- 브라우저 기반 서비스에서는 CSRF와 별개로 토큰 보관 전략까지 직접 설계해야 한다.
- 로그아웃과 토큰 무효화 정책이 추가로 필요하다.

### Spring Security 기본 formLogin

장점:
- 구현이 단순하다.

단점:
- API 중심 포트폴리오 문서와 엔드포인트 구조를 유지하기 어렵다.
- JSON 기반 클라이언트와의 계약을 설명하기 불편하다.

## 6. 결과

- 보안 설정과 인증 흐름이 명확해진다.
- USER/SELLER/ADMIN 권한 제어를 문서와 구현 모두에서 설명하기 쉬워진다.
- 세션 생성/만료, 세션 고정 보호, 동시 세션 제어, 로그인 실패 잠금, CSRF 검증 테스트가 중요해진다.
- 동시 로그인으로 밀려난 기존 세션도 `AUTH_SESSION_EXPIRED` 계약으로 일관되게 응답해야 한다.
- 브라우저 기반 상태 변경 요청은 CSRF 토큰 재발급 흐름까지 함께 검증해야 한다.

## 7. 참고 자료

- [Spring Security Getting Started](https://docs.spring.io/spring-security/reference/servlet/getting-started.html)
- [Spring Security Method Security](https://docs.spring.io/spring-security/reference/servlet/authorization/method-security.html)
- [Spring Security Session Management](https://docs.spring.io/spring-security/reference/servlet/authentication/session-management.html)
- [Spring Security CSRF](https://docs.spring.io/spring-security/reference/servlet/exploits/csrf.html)
- [Spring Session Redis](https://spring.io/blog/2015/03/01/the-portable-cloud-ready-http-session)
