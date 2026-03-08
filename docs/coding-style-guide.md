# fromVillage Coding Style Guide

## 1. 프로젝트 개요

| 항목 | 기술 |
| --- | --- |
| Language | Java 21 |
| Framework | Spring Boot 3.5.11 |
| Build Tool | Groovy Gradle |
| Database | MySQL 8 |
| Session / Cache | Redis 7 |
| 아키텍처 | Modular Monolith (단일 Spring Boot 앱, 도메인 기준 모듈 분리) |
| 루트 패키지 | `com.fromvillage` |

참고:
- `build.gradle` — 의존성 정의
- `docker-compose.yml` — MySQL 8 + Redis 7 로컬 환경
- `.env.example` — 환경변수 템플릿
- `docs/architecture.md` — 아키텍처 상세

## 2. 패키지 구조 & 레이어 규칙

### 2.1 모듈 구성

애플리케이션은 도메인 기준 9개 모듈로 구성한다.

| 모듈 | 책임 |
| --- | --- |
| `auth` | 로그인, 로그아웃, 세션 인증, CSRF 토큰 발급 |
| `user` | 회원, 역할, 판매자 권한 부여 |
| `product` | 상품 등록, 수정, 조회, 재고 관리, soft delete |
| `cart` | 장바구니 담기, 수정, 삭제 |
| `order` | 체크아웃, 판매자별 주문 생성, 주문 취소 |
| `coupon` | 쿠폰 정책, 선착순 발급, 쿠폰 사용 및 복구 |
| `settlement` | 정산 대상 조회, 정산 배치, 정산 결과 기록 |
| `admin` | 관리자 전용 조회 및 운영 기능 |
| `common` | 공통 응답, 공통 예외, 보안 설정, 유틸리티 |

패키지 예시:

```
com.fromvillage
├── auth
│   ├── presentation
│   ├── application
│   ├── domain
│   └── infrastructure
├── user
├── product
├── cart
├── order
├── coupon
├── settlement
├── admin
└── common
    ├── exception
    ├── response
    ├── config
    └── util
```

### 2.2 레이어 구조

각 모듈은 4개 레이어로 구성한다.

| 레이어 | 책임 | 포함 요소 |
| --- | --- | --- |
| `presentation` | 요청 검증, 응답 변환 | Controller, Request/Response DTO |
| `application` | 유즈케이스 흐름, 트랜잭션 경계 | Application Service, Facade |
| `domain` | 핵심 비즈니스 규칙, 상태 전이 | Entity, Enum, Domain Service, Policy |
| `infrastructure` | 기술 구현 | JPA Repository, Redis 연동, Batch 설정 |

### 2.3 의존 방향

의존성은 바깥에서 안쪽으로 향한다. 역방향 의존은 금지한다.

```
presentation → application → domain ← infrastructure
```

- `presentation` → `application` → `domain` (단방향)
- `infrastructure` → `domain` / `application` (구현 제공)
- `domain`은 다른 레이어에 의존하지 않는다.
- `infrastructure`에 도메인 규칙을 포함하지 않는다.

모듈 간 협력은 MVP 기준으로 직접 서비스 호출을 사용한다.

## 3. 네이밍 컨벤션

### 3.1 클래스 (PascalCase)

| 유형 | 접미사/규칙 | 예시 |
| --- | --- | --- |
| Controller | `*Controller` | `OrderController` |
| Application Service | `*Service`, `*ApplicationService` | `OrderService` |
| Repository | `*Repository` | `OrderRepository` |
| Entity | 도메인명 그대로 | `Order`, `Product`, `CheckoutOrder` |
| Enum | `*Status`, `*Role`, `*Category` | `OrderStatus`, `UserRole`, `ProductCategory` |
| Request DTO | `*Request` | `LoginRequest`, `CreateProductRequest` |
| Response DTO | `*Response` | `OrderResponse`, `ProductResponse` |
| Policy | `*Policy` | `CouponPolicy` |
| Config | `*Config` | `SecurityConfig`, `RedisConfig` |

### 3.2 메서드 / 변수 (camelCase)

