# fromVillage API Design

## 1. 문서 목적

본 문서는 fromVillage MVP의 HTTP API 설계를 정의한다.
엔드포인트 구조, 인증 방식, 공통 응답 형식, 페이지네이션 규칙, 도메인별 API 범위를 문서화한다.

## 2. 기본 규칙

- 모든 API 기본 경로는 `/api/v1`이다.
- 관리자 API는 `/api/v1/admin/**` 경로를 사용한다.
- 보호된 API는 서버 세션 기반으로 인증한다.
- 세션 식별자는 HttpOnly 쿠키로만 전달한다.
- 세션 식별자는 응답 바디와 URL에 노출하지 않는다.
- 상태 변경 요청은 `/api/v1/csrf` 응답의 `headerName` 값으로 CSRF 토큰을 함께 전달해야 한다. 현재 기본값은 `X-CSRF-TOKEN`이다.
- 클라이언트는 로그인 전과 로그인/로그아웃 직후 `/api/v1/csrf`를 호출해 최신 CSRF 토큰을 다시 조회해야 한다.
- 성공/실패 응답은 모두 공통 래퍼 형식으로 통일한다.
- 목록 조회는 `page`, `size`, `sort` 기반 페이지네이션을 사용한다.
- 세션 쿠키 속성, 세션 만료 정책, 로그인 잠금 정책의 상세값은 `docs/adr/001-auth-strategy.md`를 따른다.

## 3. 공통 응답 형식

### 3.1 성공 응답

```json
{
  "success": true,
  "code": "SUCCESS",
  "message": "요청이 성공했습니다.",
  "data": {}
}
```

### 3.2 실패 응답

```json
{
  "success": false,
  "code": "ORDER_NOT_FOUND",
  "message": "주문을 찾을 수 없습니다.",
  "data": null,
  "errors": []
}
```

인증/인가 실패 규칙:

- 인증되지 않은 경우 `401 Unauthorized` + `AUTH_UNAUTHORIZED`
- 세션이 만료된 경우 `401 Unauthorized` + `AUTH_SESSION_EXPIRED`
- CSRF 토큰이 없거나 유효하지 않은 경우 `403 Forbidden` + `AUTH_CSRF_INVALID`
- 인증은 되었지만 권한이 부족한 경우 `403 Forbidden` + `AUTH_FORBIDDEN`
- 로그인 잠금 상태의 로그인 요청은 `401 Unauthorized`와 `AUTH_LOGIN_TEMPORARILY_LOCKED` 에러 코드로 응답
- 세션 쿠키가 없는 보호 자원 요청은 `AUTH_UNAUTHORIZED`로 처리한다.
- 세션 쿠키는 존재하지만 Redis에 세션이 없거나 만료된 세션 ID가 제출된 경우는 `AUTH_SESSION_EXPIRED`로 처리한다.
- 동시 로그인 제한으로 기존 세션이 만료된 경우도 `AUTH_SESSION_EXPIRED`로 처리한다.

### 3.3 검증 실패 응답

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

## 4. 페이지네이션 응답 형식

목록 조회 응답은 공통 래퍼 안에 목록과 페이지 정보를 함께 담는다.

```json
{
  "success": true,
  "code": "SUCCESS",
  "message": "조회에 성공했습니다.",
  "data": {
    "content": [],
    "page": 0,
    "size": 20,
    "totalElements": 100,
    "totalPages": 5,
    "hasNext": true
  }
}
```

기본 파라미터 규칙:

- `page`
  0부터 시작하는 페이지 번호
- `size`
  페이지 크기
- `sort`
  정렬 기준. 예: `createdAt,desc`, `price,asc`

## 5. 인증 API

### 5.1 회원가입

- `POST /api/v1/auth/signup`
- 성공 시 `201 Created`
- 인증 불필요
- CSRF 토큰 필요

요청 검증 규칙:

