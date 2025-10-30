# 커피 원두 판매 시스템 시퀀스 다이어그램

## 1. 상품 조회 및 탐색

고객이 메인 페이지에 접속하여 인기 상품을 확인하고, 상품 상세 페이지에서 옵션별 상세 정보를 조회하는 기능

```mermaid
sequenceDiagram
    actor Customer as 고객
    participant Web as Web/App
    participant ProductAPI as 상품 API
    participant ProductService as 상품 서비스
    participant InventoryService as 재고 서비스
    participant DB as Database

    Customer->>Web: 메인 페이지 접속
    Web->>ProductAPI: GET /products/top-selling?days=3&limit=5
    ProductAPI->>ProductService: 인기 상품 조회 요청
    ProductService->>DB: 최근 3일 판매량 기준 조회
    DB-->>ProductService: Top 5 상품 목록
    ProductService-->>ProductAPI: 상품 목록 반환
    ProductAPI-->>Web: 200 OK (상품 목록)
    Web-->>Customer: 인기 상품 Top 5 표시

    Customer->>Web: 상품 클릭 (예: 에티오피아 예가체프 G1)
    Web->>ProductAPI: GET /products/{productId}
    ProductAPI->>ProductService: 상품 상세 조회
    ProductService->>DB: 상품 정보 및 옵션 조회
    DB-->>ProductService: 상품 상세 정보 (옵션 포함)
    ProductService-->>ProductAPI: 상품 상세 반환
    ProductAPI-->>Web: 200 OK (상품 상세)
    Web-->>Customer: 상품 상세 페이지 표시

    Customer->>Web: 옵션 선택 (핸드드립, 200g)
    Web->>ProductAPI: GET /products/options/{optionCode}
    ProductAPI->>InventoryService: 옵션별 재고 및 가격 조회
    InventoryService->>DB: 해당 수량 & 가격 조회
    DB-->>InventoryService: 재고 수량 (8개), 가격(21,000원)
    InventoryService-->>ProductAPI: 재고 & 가격 정보 반환
    ProductAPI-->>Web: 200 OK (가격: 21,000원, 재고: 8개)
    Web-->>Customer: 선택 옵션의 가격과 재고 표시
```

---

## 2. 쿠폰 발급

고객이 쿠폰 배너를 클릭하여 쿠폰을 발급하는 기능 (선착순 발급 상황 고려)

```mermaid
sequenceDiagram
    actor Customer as 고객
    participant Web as Web/App
    participant CouponAPI as 쿠폰 API
    participant CouponService as 쿠폰 서비스
    participant DB as Database

    Customer->>Web: 쿠폰 배너 클릭
    Web->>CouponAPI: POST /coupons/{couponId}/issue
    CouponAPI->>CouponService: 쿠폰 발급 요청

    CouponService->>DB: 쿠폰 발급 가능 여부 확인 (동시성 제어)
    DB-->>CouponService: 발급 가능 여부

    alt 발급 가능
        CouponService-->>CouponAPI: 발급 성공
        CouponAPI-->>Web: 201 Created (쿠폰 정보)
        Web-->>Customer: "쿠폰 발급이 완료되었습니다"

    else 발급 불가 (수량 소진 또는 중복 발급)
        CouponService-->>CouponAPI: 발급 실패
        CouponAPI-->>Web: 409 Conflict
        Web-->>Customer: "쿠폰이 모두 소진되었습니다"
    end
```

---

## 3. 장바구니 관리

고객이 선택한 상품을 장바구니에 담을 수 있고, 장바구니에 담은 상품을 조회하거나 삭제할 수 있는 기능