| 용도 | 접두사/패턴 | 예시 |
| --- | --- | --- |
| 조회 | `get*`, `find*`, `list*` | `getOrder`, `findByEmail`, `listProducts` |
| 생성/변경 | 동사 우선 | `createOrder`, `cancelOrder`, `issueCoupon` |
| 검증 | `is*`, `can*` | `isExpired`, `canIssue` |

### 3.3 상수 (UPPER_SNAKE_CASE)

```java
private static final int MAX_LOGIN_FAILURE_COUNT = 5;
private static final Duration LOGIN_LOCK_DURATION = Duration.ofMinutes(10);
```

### 3.4 패키지

- 소문자만 사용한다.
- 언더스코어를 사용하지 않는다.

```
com.fromvillage.order.domain
com.fromvillage.coupon.infrastructure
```

### 3.5 DB 컬럼 (snake_case)

| 용도 | 규칙 | 예시 |
| --- | --- | --- |
| PK | `id` | `id` |
| FK | `*_id` | `user_id`, `seller_order_id` |
| 시간 | `*_at` | `created_at`, `updated_at`, `deleted_at` |
| 수량 | `*_count`, `*_quantity` | `stock_quantity`, `issued_quantity` |
| 금액 | `*_amount` | `total_amount`, `discount_amount` |
| 불리언 | `is_*` | — |

## 4. 코드 포맷팅

- **인덴트**: 4 spaces (탭 사용 금지)
- **줄 길이**: soft 100자 / hard 120자
- **파일**: 파일당 하나의 top-level 클래스

### 클래스 멤버 순서

```java
public class Example {
    // 1. static 상수
    private static final String CONSTANT = "value";

    // 2. static 필드
    private static int counter;

    // 3. 인스턴스 필드
    private final SomeService someService;

    // 4. 생성자
    public Example(SomeService someService) { ... }

    // 5. public 메서드
    public void doSomething() { ... }

    // 6. private 메서드
    private void helper() { ... }

    // 7. 내부 클래스
    private static class Inner { ... }
}
```

## 5. 의존성 주입 & Lombok

### 의존성 주입

- **생성자 주입**을 기본으로 사용한다 (불변성, 테스트 용이성).
- Lombok `@RequiredArgsConstructor`를 활용한다.
- 필드 주입(`@Autowired`)은 테스트 코드(`@InjectMocks`)에서만 허용한다.

```java
@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
}
```

### 로깅

- `@Slf4j` (SLF4J)를 사용한다.
- `System.out.println` 사용을 금지한다.

```java
@Slf4j
@Service
@RequiredArgsConstructor
public class CouponService {

    public void issueCoupon(Long couponPolicyId, Long userId) {
        log.info("쿠폰 발급 요청: couponPolicyId={}, userId={}", couponPolicyId, userId);
    }
}
```

## 6. API 컨벤션

### 6.1 기본 경로

| 구분 | 경로 |
| --- | --- |
| 일반 API | `/api/v1/**` |
| 관리자 API | `/api/v1/admin/**` |

### 6.2 공통 응답 포맷

성공 응답:

```json
{
  "success": true,
  "code": "SUCCESS",
  "message": "요청이 성공했습니다.",
  "data": {}
}
```

실패 응답:

```json
{
  "success": false,
  "code": "ORDER_NOT_FOUND",
  "message": "주문을 찾을 수 없습니다.",
  "data": null,
  "errors": []
}
```

검증 실패 응답 예시:

```json
{
  "success": false,
  "code": "VALIDATION_ERROR",
  "message": "입력한 내용을 다시 확인해 주세요.",
  "data": null,
  "errors": [
    {
      "field": "email",
      "reason": "이메일 형식이 올바르지 않습니다."
    }
  ]
}
```

### 6.3 페이지네이션

요청 파라미터:

| 파라미터 | 설명 |
| --- | --- |
| `page` | 0부터 시작하는 페이지 번호 |
| `size` | 페이지 크기 |
| `sort` | 정렬 기준 (예: `createdAt,desc`) |

응답 구조:

```json
{
  "content": [],
  "page": 0,
  "size": 20,
  "totalElements": 100,
  "totalPages": 5,
  "hasNext": true
}
```