- `password`는 8자 이상 20자 이하
- `password`는 영문 대문자, 영문 소문자, 숫자, 특수문자 중 3종 이상 포함
- `password`는 공백 불가
- `email`은 필수이며 이메일 형식이어야 하고 320자 이하여야 한다.
- `nickname`은 필수이며 50자 이하여야 한다.

대표 검증 메시지:

- `비밀번호는 8자 이상 20자 이하로 입력해 주세요.`
- `비밀번호에는 공백을 사용할 수 없습니다.`
- `비밀번호는 영문 대문자, 영문 소문자, 숫자, 특수문자 중 3가지 이상을 포함해야 합니다.`
- `이메일 형식이 올바르지 않습니다.`
- `이메일은 320자 이하로 입력해 주세요.`
- `닉네임이 입력되지 않았습니다.`
- `닉네임은 50자 이하로 입력해 주세요.`

요청:

```json
{
  "email": "user@example.com",
  "password": "Password123!",
  "nickname": "fromvillage"
}
```

응답 데이터 예시:

```json
{
  "email": "user@example.com",
  "nickname": "fromvillage",
  "role": "USER"
}
```

중복 이메일 실패 예시:

```json
{
  "success": false,
  "code": "USER_EMAIL_ALREADY_EXISTS",
  "message": "이미 사용 중인 이메일입니다.",
  "data": null,
  "errors": []
}
```

### 5.2 로그인

- `POST /api/v1/auth/login`
- 인증 불필요
- CSRF 토큰 필요

요청:

```json
{
  "email": "user@example.com",
  "password": "Password123!"
}
```

설명:

- 로그인 성공 시 서버는 세션 쿠키를 설정한다.
- 로그인 성공 시 세션 ID는 재발급된다.
- 로그인 잠금 동작과 상세 수치는 `docs/adr/001-auth-strategy.md`를 따른다.
- 자격 증명 실패가 5회 연속 누적되면 5번째 요청부터 `AUTH_LOGIN_TEMPORARILY_LOCKED`를 반환한다.
- 로그인 요청 본문 형식 오류, 필수값 누락, CSRF 실패는 로그인 실패 횟수에 포함하지 않는다.
- 이메일 또는 비밀번호가 올바르지 않으면 `401 Unauthorized`와 `AUTH_UNAUTHORIZED`로 응답한다.

응답 데이터 예시:

```json
{
  "email": "user@example.com",
  "nickname": "fromvillage",
  "role": "USER"
}
```

로그인 실패 예시:

```json
{
  "success": false,
  "code": "AUTH_UNAUTHORIZED",
  "message": "이메일 또는 비밀번호가 올바르지 않습니다.",
  "data": null,
  "errors": []
}
```

### 5.3 CSRF 토큰 조회

- `GET /api/v1/csrf`
- 인증 불필요

설명:

- 브라우저 클라이언트는 애플리케이션 초기 진입 시 CSRF 토큰을 먼저 조회한다.
- 로그인 성공과 로그아웃 성공 이후에는 새 세션 기준으로 CSRF 토큰을 다시 조회해야 한다.
- 상태 변경 요청은 이 응답의 `headerName` 값을 그대로 사용한다.

응답 데이터 예시:

```json
{
  "headerName": "X-CSRF-TOKEN",
  "parameterName": "_csrf",
  "token": "csrf-token-value"
}
```

### 5.4 로그아웃

- `POST /api/v1/auth/logout`
- 인증 필요
- CSRF 토큰 필요
- 요청 바디 없음

설명:

- 로그아웃 시 현재 세션이 서버에서 무효화된다.

응답 데이터 예시:

```json
{
  "success": true,
  "code": "SUCCESS",
  "message": "요청이 성공했습니다.",
  "data": null
}
```

## 6. 상품 API

### 6.1 상품 목록 조회

- `GET /api/v1/products`
- 인증 불필요

쿼리 파라미터:

- `keyword`
  상품명 키워드 검색
- `category`
  `AGRICULTURE`, `FISHERY`
