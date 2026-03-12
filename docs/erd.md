# fromVillage ERD

## 1. 문서 목적

본 문서는 fromVillage MVP의 핵심 데이터 모델을 정의한다.
본 ERD는 `one-pager`, `glossary`, `prd`에서 확정한 역할, 기능 범위, 비즈니스 규칙을 기준으로 작성한다.

## 2. 설계 원칙

- MVP 범위를 벗어나는 확장 모델은 배제하고 핵심 도메인만 표현한다.
- 인증, 상품, 장바구니, 주문, 쿠폰, 정산을 중심으로 테이블을 구성한다.
- 주문/쿠폰/정산은 상태 전이와 이력 추적이 가능하도록 설계한다.
- 체크아웃은 여러 판매자 상품을 담을 수 있지만, 내부적으로는 판매자별 주문으로 분리한다.
- Redis는 쿠폰 동시성 처리에 사용하되, 영속 데이터의 기준은 RDB 테이블로 둔다.

## 3. 핵심 테이블

### 3.1 users

회원 정보를 저장한다.
구매자, 판매자, 관리자 모두 하나의 테이블에서 관리하며, 역할은 role 컬럼으로 구분한다.
MVP에서는 한 계정이 동시에 여러 역할을 가지지 않고, 하나의 role 값만 가진다.
ADMIN 계정은 공개 회원가입으로 생성하지 않고, 초기 시드 또는 운영 초기화 절차를 통해 주입한다.
회원 계정은 MVP 현재 범위에서 `deleted_at` soft delete를 두지 않으며, 계정 탈퇴/비활성화는 별도 lifecycle 정책으로 다룬다.

| 컬럼명 | 타입 | 설명 |
| --- | --- | --- |
| id | bigint | PK |
| email | varchar | 로그인 이메일, unique |
| password | varchar | 암호화된 비밀번호 |
| nickname | varchar | 사용자 닉네임 |
| role | varchar | `USER`, `SELLER`, `ADMIN` |
| seller_approved_at | datetime | USER에서 SELLER로 역할 전환된 시각, nullable |
| created_at | datetime | 생성 시각 |
| updated_at | datetime | 수정 시각 |

제약:
- `email` unique (`uk_users_email`)

### 3.2 server_session

세션 인증 상태는 Spring Session을 통해 Redis에 저장한다.
이 저장소는 RDB ERD의 핵심 테이블은 아니지만, 인증/인가 설계 설명을 위해 개념적으로 함께 정리한다.

| 필드명 | 타입 | 설명 |
| --- | --- | --- |
| session_id | string | 세션 식별자 |
| user_id | bigint | 인증 사용자 식별자 |
| role | varchar | 현재 역할 정보 |
| created_at | datetime | 세션 생성 시각 |
| last_accessed_at | datetime | 마지막 접근 시각 |
| expires_at | datetime | 세션 만료 시각 |
| csrf_token | string | 세션과 연계된 CSRF 토큰 |

메모:
- 세션 식별자는 HttpOnly 쿠키로만 전달한다.
- 세션 데이터와 CSRF 토큰은 Spring Session + Redis가 관리한다.
- 로그인 성공 시 세션 ID가 재발급된다.
- 세션 쿠키 속성, 세션 만료, 동시 세션 제한의 상세 정책은 `docs/adr/001-auth-strategy.md`를 따른다.

### 3.3 login_failure_state

로그인 실패 횟수와 잠금 상태는 Redis에 저장한다.
이 저장소는 인증 보안 정책 설명을 위한 개념 모델이다.

| 필드명 | 타입 | 설명 |
| --- | --- | --- |
| user_key | string | 사용자 식별 키 |
| failed_count | int | 연속 로그인 실패 횟수 |
| locked_until | datetime | 로그인 잠금 해제 시각, nullable |
| updated_at | datetime | 마지막 갱신 시각 |

메모:
- 로그인 실패가 누적될 때마다 `failed_count`가 증가한다.
- 실패 횟수 증가, 잠금 시각 계산, 잠금 TTL 설정은 Redis에서 원자적으로 처리한다.
- 잠금 임계값과 잠금 시간의 상세 정책은 `docs/adr/001-auth-strategy.md`를 따른다.
- 로그인 성공 시 실패 횟수는 초기화된다.

### 3.4 products

판매자가 등록한 상품 정보를 저장한다.
MVP에서는 대표 이미지 1장만 지원하며, 상품 카테고리는 enum으로 관리한다.