### 6.4 에러 코드

에러 코드는 UPPER_SNAKE_CASE를 사용하며, 도메인 접두사를 붙인다.

| 접두사 | 예시 |
| --- | --- |
| `AUTH_*` | `AUTH_UNAUTHORIZED`, `AUTH_SESSION_EXPIRED`, `AUTH_CSRF_INVALID`, `AUTH_FORBIDDEN`, `AUTH_LOGIN_TEMPORARILY_LOCKED` |
| `ORDER_*` | `ORDER_NOT_FOUND` |
| `PRODUCT_*` | `PRODUCT_NOT_FOUND` |
| `COUPON_*` | `COUPON_ALREADY_ISSUED` |
| 공통 | `VALIDATION_ERROR`, `SUCCESS` |

### 6.5 HTTP 상태 코드 매핑

| 상태 코드 | 용도 |
| --- | --- |
| 200 | 정상 응답 |
| 201 | 리소스 생성 성공 |
| 400 | 요청 검증 실패 |
| 401 | 미인증 (`AUTH_UNAUTHORIZED`, `AUTH_SESSION_EXPIRED`, `AUTH_LOGIN_TEMPORARILY_LOCKED`) |
| 403 | 권한 부족 (`AUTH_FORBIDDEN`, `AUTH_CSRF_INVALID`) |
| 404 | 리소스 미존재 |
| 409 | 충돌 (중복 발급 등) |
| 500 | 서버 내부 오류 |

## 7. Entity & DTO 규칙

### 7.1 Entity 공통

- **PK**: `id` (bigint, auto-increment)
- **필수 컬럼**: `created_at`, `updated_at`
- **Soft Delete**: `deleted_at` (nullable) 사용, 하드 삭제 금지
- 주문 시 상품 스냅샷 저장 (`order_item`에 `product_name_snapshot`, `product_price_snapshot` 복제)

### 7.2 DTO 규칙

- **Request DTO**에 Bean Validation 어노테이션을 사용한다.

```java
public record CreateProductRequest(
    @NotBlank String name,
    @NotBlank String description,
    @NotNull ProductCategory category,
    @Min(1) Long price,
    @Min(0) Integer stockQuantity,
    @NotBlank @Pattern(regexp = "^https://.*") String imageUrl
) {}
```

- **Response DTO**에 민감 정보를 노출하지 않는다.
  - 비밀번호, 세션 ID, 내부 bigint 식별자(`userId`) 노출 금지
  - SELLER 주문 조회에서 구매자 정보는 닉네임까지만 포함

## 8. 트랜잭션 전략

- `@Transactional`은 **application 레이어에서만** 선언한다.
- 유즈케이스 단위로 하나의 트랜잭션을 사용한다.
- 중첩 트랜잭션은 사용하지 않는다 (서비스 간 직접 호출).
- Propagation 기본값: `REQUIRED`

```java
@Service
@RequiredArgsConstructor
public class OrderService {

    @Transactional
    public OrderResponse createOrder(Long userId, CheckoutRequest request) {
        // 하나의 트랜잭션 안에서 유즈케이스 전체 수행
    }
}
```

주요 트랜잭션 사례:
- **주문 생성**: 장바구니 조회 → 판매자별 주문 생성 → 재고 차감 → 품절 전환 → 장바구니 삭제
- **주문 취소**: 주문 상태 변경 → 재고 복구 → 판매 상태 복구 → 쿠폰 복구
- **쿠폰 발급 확정**: 중복 검증 → 발급 이력 저장

## 9. 보안 규칙

### 9.1 인증

- Spring Security + Spring Session + Redis 기반 stateful 세션 인증
- 세션 쿠키 속성: `HttpOnly=true`, `Secure=true`, `SameSite=Lax`
- 세션 만료: 비활성 30분
- 세션 추적: 쿠키만 사용 (URL 기반 추적 금지)
- 로그인 성공 시 세션 ID 재생성 (`changeSessionId()`)
- 동시 세션 1개 제한 (새 로그인 시 기존 세션 만료)
- 비밀번호: `BCryptPasswordEncoder`로 해시 저장