- `page`
- `size`
- `sort`
  `createdAt,desc`, `price,asc`, `price,desc`

### 6.2 상품 상세 조회

- `GET /api/v1/products/{productId}`
- 인증 불필요

설명:

- soft delete된 상품은 공개 상세 조회에서 `404 Not Found`로 응답한다.

### 6.3 상품 등록

- `POST /api/v1/products`
- `SELLER`
- CSRF 토큰 필요

요청 필드:

- `name`
- `description`
- `category`
- `price`
- `stockQuantity`
- `imageUrl`
  `https` URL만 허용하며, 서버는 외부 URL을 직접 fetch하지 않는다.

### 6.4 내 상품 목록 조회

- `GET /api/v1/seller/products`
- `SELLER`
- `page`, `size`, `sort` 지원

설명:

- 본인 소유 상품만 조회한다.
- 관리 목적 조회이므로 soft delete된 상품도 포함하되, `deletedAt` 또는 삭제 여부를 함께 반환한다.

응답 데이터 예시:

```json
{
  "content": [
    {
      "productId": 12,
      "name": "완도 활전복 1kg",
      "category": "FISHERY",
      "price": 39000,
      "stockQuantity": 0,
      "status": "SOLD_OUT",
      "deletedAt": null
    },
    {
      "productId": 18,
      "name": "유기농 감자 5kg",
      "category": "AGRICULTURE",
      "price": 22000,
      "stockQuantity": 12,
      "status": "ON_SALE",
      "deletedAt": "2026-03-07T10:15:30"
    }
  ],
  "page": 0,
  "size": 20,
  "totalElements": 2,
  "totalPages": 1,
  "hasNext": false
}
```

### 6.5 상품 수정

- `PUT /api/v1/products/{productId}`
- 본인 상품 소유 `SELLER`
- CSRF 토큰 필요

### 6.6 상품 삭제

- `DELETE /api/v1/products/{productId}`
- 본인 상품 소유 `SELLER`
- CSRF 토큰 필요

설명:

- 상품 삭제는 hard delete가 아니라 soft delete로 처리한다.
- soft delete된 상품은 공개 상품 목록/상세, 장바구니 담기, 바로 구매, 체크아웃 대상에서 제외한다.
- 기존 주문 이력 조회를 위해 상품 레코드는 DB에 유지한다.

## 7. 장바구니 API

### 7.1 장바구니 조회

- `GET /api/v1/cart-items`
- 인증 필요

### 7.2 장바구니 담기

- `POST /api/v1/cart-items`
- 인증 필요
- CSRF 토큰 필요

요청:

```json
{
  "productId": 1,
  "quantity": 2
}
```

### 7.3 장바구니 수량 수정

- `PATCH /api/v1/cart-items/{cartItemId}`
- 인증 필요
- CSRF 토큰 필요

### 7.4 장바구니 삭제

- `DELETE /api/v1/cart-items/{cartItemId}`
- 인증 필요
- CSRF 토큰 필요

## 8. 주문 API

주문은 장바구니 체크아웃과 바로 구매 두 경로를 모두 지원하지만, 내부적으로는 공통 체크아웃 유스케이스로 처리한다.
고객이 조회하는 주문 단위는 `checkout_order`이며, 내부적으로는 판매자별 `seller_order`로 분리된다.

### 8.1 장바구니 체크아웃

- `POST /api/v1/orders/checkout`
- 인증 필요
- CSRF 토큰 필요

요청:

```json
{
  "issuedCouponId": 10,
  "targetSellerId": 3
}
```

쿠폰 미사용 요청 예시:

```json
{}
```

설명:

- 장바구니에 담긴 여러 SELLER 상품을 기준으로 주문을 생성한다.
- 고객 관점에서는 하나의 주문이 생성되고, 내부적으로 SELLER별 `seller_order`로 분리된다.
- 체크아웃 성공 시 해당 주문에 포함된 장바구니 항목만 삭제된다.
- `issuedCouponId`는 발급된 개인 쿠폰 식별자이며, 쿠폰 미사용 시 생략 가능하다.
- `targetSellerId`는 쿠폰을 적용할 대상 SELLER 주문을 지정하며, 쿠폰 미사용 시 생략 가능하다.
- 쿠폰은 분리된 SELLER 주문 1건에만 적용된다.
- 발급된 개인 쿠폰은 동시에 하나의 SELLER 주문에만 연결된다.
- 쿠폰 최소 주문 금액은 할인 전 해당 `seller_order`의 상품 금액 합계 기준으로 검증한다.

### 8.2 바로 구매

- `POST /api/v1/orders/direct-checkout`
- 인증 필요
- CSRF 토큰 필요

설명:

- 바로 구매는 장바구니 항목을 생성하거나 삭제하지 않는다.
- 내부 주문 생성 규칙과 쿠폰 적용 규칙은 장바구니 체크아웃과 동일하다.
- 바로 구매는 단일 상품 기준이므로 `productId`로 SELLER가 결정되며, `targetSellerId`를 별도로 받지 않는다.
- `issuedCouponId`는 쿠폰 미사용 시 생략 가능하다.
- 쿠폰 최소 주문 금액은 할인 전 해당 상품의 주문 금액 기준으로 검증한다.

요청:

```json
{
  "productId": 1,
  "quantity": 2,
  "issuedCouponId": 10
}
```

### 8.3 내 주문 목록 조회

- `GET /api/v1/orders`
- 인증 필요
- `page`, `size`, `sort` 지원

### 8.4 내 주문 상세 조회

- `GET /api/v1/orders/{orderId}`
- 고객 관점 `checkout_order` 상세를 조회한다.
- 본인 주문 `USER`, 전체 조회 `ADMIN`

### 8.5 주문 취소

- `POST /api/v1/orders/{orderId}/cancel`
- 고객 관점 `checkout_order`를 기준으로 취소한다.
- 본인 주문 `USER`
- CSRF 토큰 필요
- `COMPLETED` 상태 주문만 취소할 수 있다.
- 취소 시 하위의 모든 `seller_order`도 함께 `CANCELED` 처리된다.

### 8.6 내 판매자 주문 목록 조회

- `GET /api/v1/seller-orders`
- `SELLER`
- 본인 상품이 포함된 `seller_order`만 조회한다.
- `page`, `size`, `sort` 지원
- 구매자 정보는 닉네임만 포함한다.

### 8.7 내 판매자 주문 상세 조회

- `GET /api/v1/seller-orders/{sellerOrderId}`
- `SELLER`
- 본인 상품이 포함된 `seller_order`만 조회한다.
- 구매자 정보는 닉네임만 포함하며, 이메일/전화번호/주소는 제외한다.

## 9. 쿠폰 API

### 9.1 발급 가능한 쿠폰 목록 조회

- `GET /api/v1/coupons`
- 인증 필요

### 9.2 쿠폰 발급 요청

- `POST /api/v1/coupons/{couponPolicyId}/issue`
- 인증 필요
- CSRF 토큰 필요

### 9.3 내 보유 쿠폰 조회

- `GET /api/v1/coupons/me`
- 인증 필요
- 사용 중이 아닌 쿠폰만 주문에 적용 가능하다.

## 10. 관리자 API

관리자 API는 `/api/v1/admin/**` 경로를 사용한다.

### 10.1 회원 목록 조회

- `GET /api/v1/admin/users`
- `ADMIN`
- `page`, `size`, `sort` 지원
- `sort` 미지정 시 `createdAt,desc`를 기본값으로 사용한다.
- 응답 항목은 `userId`, `email`, `nickname`, `role`, `sellerApprovedAt`, `createdAt`만 포함한다.

응답 예시:

```json
{
  "success": true,
  "code": "SUCCESS",
  "message": "요청이 성공했습니다.",
  "data": {
    "content": [
      {
        "userId": 1,
        "email": "admin@example.com",
        "nickname": "운영자",
        "role": "ADMIN",
        "sellerApprovedAt": null,
        "createdAt": "2026-03-09T00:00:00"
      },
      {
        "userId": 2,
        "email": "seller@example.com",
        "nickname": "판매자",
        "role": "SELLER",
        "sellerApprovedAt": "2026-03-09T00:00:00",
        "createdAt": "2026-03-09T00:05:00"
      }
    ],
    "page": 0,
    "size": 20,
    "totalElements": 2,
    "totalPages": 1,
    "hasNext": false
  }
}
```

### 10.2 판매자 권한 부여

- `POST /api/v1/admin/users/{userId}/seller-role`
- `ADMIN`
- CSRF 토큰 필요
- 대상 계정의 역할은 `USER -> SELLER`로 전환된다.
- 이미 SELLER인 계정에는 다시 판매자 권한을 부여하지 않는다.
- ADMIN 계정은 SELLER로 전환하지 않는다.
- 존재하지 않는 `userId`는 `404 + USER_NOT_FOUND`를 반환한다.

성공 응답 예시:

```json
{
  "success": true,
  "code": "SUCCESS",
  "message": "요청이 성공했습니다.",
  "data": {
    "userId": 3,
    "email": "user@example.com",
    "nickname": "일반회원",
    "role": "SELLER",
    "sellerApprovedAt": "2026-03-09T00:00:00",
    "createdAt": "2026-03-08T21:00:00"
  }
}
```

### 10.3 쿠폰 정책 생성

- `POST /api/v1/admin/coupon-policies`
- `ADMIN`
- CSRF 토큰 필요

요청 필드:

- `name`
- `discountAmount`
- `minimumOrderAmount`
- `totalQuantity`
- `startedAt`
- `endedAt`

### 10.4 쿠폰 정책 오픈

- `POST /api/v1/admin/coupon-policies/{couponPolicyId}/open`
- `ADMIN`
- CSRF 토큰 필요

### 10.5 쿠폰 정책 종료

- `POST /api/v1/admin/coupon-policies/{couponPolicyId}/close`
- `ADMIN`
- CSRF 토큰 필요

### 10.6 주문 목록 조회

- `GET /api/v1/admin/orders`
- `ADMIN`
- `page`, `size`, `sort` 지원

### 10.7 정산 배치 실행

- `POST /api/v1/admin/settlements/batch`
- `ADMIN`
- CSRF 토큰 필요

### 10.8 정산 배치 이력 조회

- `GET /api/v1/admin/settlements/batches`
- `ADMIN`
- `page`, `size`, `sort` 지원

## 11. 권한 규칙 요약

- 공개 API
  회원가입, 로그인, CSRF 토큰 조회, 상품 목록 조회, 상품 상세 조회
- USER
  장바구니, 주문, 쿠폰 발급 및 사용, 본인 주문 조회/취소
- SELLER
  본인 상품 등록/수정/삭제, 본인 상품 재고 관리, 본인 판매자 주문 조회
  USER 구매 기능은 수행하지 않음
- ADMIN
  관리자 경로 전용 기능, 회원/주문/쿠폰 정책/정산 운영
  일반 구매자/판매자 기능은 수행하지 않음

## 12. 문서화 기준

- OpenAPI 문서 기준 경로는 `/api-docs`
- Swagger UI 경로는 `/swagger-ui.html`
- 일반 API와 관리자 API는 springdoc 그룹 분리 가능성을 고려한다.
- 세션 쿠키 인증과 CSRF 헤더 요구사항을 OpenAPI에 명시한다.

## 13. 참고 자료

- [Spring Data JPA](https://github.com/spring-projects/spring-data-jpa)
- [Springdoc OpenAPI](https://github.com/springdoc/springdoc-openapi)
- [Spring Security CSRF](https://docs.spring.io/spring-security/reference/servlet/exploits/csrf.html)