| 컬럼명 | 타입 | 설명 |
| --- | --- | --- |
| id | bigint | PK |
| seller_id | bigint | FK -> users.id |
| name | varchar | 상품명 |
| description | text | 상품 설명 |
| category | varchar | `AGRICULTURE`, `FISHERY` |
| price | bigint | 정가 |
| stock_quantity | int | 재고 수량 |
| status | varchar | `ON_SALE`, `SOLD_OUT` |
| image_url | varchar | 대표 이미지 URL, `https`만 허용 |
| deleted_at | datetime | soft delete 시각, nullable |
| created_at | datetime | 생성 시각 |
| updated_at | datetime | 수정 시각 |

메모:
- 상품 삭제는 물리 삭제가 아니라 `deleted_at`을 설정하는 soft delete로 처리한다.
- soft delete된 상품은 공개 목록/상세 조회와 신규 주문 흐름에서는 제외한다.
- 과거 주문 이력의 `order_item.product_id` 참조 보존을 위해 상품 레코드는 유지한다.

### 3.5 cart_item

장바구니에 담긴 상품 정보를 저장한다.
장바구니는 USER별로 관리하며, 여러 판매자의 상품을 함께 담을 수 있다.
장바구니 수량은 1 이상이어야 하며, 같은 USER는 같은 상품을 하나의 장바구니 항목으로만 가진다.

| 컬럼명 | 타입 | 설명 |
| --- | --- | --- |
| id | bigint | PK |
| user_id | bigint | FK -> users.id |
| product_id | bigint | FK -> products.id |
| quantity | int | 담은 수량 |
| created_at | datetime | 생성 시각 |
| updated_at | datetime | 수정 시각 |

제약:
- `(user_id, product_id)` unique

메모:
- 기본 장바구니 조회는 USER 기준 전체 항목을 대상으로 한다.
- 후속 장바구니 조회/체크아웃에서는 soft delete된 상품을 제외하는 active 조회를 재사용한다.

### 3.6 checkout_order

고객 관점의 주문 단위를 저장한다.
사용자는 한 번의 체크아웃 결과를 하나의 주문으로 인식하며, 내부적으로 여러 판매자 주문을 포함할 수 있다.

| 컬럼명 | 타입 | 설명 |
| --- | --- | --- |
| id | bigint | PK |
| user_id | bigint | FK -> users.id |
| status | varchar | `CREATED`, `COMPLETED`, `CANCELED` |
| total_amount | bigint | 주문 전체 상품 금액 합계 |
| discount_amount | bigint | 주문 전체 할인 금액 |
| final_amount | bigint | 주문 전체 최종 결제 금액 |
| completed_at | datetime | 주문 완료 시각, nullable |
| canceled_at | datetime | 주문 취소 시각, nullable |
| created_at | datetime | 생성 시각 |
| updated_at | datetime | 수정 시각 |

### 3.7 seller_order

체크아웃 결과 생성되는 판매자 단위 주문이다.
사용자에게는 하나의 `checkout_order`로 보이지만, 내부적으로는 판매자별 주문으로 분리한다.
현재 구현 기준으로 `issued_coupon_id` 연관은 아직 포함하지 않고, 쿠폰 연결 컬럼과 unique 제약은 `M4-04 주문 쿠폰 적용 및 취소 시 복구 구현`에서 추가한다.

| 컬럼명 | 타입 | 설명 |
| --- | --- | --- |
| id | bigint | PK |
| checkout_order_id | bigint | FK -> checkout_order.id |
| seller_id | bigint | FK -> users.id |
| status | varchar | `CREATED`, `COMPLETED`, `CANCELED` |
| total_amount | bigint | 상품 금액 합계 |
| discount_amount | bigint | 쿠폰 할인 금액 |
| final_amount | bigint | 최종 결제 금액 |
| completed_at | datetime | 주문 완료 시각, nullable |
| canceled_at | datetime | 주문 취소 시각, nullable |
| created_at | datetime | 생성 시각 |
| updated_at | datetime | 수정 시각 |

### 3.8 order_item

주문에 포함된 상품 정보를 저장한다.
주문 당시 값을 보존하기 위해 상품 스냅샷을 함께 저장한다.
상품이 이후 soft delete되더라도 과거 주문 이력은 `product_id` FK와 스냅샷으로 함께 보존한다.

| 컬럼명 | 타입 | 설명 |
| --- | --- | --- |
| id | bigint | PK |
| seller_order_id | bigint | FK -> seller_order.id |
| product_id | bigint | FK -> products.id |
| product_name_snapshot | varchar | 주문 당시 상품명 |
| product_price_snapshot | bigint | 주문 당시 상품 가격 |
| quantity | int | 주문 수량 |
| line_amount | bigint | 상품 금액 합계 |
| created_at | datetime | 생성 시각 |

### 3.9 coupon_policy

선착순 쿠폰 정책을 저장한다.
정책 정보와 개인 발급 이력은 분리한다.

