# 🚀 CLAUDE_Layered.md: Kotlin Spring Boot E-commerce API 개발 가이드

이 문서는 전통적인 **레이어드 아키텍처**를 기반으로 E-commerce API를 개발하는 가이드입니다.
각 계층의 책임을 명확히 분리하고 **DIP (의존성 역전 원칙)** 를 준수하여 견고한 시스템을 구축하는 데 집중합니다.

## 0. 🎯 핵심 개발 원칙 및 목표

1. **4계층 아키텍처 준수**: 계층 구조는 **Presentation Layer → Application Layer → Domain Layer ← Infrastructure Layer** 순서이며, 상위 계층은 하위 계층에만 의존하고, 하위 계층은 상위 계층을 알지 말아야 합니다.
2. **DIP 적용**: **Repository 계층에만** Interface와 구현체를 분리하여 DIP를 구현합니다. Service, UseCase, Controller는 변경이 적으므로 Interface 없이 클래스만 사용합니다.
3. **책임 분리 원칙**:
   - **Controller**: HTTP 요청/응답, DTO 변환, 입력 유효성 검증
   - **UseCase**: 여러 Service의 조율 (Facade 패턴), 복합 트랜잭션 처리 (클래스)
   - **Service**: 비즈니스 로직, 도메인 규칙 검증, Repository 호출 (클래스)
   - **Repository**: 영속성 계약 정의 (Interface는 Domain Layer, 구현체는 Infrastructure Layer)
4. **TDD 접근**: 단위 테스트를 통해 클래스의 책임이 올바르게 분산되고, 로직이 올바르게 추상화되었는지 설계를 검증하는 목적에 집중합니다.
5. **패키지 경로**: `com.beanbliss.domain.{도메인명}`, `com.beanbliss.common` 을 사용합니다.
6. **테스트 파일 네이밍 규칙**: 기능별로 명확한 네이밍을 사용하여 테스트의 목적과 범위를 명시합니다.

## 1. 🍳 API 설계

- 지정한 기능을 구성하기 전 PRD와 ERD 문서를 활용하여 해당 기능을 구성하기 위해 필요한 API 설계 문서를 작성합니다.
- **문서 위치**: `docs/api/{도메인}/{기능명}.md` 형식으로 작성합니다.
  - 예: `docs/api/product/get-products.md`, `docs/api/order/create-order.md`
  - 도메인별로 디렉토리를 분리하고, 각 API마다 별도의 파일로 작성합니다.

### API 설계 문서 구조

1. **개요**: 목적, 사용 시나리오, PRD 참고, 연관 테이블 명시
2. **API 명세**: Endpoint, Request/Response, HTTP Status Codes, Error Codes
3. **비즈니스 로직**: 핵심 비즈니스 규칙, 유효성 검사, 계산 로직, 정렬/필터링 조건
4. **구현 시 고려사항**: 성능 최적화, 동시성 제어, 데이터 일관성
5. **레이어드 아키텍처 흐름**: Mermaid 시퀀스 다이어그램, 트랜잭션 범위와 격리 수준, 예외 처리 흐름

## 2. 📂 프로젝트 구조 (패키지 가이드)

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
│ 여러 Service 조합, 복합 트랜잭션 처리 (Facade)               │
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
│   │   │   ├── usecase/                  # [Application] 유스케이스 조율 (Facade)
│   │   │   ├── service/                  # [Domain] 비즈니스 로직
│   │   │   ├── domain/                   # [Domain] 도메인 모델
│   │   │   ├── exception/                # [Domain] 도메인별 예외
│   │   │   ├── repository/               # [Domain] Repository Interface
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
│       ├── dto/                          # 공통 DTO (PageableResponse 등)
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

## 3. 📦 도메인 모델 (Domain Layer)

**책임**: 비즈니스 규칙과 상태를 캡슐화하는 핵심 객체
- `domain` 패키지에 위치하며, **순수한 비즈니스 로직**만 포함
- 도메인별 비즈니스 규칙 수행 (예: `hasStock()`, `reduceStock()`, `reserveStock()`)
- SOLID - SRP: 도메인 로직에 대한 책임만 가짐

## 4. 🗄️ 영속성 계층 (Infrastructure Layer): Repository

### 계층 분리 원칙

- **Repository Interface**: `repository` 패키지 (Domain Layer) - Service가 의존하는 계약
- **Repository 구현체**: `repository` 패키지 (Infrastructure Layer) - 실제 DB 접근 로직
- **JPA Entity**: `entity` 패키지 (Infrastructure Layer) - DB 테이블과 매핑

Service는 **Repository Interface**에만 의존하여 DIP를 구현하고, 구현체는 Entity와 Domain Model 간 변환을 담당합니다.

