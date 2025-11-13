# ☕ 커피 원두 이커머스 플랫폼

## 프로젝트 소개

이 프로젝트는 커피 원두 전문 온라인 쇼핑몰 시나리오에 기반한 더미 프로젝트입니다.
고객이 다양한 커피 원두를 탐색하고 구매할 수 있는 이커머스 플랫폼으로,
특히 **재고 관리**와 **선착순 쿠폰 발급**의 동시성 제어에 중점을 두고 설계되었습니다.

## 핵심 기능

### 1. 상품 카탈로그
- 인기 상품 조회 (최근 N일 기준 Top K)
- 상품 목록 조회 (페이지네이션, 필터링, 정렬)
- 상품 상세 정보 및 옵션별 가격/재고 조회

### 2. 쿠폰 시스템
- 발급 가능한 쿠폰 목록 조회
- **선착순 쿠폰 발급** (동시성 제어)
    - 정확한 수량 제어
    - 1인 1매 중복 방지
    - 유효성 검증 (유효기간, 최소 주문금액)
- 내가 발급받은 쿠폰 목록 조회

### 3. 재고 관리
- 실시간 재고 조회
- **하이브리드 락 기반 재고 관리**
    - Phase 1: 주문창 진입 시 재고 예약 (10분 타임아웃)
    - Phase 2: 결제 시 실제 재고 차감
- 결제 실패 시 자동 복구

### 4. 주문/결제
- 장바구니 관리
- 주문 생성 및 결제
- 쿠폰 할인 적용
- 잔액 기반 결제
- 트랜잭션 원자성 보장 (재고 차감 → 주문 생성 → 쿠폰 사용 → 잔액 차감)

## 기술 스택

- **Language**: Java 17+ / Kotlin
- **Framework**: Spring Boot 3.x

## 프로젝트 구조

이 프로젝트는 **4계층 아키텍처**를 기반으로 구성되어 있으며, 각 계층의 책임을 명확히 분리합니다.

### 아키텍처 계층 구조

```
┌──────────────────────────────────────────────────────────┐
│ Presentation Layer                                       │
│ HTTP 요청/응답, 유효성 검사, DTO 변환                        │
│ → controller/, dto/                                      │
└──────────────────────────────────────────────────────────┘
                         ↓
┌──────────────────────────────────────────────────────────┐
│ Application Layer                                        │
│ 여러 Service 조합, 복합 트랜잭션 처리                         │
│ → usecase/                                               │
└──────────────────────────────────────────────────────────┘
                         ↓
┌──────────────────────────────────────────────────────────┐
│ Domain Layer                                             │
│ 비즈니스 로직, 도메인 규칙, 영속성 계약 정의 (DIP)              │
│ → service/, domain/, repository/ (interface), exception/ │
└──────────────────────────────────────────────────────────┘
                         ↓
┌──────────────────────────────────────────────────────────┐
│ Infrastructure Layer                                     │
│ DB 접근, 외부 시스템 연동, 기술 종속적 구현                    │
│ → repository/ (impl), entity/                            │
└──────────────────────────────────────────────────────────┘

의존성: Presentation → Application → Domain ← Infrastructure
```

### 디렉토리 구조

```
src/
├── main/kotlin/com/beanbliss/
│   ├── domain/                           # 도메인별 패키지
│   │   ├── product/                      # 상품 도메인
│   │   │   ├── controller/               # [Presentation] REST API 엔드포인트
│   │   │   ├── dto/                      # [Presentation] Request/Response DTO
│   │   │   ├── usecase/                  # [Application] 유스케이스 조율
│   │   │   ├── service/                  # [Domain] 비즈니스 로직
│   │   │   ├── domain/                   # [Domain] 도메인 모델
│   │   │   ├── exception/                # [Domain] 도메인별 예외
│   │   │   ├── repository/               # [Domain] Repository 인터페이스
│   │   │   │                             # [Infrastructure] Repository 구현체
│   │   │   └── entity/                   # [Infrastructure] JPA Entity
│   │   │
│   │   ├── coupon/                       # 쿠폰 도메인
│   │   ├── order/                        # 주문 도메인
│   │   ├── inventory/                    # 재고 도메인
│   │   ├── cart/                         # 장바구니 도메인
│   │   └── user/                         # 사용자 도메인
│   │
│   └── common/                           # 공통 모듈
│       ├── dto/                          # 공통 DTO
│       ├── pagination/                   # 페이지네이션 유틸
│       └── exception/                    # 공통 예외 처리
│
└── test/kotlin/com/beanbliss/            # 테스트 코드
    └── domain/
        └── product/
            ├── controller/               # Presentation Layer 테스트
            ├── usecase/                  # Application Layer 테스트
            └── service/                  # Domain Layer 테스트
```

## 시작하기

### 개발 환경 설정

#### 1. 사전 요구사항
- Java 17 이상
- MySQL 8.0 이상
- Gradle 8.x

#### 2. 데이터베이스 설정

```bash
# MySQL에서 commerce 데이터베이스 생성
mysql -u root -p -e "CREATE DATABASE commerce CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"
```

#### 3. 애플리케이션 실행

##### 프로덕션 환경 (기본)
```bash
./gradlew bootRun
```

##### 개발 환경 (dev 프로파일 - 더미 데이터 자동 생성)
```bash
./gradlew bootRun --args="--spring.profiles.active=dev"
```

#### 4. API 문서 (Swagger)

애플리케이션 실행 후 다음 URL로 Swagger UI에 접근할 수 있습니다:

```
http://localhost:8080/swagger-ui/index.html
```

---

## 더미 데이터 초기화

### 개요

dev 프로파일로 애플리케이션을 실행하면 **자동으로 더미 데이터가 생성**됩니다.

이는 `ApplicationReadyEvent`를 기반으로 한 `DataInitializer` 클래스에 기반합니다.

### 생성되는 데이터

각 엔티티별로 **100건씩** 생성됩니다:

| 엔티티 | 수량 | 설명 |
|--------|------|------|
| **User** | 100명 | `user1@beanbliss.com` ~ `user100@beanbliss.com` |
| **Balance** | 100개 | 각 사용자마다 초기 잔액 100,000원 |
| **Product** | 100개 | 다양한 커피 브랜드 상품 |
| **ProductOption** | 300개 | 상품당 3개 옵션 (250g, 500g, 1kg) |
| **Inventory** | 300개 | 옵션당 1,000개 초기 재고 |
| **Coupon** | 100개 | 정률/정액 할인 쿠폰 (혼합) |
| **CouponTicket** | 10,000개 | 쿠폰당 100개 선착순 티켓 |
| **CartItem** | 100개 | 사용자별 장바구니 상품 |
| **Order** | 100개 | 다양한 주문 상태 (결제완료, 배송준비, 배송중, 완료) |
| **OrderItem** | 100~300개 | 주문당 1~3개 상품 |
| **InventoryReservation** | 50개 | 주문에 대한 재고 예약 |

### 실행 방법

#### dev 프로파일로 애플리케이션 시작

```bash
./gradlew bootRun --args="--spring.profiles.active=dev"
```

#### 더미 데이터 리셋 (초기화)

```bash
# MySQL에서 데이터베이스 삭제 및 재생성
mysql -u root -p -e "DROP DATABASE commerce; CREATE DATABASE commerce CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"
```