| 컬럼명 | 타입 | 설명 |
| --- | --- | --- |
| id | bigint | PK |
| name | varchar | 쿠폰명 |
| discount_amount | bigint | 정액 할인 금액 |
| minimum_order_amount | bigint | 최소 주문 금액 |
| total_quantity | int | 총 발급 수량 |
| issued_quantity | int | 현재 발급 수량, 최종 정합성 기준은 DB |
| started_at | datetime | 발급 시작 시각 |
| ended_at | datetime | 발급 종료 시각 |
| status | varchar | `READY`, `OPEN`, `CLOSED` |
| created_by | bigint | FK -> users.id |
| created_at | datetime | 생성 시각 |
| updated_at | datetime | 수정 시각 |

### 3.10 issued_coupon

개인별 쿠폰 발급 이력을 저장한다.
쿠폰 복구 시 별도 복구 상태를 두지 않고 `ISSUED` 상태로 되돌린다.

| 컬럼명 | 타입 | 설명 |
| --- | --- | --- |
| id | bigint | PK |
| coupon_policy_id | bigint | FK -> coupon_policy.id |
| user_id | bigint | FK -> users.id |
| status | varchar | `ISSUED`, `USED` |
| issued_at | datetime | 발급 시각 |
| used_at | datetime | 사용 시각, nullable |
| created_at | datetime | 생성 시각 |
| updated_at | datetime | 수정 시각 |

제약:
- `(coupon_policy_id, user_id)` unique

### 3.11 settlement_batch

정산 배치 실행 이력을 저장한다.
관리자가 수동으로 실행한 배치의 실행 단위를 표현한다.

| 컬럼명 | 타입 | 설명 |
| --- | --- | --- |
| id | bigint | PK |
| executed_by | bigint | FK -> users.id |
| status | varchar | `STARTED`, `COMPLETED`, `FAILED` |
| target_order_count | int | 정산 대상 주문 수 |
| success_order_count | int | 정산 성공 주문 수 |
| failure_order_count | int | 정산 실패 주문 수 |
| started_at | datetime | 실행 시작 시각 |
| finished_at | datetime | 실행 종료 시각, nullable |
| failure_reason | varchar | 실패 사유, nullable |
| created_at | datetime | 생성 시각 |

### 3.12 settlement

판매자별 정산 결과를 저장한다.
정산 실행 이력과 판매자별 정산 결과를 분리해 관리한다.
MVP에서는 실제 지급 완료 워크플로우를 다루지 않고, 배치 실행 결과 생성까지만 표현한다.

| 컬럼명 | 타입 | 설명 |
| --- | --- | --- |
| id | bigint | PK |
| settlement_batch_id | bigint | FK -> settlement_batch.id |
| seller_id | bigint | FK -> users.id |
| seller_order_id | bigint | FK -> seller_order.id |
| order_amount | bigint | 주문 금액 |
| discount_amount | bigint | 할인 금액 |
| settlement_amount | bigint | 정산 금액 (`order_amount - discount_amount`) |
| created_at | datetime | 생성 시각 |

제약:
- `(seller_order_id)` unique

## 4. 테이블 관계

- `users` 1 : N `product` (`seller_id`)
- `users` 1 : N `cart_item`
- `users` 1 : N `checkout_order`
- `checkout_order` 1 : N `seller_order`
- `users` 1 : N `seller_order` (`seller_id`)
- `seller_order` 1 : N `order_item`
- `product` 1 : N `order_item`
- `coupon_policy` 1 : N `issued_coupon`
- `users` 1 : N `issued_coupon`
- `issued_coupon` 1 : 0..1 `seller_order`
  현재 구현 기준 이 관계는 아직 스키마에 반영되지 않았고 `M4-04`에서 추가된다.
- `settlement_batch` 1 : N `settlement`
- `users` 1 : N `settlement` (`seller_id`)
- `seller_order` 1 : 1 `settlement`

## 5. Enum 정의

### UserRole

- `USER`
- `SELLER`
- `ADMIN`

### ProductCategory

- `AGRICULTURE`
- `FISHERY`

### ProductStatus

- `ON_SALE`
- `SOLD_OUT`

### OrderStatus

- `CREATED`
- `COMPLETED`
- `CANCELED`

### CouponPolicyStatus

- `READY`
- `OPEN`
- `CLOSED`

### IssuedCouponStatus

- `ISSUED`
- `USED`

### SettlementBatchStatus

- `STARTED`
- `COMPLETED`
- `FAILED`

## 6. 핵심 데이터 흐름

### 6.1 주문 생성