### 9.2 로그인 실패 보호

- 5회 연속 실패 → 10분 잠금 (Redis 저장)
- 로그인 성공 시 실패 횟수 초기화

### 9.3 CSRF

- CSRF 보호 활성화
- 상태 변경 요청(POST, PUT, DELETE, PATCH)에 CSRF 토큰 필수
- CSRF 토큰 조회: `GET /api/v1/csrf`
- 로그인/로그아웃 후 CSRF 토큰 재조회 필요

### 9.4 인가

- URL 보안 + 메서드 보안(`@PreAuthorize`) 이중 구성
- USER: 본인 리소스만 접근
- SELLER: 본인 상품 및 본인 판매자 주문만 접근
- ADMIN: 운영 목적 전체 조회 및 관리

참고: `docs/adr/001-auth-strategy.md`

## 10. 에러 핸들링

- `@ControllerAdvice` + `@ExceptionHandler`로 전역 처리 (`GlobalExceptionHandler`)
- 도메인 예외 → API 에러 응답으로 변환
- **Unchecked 도메인 예외** 사용 (RuntimeException 상속)
- 일관된 에러 응답 포맷 유지 (6.2절 참고)

```java
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(OrderNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleOrderNotFound(OrderNotFoundException e) {
        // 도메인 예외 → 공통 에러 응답 변환
    }
}
```

인증/인가 에러 경계:

| 상황 | 상태 코드 | 에러 코드 |
| --- | --- | --- |
| 미인증 (세션 쿠키 없음) | 401 | `AUTH_UNAUTHORIZED` |
| 세션 만료 | 401 | `AUTH_SESSION_EXPIRED` |
| CSRF 토큰 오류 | 403 | `AUTH_CSRF_INVALID` |
| 권한 부족 | 403 | `AUTH_FORBIDDEN` |
| 로그인 잠금 | 401 | `AUTH_LOGIN_TEMPORARILY_LOCKED` |

## 11. 테스트 컨벤션

### 11.1 테스트 분류

| 유형 | 클래스 접미사 | 목적 | 특성 |
| --- | --- | --- | --- |
| 단위 테스트 | `*Test` | 도메인 규칙, 상태 전이 검증 | 외부 I/O 없음, 빠른 실행 |
| 통합 테스트 | `*IntegrationTest` | API 계약, 권한 검증, 트랜잭션 경계 | Testcontainers 사용 |
| 동시성 테스트 | `*ConcurrencyTest` | 쿠폰 발급 정합성 증명 | 대량 동시 요청 |
| 배치 테스트 | `*BatchTest` | 정산 중복 방지, 재실행 안전성 | Spring Batch Job/Step 검증 |

### 11.2 테스트 원칙

- 성공 케이스 + 실패 케이스 모두 작성한다.
- 기능 구현 전에 실패 테스트를 먼저 정의한다.

### 11.3 Testcontainers

MySQL 8과 Redis 7을 `@ServiceConnection`으로 연결한다.

```java
@TestConfiguration(proxyBeanMethods = false)
public class TestContainersConfig {

    @Bean
    @ServiceConnection
    MySQLContainer<?> mysqlContainer() {
        return new MySQLContainer<>("mysql:8.0");
    }

    @Bean
    @ServiceConnection(name = "redis")
    GenericContainer<?> redisContainer() {
        return new GenericContainer<>("redis:7-alpine")
                .withExposedPorts(6379);
    }
}
```

## 12. 환경변수 & 시크릿 관리

### 12.1 원칙

- DB 접속 정보, 비밀번호 등 **시크릿은 YAML에 직접 작성하지 않는다**.
- Spring Boot의 `${ENV_VAR:default}` 플레이스홀더를 사용하여 `.env` 파일 또는 시스템 환경변수에서 주입한다.
- `.env` 파일은 `.gitignore`에 등록하여 **절대 커밋하지 않는다**.
- `.env.example` 파일에 필요한 환경변수 목록과 예시 값을 유지한다.

### 12.2 환경변수 목록