```mermaid
sequenceDiagram
    actor Customer as 고객
    participant Web as Web/App
    participant CartAPI as 장바구니 API
    participant CartService as 장바구니 서비스
    participant ProductService as 상품 서비스
    participant InventoryService as 재고 서비스
    participant DB as Database

    Customer->>Web: 장바구니 담기 버튼 클릭
    Web->>CartAPI: POST /cart/items
    Note over CartAPI: {productId, optionId, quantity}

    CartAPI->>CartService: 장바구니 추가 요청
    CartService->>ProductService: 상품 및 옵션 유효성 검증
    ProductService->>DB: 상품/옵션 존재 여부 확인
    DB-->>ProductService: 상품 정보
    ProductService-->>CartService: 유효성 확인 완료

    CartService->>DB: 장바구니 아이템 저장
    DB-->>CartService: 저장 완료

    CartService-->>CartAPI: 추가 성공
    CartAPI-->>Web: 201 Created
    Web-->>Customer: "장바구니에 추가되었습니다"

    Customer->>Web: 장바구니 페이지 이동
    Web->>CartAPI: GET /cart
    CartAPI->>CartService: 장바구니 조회
    CartService->>DB: 사용자 장바구니 아이템 조회
    DB-->>CartService: 장바구니 목록
    CartService->>ProductService: 각 상품의 기본 정보 조회
    ProductService-->>CartService: 상품 정보 (이름, 이미지 등)
    CartService->>InventoryService: 각 옵션의 재고 및 가격 조회
    InventoryService-->>CartService: 재고 및 가격 정보
    CartService-->>CartAPI: 장바구니 목록 반환
    CartAPI-->>Web: 200 OK (장바구니 목록)
    Web-->>Customer: 장바구니 내역 표시

    Customer->>Web: 특정 상품 삭제 버튼 클릭
    Web->>CartAPI: DELETE /cart/items/{cartItemId}
    CartAPI->>CartService: 장바구니 아이템 삭제 요청
    CartService->>DB: 장바구니 아이템 삭제
    DB-->>CartService: 삭제 완료
    CartService-->>CartAPI: 삭제 성공
    CartAPI-->>Web: 204 No Content
    Web-->>Customer: 장바구니에서 상품 제거 (화면 갱신)
```

---

## 4. 주문 및 결제

고객이 장바구니에서 주문을 생성하고 주문 정보를 입력한 뒤, 쿠폰 선택 후 결제할 수 있는 기능

