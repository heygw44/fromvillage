# fromVillage Architecture

## 1. 문서 목적

본 문서는 fromVillage MVP의 애플리케이션 구조, 모듈 경계, 보안 구조, 트랜잭션 전략, 쿠폰 동시성 처리 방식, 정산 배치 구조를 정의한다.

## 2. 아키텍처 방향

fromVillage는 모듈형 모놀리식 아키텍처를 채택한다.
MSA는 MVP 범위에서 제외하고, 하나의 Spring Boot 애플리케이션 안에서 도메인 기준으로 모듈을 분리한다.

이 구조는 OOP, 클린코드, DDD를 다음처럼 실용적으로 반영한다.

- OOP
  엔티티와 도메인 객체가 자신의 상태와 규칙을 가진다.
- 클린코드
  책임이 다른 코드를 계층과 패키지로 분리하고, 역할이 드러나는 이름을 사용한다.
- DDD
  기술 레이어보다 도메인 경계를 먼저 나누고, 주문, 쿠폰, 정산 같은 핵심 규칙을 도메인 중심으로 설계한다.

Context7 기준으로도 Spring Modulith는 비즈니스 도메인 기준 모듈 구성을 강조하고, Spring Data JPA는 여러 저장소를 아우르는 비즈니스 흐름의 트랜잭션 경계를 서비스 계층에서 관리하는 방향을 보여준다.