| 변수명 | 용도 | 예시 |
| --- | --- | --- |
| `DB_URL` | JDBC 접속 URL | `jdbc:mysql://localhost:3306/fromvillage` |
| `DB_USERNAME` | DB 사용자명 | `fromvillage` |
| `DB_PASSWORD` | DB 비밀번호 | `fromvillage` |
| `REDIS_HOST` | Redis 호스트 | `localhost` |
| `REDIS_PORT` | Redis 포트 | `6379` |

### 12.3 프로파일별 적용

| 프로파일 | 시크릿 소스 | 설명 |
| --- | --- | --- |
| `local` | `.env` 파일 또는 기본값 | `${DB_URL:jdbc:mysql://localhost:3306/fromvillage}` 형태로 기본값 제공 |
| `test` | Testcontainers `@ServiceConnection` | 컨테이너가 자동으로 접속 정보 주입 |
| 운영 | 시스템 환경변수 / 시크릿 매니저 | 기본값 없이 `${DB_URL}` 사용, 미설정 시 기동 실패 |

### 12.4 application.yml 예시

```yaml
# application.yml (base) — 기본값 없음, 환경변수 필수
spring:
  datasource:
    url: ${DB_URL}
    username: ${DB_USERNAME}
    password: ${DB_PASSWORD}
```

```yaml
# application-local.yml — 로컬 개발용 기본값 제공
spring:
  datasource:
    url: ${DB_URL:jdbc:mysql://localhost:3306/fromvillage}
    username: ${DB_USERNAME:fromvillage}
    password: ${DB_PASSWORD:fromvillage}
```

### 12.5 Docker Compose

`docker-compose.yml`은 `env_file: .env`로 환경변수를 주입받는다. 로컬 실행 시 `.env.example`을 복사하여 `.env`를 생성한다.

```bash
cp .env.example .env
docker-compose up
```

## 13. Redis 사용 규칙

MVP에서 Redis는 세션 관리, 쿠폰 동시성 제어, 로그인 실패 보호에 사용한다. 애플리케이션 캐시는 MVP 범위에서 제외한다.

| 용도 | 키 패턴 |
| --- | --- |
| 세션 | `fromvillage:session:*` (Spring Session 관리) |
| 쿠폰 정책 발급 수 | `coupon:policy:{id}:issued` |
| 사용자별 쿠폰 발급 여부 | `coupon:user:{userId}:{policyId}:issued` |
| 로그인 실패 횟수 | `login:failure:{email}:count` |
| 로그인 잠금 만료 시각 | `login:failure:{email}:locked_until` |

참고: `docs/adr/002-coupon-concurrency.md`

## 14. 로깅 규칙

| 레벨 | 용도 | 예시 |
| --- | --- | --- |
| `INFO` | 비즈니스 이벤트 | 로그인 성공, 주문 생성, 쿠폰 발급 |
| `DEBUG` | 상세 흐름 | 메서드 진입/종료, 중간 데이터 |
| `WARN` | 복구 가능한 에러 | 쿠폰 발급 실패 (수량 초과) |
| `ERROR` | 시스템 장애 | DB 연결 실패, 예상치 못한 예외 |

- **`@Slf4j`**를 사용한다.
- **`System.out.println` 금지**.

## 15. 금지 사항 요약

| 규칙 | 이유 |
| --- | --- |
| 하드 삭제 금지 | 과거 주문 이력 참조 무결성 보존 (`deleted_at` soft delete 사용) |
| HTTP 이미지 URL 금지 | `https` 스킴만 허용 |
| 외부 이미지 URL 직접 fetch 금지 | 서버는 `imageUrl`을 저장만 하고 fetch하지 않음 |
| Response에 비밀번호/세션ID/내부ID 노출 금지 | 민감 정보 보호 |
| USER+SELLER 동시 역할 금지 (MVP) | 역할 전환 방식으로 처리 |
| 공개 회원가입으로 ADMIN 생성 금지 | 초기 시드 또는 운영 초기화로만 생성 |
| infrastructure 레이어에 도메인 규칙 포함 금지 | 도메인 규칙은 domain 레이어에 위치 |
| `System.out.println` 사용 금지 | `@Slf4j` 사용 |
| 필드 주입 금지 (프로덕션 코드) | 생성자 주입 사용 |
| application 레이어 외 `@Transactional` 선언 금지 | 트랜잭션 경계 일관성 |
| YAML에 시크릿 하드코딩 금지 | `${ENV_VAR}` 플레이스홀더 사용, `.env` 파일로 주입 |
| `.env` 파일 커밋 금지 | `.gitignore`에 등록, `.env.example`만 커밋 |