1. USER는 장바구니에 여러 SELLER의 상품을 담을 수 있다.
2. 체크아웃 시 시스템은 고객 관점의 `checkout_order`를 생성한다.
3. 이어서 SELLER별로 `seller_order`를 생성한다.
4. 각 `seller_order` 아래에 `order_item`이 생성된다.
5. 주문 완료 시 재고가 차감되고 상태가 `COMPLETED`로 변경된다.
6. `checkout_order` 취소 시 하위의 모든 `seller_order`도 함께 `CANCELED`로 전이된다.

### 6.2 쿠폰 발급 및 사용

1. ADMIN이 `coupon_policy`를 생성하고 오픈한다.
2. USER가 발급 요청 시 Redis에서 선착순 수량과 중복 여부를 우선 검증한다.
3. 최종 발급 결과는 `issued_coupon`에 저장한다.
4. 주문 시 `issued_coupon`을 연결해 할인 금액을 반영한다. 현재 `M3-03` 주문 모델 단계에서는 이 연결 컬럼을 두지 않고, `M4-04`에서 `seller_order`와 연결한다.
5. 주문 취소 시 `issued_coupon.status`를 다시 `ISSUED`로 복구한다.
6. 하나의 `issued_coupon`은 동시에 하나의 `seller_order`에만 연결될 수 있다.
7. `coupon_policy.issued_quantity`의 최종 정합성 기준은 DB이며, Redis 카운터와 차이가 발생하면 DB 기준으로 Redis를 보정한다.

### 6.3 세션 인증 및 폐기

1. 로그인 시 서버는 사용자 인증 후 세션을 생성한다.
2. 세션 데이터와 CSRF 토큰은 Spring Session을 통해 Redis에 저장한다.
3. 로그인 성공 시 세션 ID를 재발급해 세션 고정 공격을 방어한다.
4. 동일 계정으로 새 로그인 시 기존 세션은 만료된다.
5. 로그아웃 시 현재 세션이 무효화되고, 이후 클라이언트는 새 CSRF 토큰을 다시 조회해야 한다.

### 6.4 로그인 실패 제어

1. 로그인 실패 시 Redis의 `login_failure_state.failed_count`를 증가시킨다.
2. 잠금 정책의 상세 임계값과 응답 규칙은 `docs/adr/001-auth-strategy.md`를 따른다.
3. 잠금 상태에서의 로그인 요청은 인증 정책에 정의된 에러 코드로 응답한다.
4. 로그인 성공 시 실패 횟수와 잠금 상태를 초기화한다.

### 6.5 정산 배치

1. ADMIN이 정산 배치를 수동 실행한다.
2. 시스템은 `COMPLETED` 상태이면서 아직 정산되지 않은 `seller_order`를 조회한다.
3. 배치 실행 기록은 `settlement_batch`에 저장한다.
4. 판매자별 정산 결과는 `settlement`에 저장한다.
5. `settlement` 레코드가 존재하는 주문은 이후 배치에서 제외한다.

## 7. 설계 메모

- `users`와 `seller_profile`을 분리하지 않고 단일 사용자 테이블로 유지해 MVP 복잡도를 줄인다.
- 고객 관점 주문과 판매자 내부 주문을 분리하기 위해 `checkout_order`와 `seller_order`를 함께 둔다.
- 주문 이력의 안정성을 위해 `order_item`에는 상품 스냅샷을 저장한다.
- 쿠폰은 정책과 발급 이력을 분리해 중복 발급 방지와 사용/복구 처리를 명확히 한다.
- 발급된 개인 쿠폰의 단일 사용성은 최종 MVP에서 `seller_order`와의 단일 연결로 보장하며, 현재 구현 시점에서는 해당 컬럼과 unique 제약을 `M4-04`로 유예한다.
- `coupon_policy.issued_quantity`는 최종적으로 DB 값을 기준으로 관리하고, Redis는 발급 제어용 카운터로 사용한다.
- 세션 인증 상태는 RDB가 아니라 Spring Session + Redis가 관리한다.
- 로그인 실패 잠금 상태 또한 Redis 개념 저장소로 관리한다.
- 세션 쿠키는 HttpOnly 기반으로 전달하고, URL 기반 세션 추적은 사용하지 않는다.
- 정산은 배치 실행 이력과 정산 결과를 분리해 운영 추적성을 높이며, MVP에서는 지급 완료 단계까지 확장하지 않는다.

## 8. 참고 자료

- [Spring Data JPA](https://github.com/spring-projects/spring-data-jpa)
- [Spring Security Session Management](https://docs.spring.io/spring-security/reference/servlet/authentication/session-management.html)
- `docs/adr/001-auth-strategy.md`
- `docs/adr/002-coupon-concurrency.md`
- `docs/adr/003-settlement-batch.md`