### 핵심 패턴

- Interface는 계약만 정의 (`findById`, `findAll`, `save` 등)
- 구현체는 JPA Repository를 활용하여 실제 DB 접근
- Entity는 `toDomain()`, `fromDomain()` 메서드로 Domain Model과 변환

## 5. ⚙️ 비즈니스 계층 (Domain Layer): Service

Service는 **핵심 비즈니스 로직과 도메인 규칙 검증**을 담당합니다.
**Interface 없이 클래스로만 구현**하여 불필요한 추상화를 제거합니다.

### Service 책임

- ✅ 비즈니스 로직 수행
- ✅ 도메인 규칙 검증 (예: 재고 부족, 쿠폰 사용 가능 여부)
- ✅ Repository Interface를 통한 데이터 조회/저장
- ✅ 도메인 모델에 비즈니스 로직 위임
- ❌ HTTP 요청/응답 처리 (Controller의 책임)
- ❌ 여러 도메인 Service 조율 (UseCase의 책임)

### 구현 패턴

- `@Service` 애노테이션 사용
- `execute()` 메서드로 비즈니스 로직 수행
- Repository Interface에만 의존 (DIP 준수)
- `@Transactional` 애노테이션으로 트랜잭션 관리

## 5-1. 🎯 애플리케이션 계층 (Application Layer): UseCase

UseCase는 **여러 Service를 조율하는 Facade 패턴**을 적용하여, 복합적인 비즈니스 흐름을 완성합니다.

### UseCase 책임

- ✅ 여러 도메인 Service의 조율 (Orchestration)
- ✅ 복합 트랜잭션 경계 설정
- ✅ Service 호출 순서 제어
- ❌ 비즈니스 로직 구현 (Service의 책임)
- ❌ 도메인 규칙 검증 (Service의 책임)
- ❌ 입력 유효성 검증 (Controller의 책임)

### 구현 패턴

- `@Component` 애노테이션 사용
- 여러 Service를 주입받아 순서대로 호출
- `@Transactional` 애노테이션으로 복합 트랜잭션 관리
- Service에 비즈니스 로직 위임, UseCase는 조율만 담당

## 6. 🌐 프레젠테이션 계층 (Presentation Layer): Controller

Controller는 **HTTP 요청/응답 처리, 입력 유효성 검증, DTO 변환**을 담당합니다.

### Controller 책임

- ✅ HTTP 요청/응답 처리
- ✅ 입력 유효성 검증 (`@Valid`, `@RequestParam` validation)
- ✅ DTO ↔ Domain Model 변환 (필요시)
- ✅ Service 또는 UseCase에 비즈니스 로직 위임
- ❌ 비즈니스 로직 수행 (Service의 책임)
- ❌ Repository 직접 호출 (Service의 책임)

### 구현 패턴

- `@RestController` 애노테이션 사용
- 단일 도메인 조회는 Service 직접 호출
- 복합 도메인 조율은 UseCase 호출
- DTO는 `companion object`의 `from()` 메서드로 변환

## 7. ⚠️ 공통 예외 처리 (Common Layer)

### 예외 분류 원칙

| 분류 | 위치 | 예시 |
|------|------|------|
| **공통 예외** | `com.beanbliss.common.exception` | `ResourceNotFoundException`, `InvalidPageNumberException`, `InvalidPageSizeException` |
| **도메인 예외** | `com.beanbliss.domain.{도메인}.exception` | `InsufficientStockException` (inventory), `CouponExpiredException` (coupon) |

### ExceptionHandler 우선순위

- `@Order(10)`: 도메인별 ExceptionHandler (높은 우선순위)
- `@Order(100)`: 공통 ExceptionHandler (낮은 우선순위 - Fallback)
- 처리 순서: 도메인 핸들러 → 공통 핸들러

### 베스트 프랙티스

1. 공통 예외는 common 패키지, 도메인 예외는 도메인 패키지에 정의
2. `@Order`로 우선순위 관리
3. 일관된 에러 응답: `ErrorResponse(status, code, message)` 사용

## 8. 📄 페이지네이션 공통 처리 (Common Layer)

### 핵심 원칙

1. **PageCalculator 필수 사용**: 직접 계산 금지
2. **공통 예외 사용**: `InvalidPageNumberException`, `InvalidPageSizeException`
3. **Controller 기본값**: `page = 1`, `size = 10`
4. **응답 통일**: 모든 페이지네이션 API는 `PageableResponse` 사용

### Service 구현 패턴