참고:
- [Spring Modulith](https://github.com/spring-projects/spring-modulith)
- [Spring Data JPA Transactions](https://github.com/spring-projects/spring-data-jpa/blob/main/src/main/antora/modules/ROOT/pages/jpa/transactions.adoc)

## 3. 모듈 구성

애플리케이션은 아래 모듈로 나눈다.

- `auth`
  로그인, 로그아웃, 세션 인증, CSRF 토큰 발급
- `user`
  회원, 역할, 판매자 권한 부여
- `product`
  상품 등록, 수정, 조회, 재고 관리, soft delete
- `cart`
  장바구니 담기, 수정, 삭제
- `order`
  체크아웃, 판매자별 주문 생성, 주문 취소
- `coupon`
  쿠폰 정책, 선착순 발급, 쿠폰 사용 및 복구
- `settlement`
  정산 대상 조회, 정산 배치, 정산 결과 기록
- `admin`
  관리자 전용 조회 및 운영 기능
- `common`
  공통 응답, 공통 예외, 보안 설정, 유틸리티

## 4. 모듈 내부 구조

각 모듈은 아래 계층을 기준으로 구성한다.

- `presentation`
  Controller, Request/Response DTO
- `application`
  UseCase, Application Service, Facade
- `domain`
  Entity, Enum, Domain Service, Policy
- `infrastructure`
  JPA Repository, Redis 연동, Batch 설정, 외부 구현체

저장소 협력 원칙:

- `application`은 필요한 저장 연산을 `domain` 포트 인터페이스에 의존한다.
- `infrastructure`는 Spring Data JPA 저장소와 어댑터를 통해 그 포트를 구현한다.
- 회원가입은 `SignupService -> user.domain.UserStore -> user.infrastructure.UserStoreJpaAdapter -> UserJpaRepository` 흐름으로 연결한다.

핵심 원칙은 다음과 같다.

- Controller는 요청 검증과 응답 변환에 집중한다.
- Application Service는 유스케이스 흐름과 트랜잭션 경계를 관리한다.
- Domain은 핵심 규칙과 상태 전이를 가진다.
- Infrastructure는 저장소와 기술 구현을 담당하며 도메인 규칙을 직접 소유하지 않는다.

## 5. 의존 방향

의존성은 바깥에서 안쪽으로 향한다.

- `presentation -> application -> domain`
- `infrastructure -> domain/application`
- `application -> infrastructure` 직접 의존은 허용하지 않는다.

모듈 간 협력은 MVP 기준으로 직접 서비스 호출 중심으로 설계한다.
이벤트 기반 통신은 전면 도입하지 않고, 이후 확장 포인트로만 제한한다.

## 6. 보안 아키텍처

인증은 Spring Security 기반 stateful 세션 구조를 사용한다.
인증된 사용자 상태는 HttpSession으로 관리하고, Spring Session indexed Redis repository를 통해 Redis에 저장한다.
세션 식별자는 쿠키로만 전달하며, 응답 바디나 URL에 세션 값을 노출하지 않는다.
로그인은 JSON 요청 본문을 처리하는 커스텀 인증 필터를 통해 수행하며, form login 리다이렉트는 사용하지 않는다.
로그인 성공 시 `changeSessionId()` 기반 세션 ID 재발급으로 세션 고정 공격을 방어한다.
CSRF 보호는 활성화하며, `/api/v1/csrf` 엔드포인트를 통해 클라이언트가 토큰을 조회할 수 있게 한다.
로그인 성공과 로그아웃 성공 이후에는 새로운 CSRF 토큰을 다시 발급받아야 한다.
인증되지 않은 요청은 `AuthenticationEntryPoint`를 통해 `401`과 `AUTH_UNAUTHORIZED` 에러 코드로 응답한다.
세션 만료 요청은 `InvalidSessionStrategy`를 통해 `401`과 `AUTH_SESSION_EXPIRED` 에러 코드로 응답한다.
CSRF 토큰 오류는 `AccessDeniedHandler`를 통해 `403`과 `AUTH_CSRF_INVALID` 에러 코드로 응답한다.
인증되었지만 권한이 부족한 요청은 `AccessDeniedHandler`를 통해 `403`과 `AUTH_FORBIDDEN` 에러 코드로 응답한다.
MVP에서는 remember-me 자동 로그인 기능은 포함하지 않는다.

응답 경계는 다음처럼 고정한다.

- 세션 쿠키가 없는 보호 자원 요청은 `AuthenticationEntryPoint`를 통해 `AUTH_UNAUTHORIZED`로 처리한다.
- 세션 쿠키는 존재하지만 Redis에 세션이 없거나 만료된 세션 ID가 제출된 경우는 `InvalidSessionStrategy`를 통해 `AUTH_SESSION_EXPIRED`로 처리한다.

세션 쿠키 속성, 세션 만료 시간, 동시 세션 제한, 로그인 잠금의 세부 수치와 운영 정책은 `docs/adr/001-auth-strategy.md`를 따른다.
세션 저장소는 `spring.session.redis.repository-type=indexed`를 기준으로 구성해 이후 동시 세션 제한과 principal 기반 세션 조회에 재사용한다.

인가 전략은 두 층으로 구성한다.

- URL 보안
  인증 필요 경로와 공개 경로를 1차로 구분한다.
- 메서드 보안
  `@PreAuthorize` 등을 이용해 역할 및 소유권 검증을 수행한다.

권한 검증 원칙은 다음과 같다.

- USER는 본인 리소스만 접근 가능하다.
- SELLER는 본인 상품과 본인 판매자 주문만 접근 가능하다.
- ADMIN은 운영 목적의 전체 조회와 관리가 가능하다.
- ADMIN은 운영 전용 계정으로 간주하며 일반 구매와 판매 유스케이스에는 참여하지 않는다.
- MVP에서는 하나의 계정이 동시에 USER와 SELLER를 가지지 않으며, 판매자 승인은 역할 전환으로 처리한다.
- 판매자 승인은 `USER -> SELLER` 단방향 전환으로만 다루며, 이미 SELLER인 계정 재승인이나 ADMIN 계정 전환은 허용하지 않는다.
- ADMIN 계정은 공개 회원가입이 아니라 초기 시드 또는 운영 초기화 절차로만 생성한다.
- 회원 계정의 탈퇴/비활성화는 상품 soft delete와 별도 lifecycle 정책으로 다루며, 현재 MVP 범위의 `users` 테이블에는 `deleted_at`을 두지 않는다.
- 상품 이미지 URL은 `https`만 허용하고, 서버는 외부 `imageUrl`을 직접 fetch하지 않는다.
- 상품 삭제는 hard delete가 아니라 soft delete로 처리해 과거 주문 이력의 참조 무결성을 유지한다.
- soft delete된 상품은 공개 목록/상세 API에서 숨기고, 공개 상세 조회 요청에는 `404 Not Found`를 반환한다.
- SELLER 관리 조회는 별도 경계로 두며, 본인 상품 목록에서는 soft delete된 상품도 `deletedAt`과 함께 확인할 수 있다.

참고:
- [Spring Security Getting Started](https://docs.spring.io/spring-security/reference/servlet/getting-started.html)
- [Spring Security Method Security](https://docs.spring.io/spring-security/reference/servlet/authorization/method-security.html)
- [Spring Security Session Management](https://docs.spring.io/spring-security/reference/servlet/authentication/session-management.html)
- [Spring Security CSRF](https://docs.spring.io/spring-security/reference/servlet/exploits/csrf.html)
- [Spring Session Redis](https://spring.io/blog/2015/03/01/the-portable-cloud-ready-http-session)

## 7. 트랜잭션 전략

트랜잭션 경계는 `application service`에서 관리한다.
여러 리포지토리와 도메인 규칙이 함께 동작하는 유스케이스는 서비스 단위에서 하나의 트랜잭션으로 묶는다.

대표 사례는 다음과 같다.

- 주문 생성
  장바구니 조회, 판매자별 주문 생성, 주문 아이템 생성, 재고 차감, 재고 0 시 `SOLD_OUT` 전환, 성공한 체크아웃 항목의 장바구니 삭제
- 주문 취소
  `checkout_order`와 하위 `seller_order` 상태 변경, 재고 복구, 재고 복구 시 `ON_SALE` 전환, 쿠폰 복구
- 판매자 권한 부여
  사용자 역할 변경
- 회원가입
  이메일 중복 선행 조회, 비밀번호 해시 저장, 사용자 저장
- 로그인
  사용자 인증, 세션 생성, 세션 ID 재발급
- 로그인 실패 제어
  Redis에 연속 실패 횟수와 잠금 상태를 원자적으로 갱신해 저장하고, 상세 임계값과 해제 정책은 `docs/adr/001-auth-strategy.md`를 따른다
- 로그아웃
  현재 HttpSession 무효화, 보안 컨텍스트 제거
- 쿠폰 발급 확정
  중복 검증, 발급 이력 저장
- 정산 처리
  대상 주문 조회, 정산 결과 저장, 중복 정산 방지

## 8. 쿠폰 동시성 아키텍처

쿠폰 발급은 Redis + DB 혼합 구조를 사용한다.

흐름은 다음과 같다.

1. 쿠폰 발급 요청 수신
2. Redis에서 선착순 수량 차감 또는 발급 가능 여부 확인
3. Redis에서 동일 사용자 중복 발급 여부 확인
4. 조건 통과 시 DB에 `issued_coupon` 저장
5. DB 저장 실패 시 Redis 수량과 중복 상태 보정

장바구니 체크아웃에서는 요청의 `targetSellerId`를 기준으로 특정 `seller_order` 1건에만 쿠폰을 연결한다.
바로 구매에서는 `productId`로 SELLER가 단일하게 결정되므로 별도의 `targetSellerId` 없이 해당 `seller_order`에 쿠폰을 연결한다.
쿠폰 최소 주문 금액 조건은 할인 적용 전 `seller_order.total_amount`를 기준으로 검증한다.

이 구조를 통해 빠른 동시성 제어와 영속 데이터 정합성을 함께 확보한다.
Redis는 동시성 제어의 전면, DB는 최종 기준 데이터 저장소 역할을 맡는다.
`coupon_policy.issued_quantity`의 최종 정합성 기준은 DB이며, Redis 카운터와 불일치가 발생하면 DB 기준으로 Redis를 보정한다.

## 9. 주문 아키텍처

장바구니에는 여러 판매자 상품을 담을 수 있지만, 체크아웃 시 고객 관점의 `checkout_order`를 먼저 생성한 뒤 내부적으로 판매자별 주문으로 분리한다.

- 사용자 관점
  한 번의 체크아웃 = 하나의 주문
- 시스템 관점
  하나의 `checkout_order` 아래에 여러 개의 `seller_order` 생성
- 데이터 관점
  `checkout_order`와 `seller_order`를 분리해 표현

장바구니 체크아웃이 성공하면 해당 체크아웃에 포함된 `cart_item`만 삭제한다.
바로 구매는 장바구니를 거치지 않는 별도 진입점이지만, 내부 주문 생성 규칙은 동일하게 적용한다.

`order_item`은 상품 스냅샷을 저장해, 상품 정보가 변경되거나 soft delete되어도 과거 주문 이력이 흔들리지 않도록 한다.
`checkout_order` 취소 시에는 하위의 모든 `seller_order`가 함께 취소되어 주문/재고/쿠폰 정합성을 유지한다.

## 10. 정산 배치 아키텍처

정산 기능은 Spring Batch를 사용한다.

배치 구조는 다음을 기본으로 한다.

- `Job`
  정산 배치 실행 단위
- `Step`
  정산 대상 조회 및 처리
- `Chunk`
  일정 단위로 주문을 읽고 정산 결과를 저장
- `Restartability`
  재실행 시 이미 정산된 주문은 제외
- `Batch History`
  `settlement_batch`에 실행 이력 기록

정산 대상은 `COMPLETED` 상태이면서 미정산 `seller_order`로 한정한다.
MVP에서 정산 금액은 `order_amount - discount_amount`로 계산하며, 플랫폼 수수료는 포함하지 않는다.
배치 재실행 시 중복 정산을 방지해야 하며, 실행 성공과 실패 이력을 남긴다.

참고:
- [Spring Batch Docs](https://github.com/spring-projects/spring-batch/tree/main/spring-batch-docs)

## 11. 예외 및 응답 아키텍처

API 응답은 전역 공통 계약으로 통일한다.

- 성공 응답
  일관된 응답 형식을 사용한다.
- 실패 응답
  에러 코드, 메시지, 검증 오류 정보 등을 공통 구조로 제공한다.
- 예외 처리
  `GlobalExceptionHandler`에서 전역 변환한다.

저장소 예외 매핑 원칙:

- 이메일 중복처럼 도메인 의미가 확정된 저장 예외는 저장소 어댑터에서 `BusinessException`으로 변환한다.
- 나머지 저장 예외는 오분류를 피하기 위해 인프라 예외를 유지한다.

핵심은 인증, 인가, 검증, 비즈니스 예외, 시스템 예외를 일관된 형식으로 노출하는 것이다.

## 12. 설계 원칙 요약

- 기술보다 도메인 경계를 먼저 나눈다.
- 트랜잭션은 application service에서 관리한다.
- 도메인 규칙은 domain 계층에 최대한 가깝게 둔다.
- 동시성, 보안, 정산처럼 포트폴리오 핵심 주제는 문서와 코드에서 모두 명시적으로 드러낸다.
- MVP에서는 구조를 과하게 일반화하지 않고, 설명 가능한 설계를 우선한다.

## 13. 참고 자료

- [Spring Modulith](https://github.com/spring-projects/spring-modulith)
- [Spring Data JPA Transactions](https://github.com/spring-projects/spring-data-jpa/blob/main/src/main/antora/modules/ROOT/pages/jpa/transactions.adoc)
- [Spring Security Getting Started](https://docs.spring.io/spring-security/reference/servlet/getting-started.html)
- [Spring Security Method Security](https://docs.spring.io/spring-security/reference/servlet/authorization/method-security.html)
- [Spring Security Session Management](https://docs.spring.io/spring-security/reference/servlet/authentication/session-management.html)
- [Spring Security CSRF](https://docs.spring.io/spring-security/reference/servlet/exploits/csrf.html)
- [Spring Session Redis](https://spring.io/blog/2015/03/01/the-portable-cloud-ready-http-session)
- [Spring Batch Docs](https://github.com/spring-projects/spring-batch/tree/main/spring-batch-docs)

## 14. DB 스키마 마이그레이션 운영 원칙

운영/스테이징처럼 `ddl-auto: validate`를 사용하는 환경의 스키마 변경은 Flyway 버전드 SQL로 관리한다.
로컬/테스트 프로필은 기존 개발 생산성을 위해 `create-drop`을 유지하고 Flyway를 비활성화한다.

`users.email` 유니크 제약명 표준화 원칙:

- 표준 제약명은 `uk_users_email`로 고정한다.
- 마이그레이션은 현재 환경의 기존 유니크 인덱스 이름을 조회해 rename 또는 create를 수행한다.
- `users.email` 유니크 인덱스가 복수로 감지되면 자동 수정하지 않고 실패시켜 운영 점검 후 수동 정리한다.

운영 반영 절차:

1. 배포 전 점검 SQL로 현재 `users.email` 유니크 인덱스 이름을 확인한다.
2. 애플리케이션 기동 시 Flyway가 마이그레이션을 수행한다.
3. 배포 후 `uk_users_email` 존재 여부와 Flyway 이력 테이블 반영 상태를 확인한다.

배포 전 점검 SQL:

```sql
SELECT s.index_name, GROUP_CONCAT(s.column_name ORDER BY s.seq_in_index) AS columns
FROM information_schema.statistics s
WHERE s.table_schema = DATABASE()
  AND s.table_name = 'users'
  AND s.non_unique = 0
GROUP BY s.index_name
ORDER BY s.index_name;
```

배포 후 검증 SQL:

```sql
SELECT s.index_name
FROM information_schema.statistics s
WHERE s.table_schema = DATABASE()
  AND s.table_name = 'users'
  AND s.non_unique = 0
  AND s.column_name = 'email'
GROUP BY s.index_name;

SELECT version, description, success
FROM flyway_schema_history
ORDER BY installed_rank DESC
LIMIT 5;
```