```mermaid
sequenceDiagram
    actor Customer as 고객
    participant Web as Web/App
    participant OrderAPI as 주문 API
    participant OrderService as 주문 서비스
    participant InventoryService as 재고 서비스
    participant CouponService as 쿠폰 서비스
    participant PaymentService as 결제 서비스
    participant DB as Database

    Customer->>Web: 장바구니에서 "주문하기" 클릭
    Web->>Customer: 결제 페이지 이동

    Customer->>Web: 배송지 정보 입력
    Note over Customer,Web: 주소, 연락처, 배송 메모 입력

    Customer->>Web: "보유 쿠폰 확인" 클릭
    Web->>OrderAPI: GET /coupons
    OrderAPI->>CouponService: 사용 가능한 쿠폰 목록 조회
    CouponService->>DB: 쿠폰 목록 조회
    Note over CouponService: 유효기간, 사용여부, 최소금액 확인
    DB-->>CouponService: 쿠폰 목록
    CouponService-->>OrderAPI: 쿠폰 목록 반환
    OrderAPI-->>Web: 200 OK (쿠폰 목록)
    Web-->>Customer: 사용 가능한 쿠폰 표시

    Customer->>Web: 쿠폰 선택 (10% 할인)
    Web->>OrderAPI: POST /orders/calculate
    Note over OrderAPI: {items, couponId}
    OrderAPI->>OrderService: 결제 금액 계산 요청
    OrderService->>CouponService: 쿠폰 정보 및 적용 대상 조회
    CouponService->>DB: 쿠폰 조회 (적용 상품, 할인율)
    DB-->>CouponService: 쿠폰 정보
    CouponService-->>OrderService: 쿠폰 정보 반환
    OrderService->>OrderService: 할인 계산
    Note over OrderService: 원가 21,000원 → 할인 2,100원 → 최종 18,900원
    OrderService-->>OrderAPI: 계산 결과
    OrderAPI-->>Web: 200 OK (최종 금액: 18,900원)
    Web-->>Customer: 최종 결제 금액 표시

    Customer->>Web: "결제하기" 버튼 클릭
    Web->>OrderAPI: POST /orders/checkout
    Note over OrderAPI: {items, shippingAddress, couponId}
    OrderAPI->>OrderService: 주문 생성 및 결제 요청

    OrderService->>DB: BEGIN TRANSACTION

    OrderService->>InventoryService: 재고 차감 요청
    InventoryService->>DB: SELECT FOR UPDATE + 재고 차감

    alt 재고 충분
        DB-->>InventoryService: 재고 차감 완료
        InventoryService-->>OrderService: 재고 확보 완료

        OrderService->>DB: 주문 생성 (ORDER, ORDER_ITEM)
        DB-->>OrderService: 주문 생성 완료

        OrderService->>CouponService: 쿠폰 사용 처리
        CouponService->>DB: 쿠폰 상태 변경 (USER_COUPON)
        DB-->>CouponService: 쿠폰 사용 완료
        CouponService-->>OrderService: 쿠폰 적용 완료

        OrderService->>PaymentService: 잔액 결제 요청
        PaymentService->>DB: 잔액 차감 (BALANCE)

        alt 잔액 충분
            DB-->>PaymentService: 잔액 차감 완료
            PaymentService-->>OrderService: 결제 성공

            OrderService->>DB: 주문 상태 변경 (PAYMENT_COMPLETED)
            DB-->>OrderService: 상태 변경 완료

            OrderService->>DB: COMMIT
            OrderService-->>OrderAPI: 주문 완료
            OrderAPI-->>Web: 201 Created (주문 정보)
            Web-->>Customer: "주문이 완료되었습니다"

            Note over OrderService: 외부 데이터 플랫폼 전송은<br/>별도 비동기 프로세스로 처리

        else 잔액 부족
            DB-->>PaymentService: 잔액 부족
            PaymentService-->>OrderService: 결제 실패
            OrderService->>DB: ROLLBACK
            OrderService-->>OrderAPI: 결제 실패
            OrderAPI-->>Web: 402 Payment Required
            Web-->>Customer: "잔액이 부족합니다"
        end

    else 재고 부족
        DB-->>InventoryService: 재고 부족
        InventoryService-->>OrderService: 재고 부족
        OrderService->>DB: ROLLBACK
        OrderService-->>OrderAPI: 재고 부족
        OrderAPI-->>Web: 409 Conflict
        Web-->>Customer: "죄송합니다. 재고가 부족합니다"
    end
```

---

## 5. 관리자 재고 관리

관리자가 재고 현황을 파악하고, 부족한 재고를 추가 입고하는 기능

```mermaid
sequenceDiagram
    actor Admin as 관리자 (박사장)
    participant AdminWeb as 관리자 웹
    participant ProductAPI as 상품 API
    participant InventoryService as 재고 서비스
    participant DB as Database

    Admin->>AdminWeb: 재고 현황 메뉴 클릭
    AdminWeb->>ProductAPI: GET /admin/inventory
    ProductAPI->>InventoryService: 전체 재고 현황 조회
    InventoryService->>DB: 옵션별 재고 수량 조회
    DB-->>InventoryService: 재고 목록
    InventoryService-->>ProductAPI: 재고 현황 반환
    ProductAPI-->>AdminWeb: 200 OK (재고 목록)
    AdminWeb-->>Admin: 재고 현황 표시
    Note over Admin: '에티오피아 예가체프 G1 - 핸드드립/200g'<br/>재고가 적음을 확인

    Admin->>AdminWeb: 재고 추가 입고 (수량: 50)
    AdminWeb->>ProductAPI: POST /admin/inventory/{optionId}/stock
    Note over ProductAPI: {quantity: 50}
    ProductAPI->>InventoryService: 재고 추가 요청
    InventoryService->>DB: 재고 추가 (quantity + 50)
    DB-->>InventoryService: 재고 업데이트 완료
    InventoryService-->>ProductAPI: 재고 추가 완료
    ProductAPI-->>AdminWeb: 200 OK
    AdminWeb-->>Admin: "재고가 추가되었습니다 (50개)"
```