1. 유효성 검증 (page ≥ 1, size ∈ [1, 100])
2. 데이터 조회 (Repository 호출)
3. 페이지 계산 (`PageCalculator.calculateTotalPages()`)
4. 응답 조립 (`PageableResponse` 포함)

## 9. 📝 파일 네이밍 가이드

### 기본 네이밍 규칙

**패턴**: `[Feature][Domain][Layer].kt`

- **Feature**: 기능 동작 (Get, Create, Update, Delete, Reduce, Reserve)
- **Domain**: 도메인 이름 (Products, Product, Order, Inventory, Stock)
- **Layer**: 계층 이름 (Controller, Service, UseCase, Repository)

### DIP 적용 규칙 (Repository만 적용)

- **Repository Interface**: `[Domain]Repository.kt`
- **Repository 구현체**: `[Domain]RepositoryImpl.kt`
- **Service, UseCase, Controller**: 클래스만 사용 (Interface 없음)

### 계층별 네이밍

| 계층 | 패턴 | 예시 |
|------|------|------|
| **Controller** | `[Domain]Controller.kt` | `ProductController.kt`, `OrderController.kt` |
| **UseCase** | `[Feature][Domain]UseCase.kt` | `CreateOrderUseCase.kt`, `IssueCouponUseCase.kt` |
| **Service** | `[Feature][Domain]Service.kt` | `GetProductsService.kt`, `ReduceStockService.kt` |
| **Repository Interface** | `[Domain]Repository.kt` | `ProductRepository.kt`, `InventoryRepository.kt` |
| **Repository 구현체** | `[Domain]RepositoryImpl.kt` | `ProductRepositoryImpl.kt` |
| **Domain Model** | `[Domain].kt` | `Product.kt`, `Inventory.kt`, `Order.kt` |
| **Entity** | `[Domain]Entity.kt` | `ProductEntity.kt`, `InventoryEntity.kt` |
| **DTO** | `[Feature][Domain]Request/Response.kt` | `CreateOrderRequest.kt`, `ProductResponse.kt` |

### 테스트 파일 네이밍

**패턴**: `[테스트대상파일명]Test.kt`

#### 계층별 테스트 전략

**Controller 테스트**
- `@WebMvcTest` 사용
- Service/UseCase Mocking
- HTTP 계층만 테스트
- 입력 유효성 검증, 상태 코드 검증

**UseCase 테스트**
- 여러 Service Mocking
- Service 조율 로직 테스트 (호출 순서, 횟수)
- 트랜잭션 경계 테스트

**Service 테스트**
- Repository Mocking
- 비즈니스 로직과 도메인 규칙 검증
- Repository 호출 검증 (`verify`)

**Repository 테스트**
- `@DataJpaTest` + TestContainers 사용
- 실제 프로덕션 DB 환경에서 테스트
- 쿼리 동작 검증
- Entity ↔ Domain Model 변환 검증

### 네이밍 규칙 요약 테이블

| 계층 | 파일명 패턴 | DIP 적용 | 예시 |
|------|-----------|---------|------|
| **Controller** | `[Domain]Controller.kt` | ❌ (클래스) | `ProductController.kt`, `OrderController.kt` |
| **UseCase** | `[Feature][Domain]UseCase.kt` | ❌ (클래스) | `CreateOrderUseCase.kt`, `IssueCouponUseCase.kt` |
| **Service** | `[Feature][Domain]Service.kt` | ❌ (클래스) | `GetProductsService.kt`, `ReduceStockService.kt` |
| **Repository Interface** | `[Domain]Repository.kt` | ✅ (Interface) | `ProductRepository.kt`, `InventoryRepository.kt` |
| **Repository 구현체** | `[Domain]RepositoryImpl.kt` | ✅ (구현체) | `ProductRepositoryImpl.kt`, `InventoryRepositoryImpl.kt` |
| **Domain Model** | `[Domain].kt` | - | `Product.kt`, `Inventory.kt`, `Order.kt` |
| **Entity** | `[Domain]Entity.kt` | - | `ProductEntity.kt`, `InventoryEntity.kt` |
| **DTO** | `[Feature][Domain]Request/Response.kt` | - | `CreateOrderRequest.kt`, `ProductResponse.kt` |
| **Test** | `[테스트대상파일명]Test.kt` | - | `GetProductsServiceTest.kt`, `CreateOrderUseCaseTest.kt` |
| **Test Base** | `[용도]TestBase.kt` | - | `RepositoryTestBase.kt` (TestContainers 공통 설정) |

### 핵심 원칙

- **Repository만 DIP 적용**: Interface + 구현체 분리
- **Service, UseCase, Controller**: 클래스만 사용 (Interface 없음)
- **Service는 Repository Interface에만 의존**하여 DIP 준수
