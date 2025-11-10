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