---

## 6. 주문 확인 및 상품 준비

관리자가 신규 주문을 확인하고 상품을 준비하여 상태를 변경하는 기능

```mermaid
sequenceDiagram
    actor Admin as 관리자 (박사장)
    participant AdminWeb as 관리자 웹
    participant OrderAPI as 주문 API
    participant OrderService as 주문 서비스
    participant DB as Database

    Admin->>AdminWeb: 관리자 페이지 접속
    AdminWeb->>OrderAPI: GET /admin/orders?status=PAYMENT_COMPLETED
    OrderAPI->>OrderService: 결제 완료 주문 조회
    OrderService->>DB: 상태별 주문 목록 조회
    DB-->>OrderService: 주문 목록
    OrderService-->>OrderAPI: 주문 목록 반환
    OrderAPI-->>AdminWeb: 200 OK (주문 목록)
    AdminWeb-->>Admin: 신규 주문 목록 표시

    Admin->>AdminWeb: 주문 상세 조회 (김지수 주문)
    AdminWeb->>OrderAPI: GET /admin/orders/{orderId}
    OrderAPI->>OrderService: 주문 상세 조회
    OrderService->>DB: 주문 상세 정보 조회
    DB-->>OrderService: 주문 정보
    Note over OrderService: 에티오피아 예가체프 G1<br/>200g, 핸드드립 분쇄
    OrderService-->>OrderAPI: 주문 상세 반환
    OrderAPI-->>AdminWeb: 200 OK (주문 상세)
    AdminWeb-->>Admin: 주문 상세 정보 표시

    Admin->>AdminWeb: '상품 준비중' 버튼 클릭
    AdminWeb->>OrderAPI: PATCH /admin/orders/{orderId}
    Note over OrderAPI: {status: 'PREPARING'}
    OrderAPI->>OrderService: 주문 상태 변경 요청
    OrderService->>DB: 주문 상태 변경 (status → PREPARING)
    DB-->>OrderService: 상태 변경 완료
    OrderService-->>OrderAPI: 상태 변경 완료
    OrderAPI-->>AdminWeb: 200 OK
    AdminWeb-->>Admin: "상태가 변경되었습니다"

    Note over Admin: 원두 포장 및 배송 상자 준비
```

---

## 7. 상품 배송 시작

관리자가 배송을 시작하고 운송장 번호를 등록하는 기능

```mermaid
sequenceDiagram
    actor Admin as 관리자 (박사장)
    participant AdminWeb as 관리자 웹
    participant OrderAPI as 주문 API
    participant OrderService as 주문 서비스
    participant ShippingService as 배송 서비스
    participant NotificationService as 알림 서비스
    participant DB as Database

    Note over Admin: 배송업체 상품 수거

    Admin->>AdminWeb: 주문 상세 페이지에서 '배송 시작' 클릭
    AdminWeb->>OrderAPI: PATCH /admin/orders/{orderId}
    Note over OrderAPI: {trackingNumber: '1234567890'}
    OrderAPI->>OrderService: 배송 시작 요청

    OrderService->>ShippingService: 운송장 번호 등록
    ShippingService->>DB: 운송장 번호 등록 (trackingNumber → 1234567890)
    DB-->>ShippingService: 운송장 등록 완료
    ShippingService-->>OrderService: 등록 완료

    OrderService->>DB: 주문 상태 변경 (status → SHIPPING)
    DB-->>OrderService: 상태 변경 완료

    OrderService-->>OrderAPI: 배송 시작 완료
    OrderAPI-->>AdminWeb: 200 OK
    AdminWeb-->>Admin: "배송이 시작되었습니다"
```