## 16. Git 브랜치 전략

### 16.1 브랜치 구조

```
main ← 최종 배포용 (항상 배포 가능한 상태 유지)
 └── develop ← 개발 통합 브랜치
      ├── feature/issue-12-login
      ├── feature/issue-25-coupon-concurrency
      └── fix/issue-30-order-cancel-bug
```

| 브랜치 | 용도 | 머지 대상 |
| --- | --- | --- |
| `main` | 최종 배포 | `develop` → PR 머지 |
| `develop` | 개발 통합 | feature/fix 브랜치 → PR 머지 |
| `feature/*` | 기능 개발 | `develop` |
| `fix/*` | 버그 수정 | `develop` |
| `hotfix/*` | 긴급 수정 | `main` + `develop` |

### 16.2 작업 흐름

1. **이슈 생성** — GitHub Issue에 작업 내용 등록
2. **브랜치 생성** — `develop`에서 분기

```bash
git switch develop
git pull origin develop
git switch -c feature/issue-12-login
```

3. **개발 & 커밋** — 작업 단위로 커밋

```bash
git add <files>
git commit -m "feat: 로그인 API 구현 (#12)"
```

4. **PR 생성** — `develop` 브랜치로 Pull Request 생성
5. **코드 리뷰 & 머지** — 리뷰 승인 후 `develop`에 머지
6. **브랜치 삭제** — 머지 완료된 브랜치 삭제

```bash
git branch -d feature/issue-12-login
```

### 16.3 브랜치 네이밍

```
<type>/issue-<번호>-<간단한-설명>
```

| 타입 | 용도 | 예시 |
| --- | --- | --- |
| `feature` | 새 기능 | `feature/issue-12-login` |
| `fix` | 버그 수정 | `fix/issue-30-order-cancel-bug` |
| `hotfix` | 긴급 배포 수정 | `hotfix/issue-45-payment-error` |
| `refactor` | 리팩토링 | `refactor/issue-50-order-service` |
| `docs` | 문서 수정 | `docs/issue-60-api-docs` |

### 16.4 커밋 메시지

```
<type>: <설명> (#이슈번호)
```

| 타입 | 용도 |
| --- | --- |
| `feat` | 새 기능 |
| `fix` | 버그 수정 |
| `refactor` | 리팩토링 (기능 변경 없음) |
| `docs` | 문서 수정 |
| `test` | 테스트 추가/수정 |
| `chore` | 빌드, 설정 변경 |

### 16.5 규칙

- `main`, `develop` 브랜치에 **직접 push 금지** — 반드시 PR을 통해 머지한다.
- PR 머지 시 **Squash Merge**를 기본으로 사용한다.
- 머지 완료된 브랜치는 즉시 삭제한다.
- `main` ← `develop` 머지는 배포 준비가 완료된 시점에만 수행한다.

## 참고 문서

- `docs/architecture.md` — 레이어 구조, 의존 방향, 모듈 구성
- `docs/api.md` — API 컨벤션, 응답 포맷, 에러 코드
- `docs/erd.md` — DB 컬럼 네이밍, 제약 조건, 관계
- `docs/prd.md` — 비즈니스 규칙, 보안 요구사항, 금지 사항
- `docs/test-strategy.md` — 테스트 분류, 네이밍, 품질 기준
- `docs/glossary.md` — 용어 정의
- `docs/adr/001-auth-strategy.md` — 인증 전략 결정
- `docs/adr/002-coupon-concurrency.md` — 쿠폰 동시성 처리 전략
- `docs/adr/003-settlement-batch.md` — 정산 배치 처리 전략