---

## 8. 배송 조회 및 완료 처리

고객이 배송 현황을 조회하고, 배송이 완료되면 시스템이 자동으로 상태를 변경하는 기능

```mermaid
sequenceDiagram
    actor Customer as 고객 (김지수)
    participant Web as Web/App
    participant OrderAPI as 주문 API
    participant OrderService as 주문 서비스
    participant ShippingService as 배송 서비스
    participant ExternalAPI as 배송업체 API
    participant NotificationService as 알림 서비스
    participant DB as Database

    Customer->>Web: 주문 내역 조회
    Web->>OrderAPI: GET /orders/{orderId}
    OrderAPI->>OrderService: 주문 상세 조회
    OrderService->>DB: 주문 정보 조회
    DB-->>OrderService: 주문 정보 (운송장 포함)
    OrderService-->>OrderAPI: 주문 상세 반환
    OrderAPI-->>Web: 200 OK (주문 정보)
    Web-->>Customer: 주문 상세 표시 (운송장: 1234567890)

    Customer->>Web: 운송장 번호 클릭
    Web->>OrderAPI: GET /orders/{orderId}/tracking
    OrderAPI->>ShippingService: 배송 추적 요청
    ShippingService->>ExternalAPI: 배송업체 배송 조회 API 호출
    ExternalAPI-->>ShippingService: 배송 현황 (배송 중)
    ShippingService-->>OrderAPI: 배송 현황 반환
    OrderAPI-->>Web: 200 OK (배송 현황)
    Web-->>Customer: 배송 현황 표시

    Note over Customer: 다음 날, 집 앞에서 상품 수령

    Note over ExternalAPI: 배송 완료 처리 후<br/>Bean Bliss 시스템에 웹훅 호출

    ExternalAPI->>OrderAPI: POST /webhooks/shipping/completed
    Note over OrderAPI: {trackingNumber}
    OrderAPI->>OrderService: 배송 완료 웹훅 처리
    OrderService->>DB: 운송장 번호 기반 주문 정보 조회 (1234567890)
    DB-->>OrderService: 주문 정보
    OrderService->>DB: 주문 상태 변경 (status → DELIVERED)
    DB-->>OrderService: 상태 변경 완료

    OrderService-->>OrderAPI: 처리 완료
    OrderAPI-->>ExternalAPI: 200 OK
```

---

## 9. 잔액 조회 및 충전

고객이 자신의 잔액을 조회하고, 충전할 수 있는 기능

```mermaid
sequenceDiagram
    actor Customer as 고객
    participant Web as Web/App
    participant BalanceAPI as 잔액 API
    participant BalanceService as 잔액 서비스
    participant DB as Database

    Customer->>Web: 마이페이지 접속
    Web->>BalanceAPI: GET /balance
    BalanceAPI->>BalanceService: 잔액 조회 요청
    BalanceService->>DB: 사용자 잔액 조회
    DB-->>BalanceService: 현재 잔액 (50,000원)
    BalanceService-->>BalanceAPI: 잔액 정보 반환
    BalanceAPI-->>Web: 200 OK (잔액: 50,000원)
    Web-->>Customer: 잔액 표시

    Customer->>Web: "잔액 충전" 버튼 클릭
    Web-->>Customer: 충전 금액 입력 화면 표시

    Customer->>Web: 충전 금액 입력 (30,000원)
    Web->>BalanceAPI: POST /balance/charge
    Note over BalanceAPI: {amount: 30000}
    BalanceAPI->>BalanceService: 잔액 충전 요청

    BalanceService->>DB: 잔액 증가
    DB-->>BalanceService: 잔액 업데이트 완료

    BalanceService-->>BalanceAPI: 충전 완료 (잔액: 80,000원)
    BalanceAPI-->>Web: 200 OK (충전 후 잔액: 80,000원)
    Web-->>Customer: "충전이 완료되었습니다 (80,000원)"
```
