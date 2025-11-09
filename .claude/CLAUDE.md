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
4. **TDD 접근**: 단위 테스트를 통해 비즈니스 로직과 책임 분산을 검증합니다. 기능 로직이 올바르게 구현되었는지를 검증하는 것보다 클래스의 책임이 올바르게 분산되고, 로직이 올바르게 추상화되었는지 설계를 검증하는 목적에 집중합니다.
5. **패키지 경로**: `com.beanbliss.domain.{도메인명}`, `com.beanbliss.common` 을 사용합니다. 자세한 내용은 프로젝트 구조 내용을 참고하세요.
6. **테스트 파일 네이밍 규칙**: 기능별로 명확한 네이밍을 사용하여 테스트의 목적과 범위를 명시합니다. 자세한 내용은 "테스트 네이밍 가이드" 섹션을 참고하세요.

## 1. 🍳 API 설계
- 지정한 기능을 구성하기 전 PRD와 ERD 문서를 활용하여 해당 기능을 구성하기 위해 필요한 API 설계 문서를 작성합니다.
- **문서 위치**: `docs/api/{도메인}/{기능명}.md` 형식으로 작성합니다.
  - 예: `docs/api/product/get-products.md`, `docs/api/order/create-order.md`
  - 도메인별로 디렉토리를 분리하고, 각 API마다 별도의 파일로 작성합니다.

### API 설계 문서 구조
각 API 설계 문서는 다음 순서로 작성합니다:

1. **개요**
   - 목적, 사용 시나리오, PRD 참고, 연관 테이블 명시

2. **API 명세**
   - Endpoint (HTTP Method, Path)
   - Request Parameters
   - Request Example
   - Response (Success) - JSON 예시
   - Response Schema - 타입과 주석 포함
   - HTTP Status Codes
   - Error Codes

3. **비즈니스 로직**
   - 핵심 비즈니스 규칙
   - 유효성 검사
   - 계산 로직
   - 정렬/필터링 조건

4. **구현 시 고려사항**
   - 성능 최적화
   - 동시성 제어
   - 데이터 일관성

5. **레이어드 아키텍처 흐름**
   - Mermaid 시퀀스 다이어그램으로 표현
   - JPA 기반 추상화된 쿼리나 메서드 호출로 작성
   - 트랜잭션 범위와 격리 수준 명시
   - 예외 처리 흐름 명시

- 설계 문서 내용은 한국어로 작성합니다.

## 2. 📂 프로젝트 구조 (패키지 가이드)

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

도메인 모델은 **비즈니스 규칙과 상태를 캡슐화**하는 핵심 객체입니다.
도메인 모델은 `domain` 패키지에 위치하며, **순수한 비즈니스 로직**만 포함합니다.

### **코드 3.1: 도메인 모델 예시 - Product.kt**

```kotlin
// com/beanbliss/domain/product/domain/Product.kt
package com.beanbliss.domain.product.domain

/**
 * [책임]: 상품의 상태 관리 및 비즈니스 규칙 수행
 * SOLID - SRP: 상품 도메인 로직에 대한 책임만 가짐
 */
data class Product(
    val id: Long,
    val name: String,
    val price: Int,
    var stock: Int,
    val category: String
) {
    /**
     * 재고 확인 비즈니스 로직
     */
    fun hasStock(quantity: Int): Boolean {
        return this.stock >= quantity
    }

    /**
     * 재고 감소 비즈니스 로직
     * 재고 부족 시 예외를 던지지 않고 false 반환 (도메인 규칙)
     */
    fun reduceStock(quantity: Int): Boolean {
        if (!hasStock(quantity)) {
            return false
        }
        this.stock -= quantity
        return true
    }
}
```

### **코드 3.2: 도메인 모델 예시 - Inventory.kt**

```kotlin
// com/beanbliss/domain/inventory/domain/Inventory.kt
package com.beanbliss.domain.inventory.domain

import com.beanbliss.domain.inventory.exception.InsufficientStockException

/**
 * [책임]: 재고 수량의 상태 관리 및 재고 부족 규칙 수행
 * SOLID - SRP: 재고 상태 변경에 대한 책임만 가짐
 */
data class Inventory(
    val productId: Long,
    var stock: Int,
    var reservedStock: Int = 0  // 예약된 재고
) {
    /**
     * 재고를 감소시키는 핵심 비즈니스 로직
     */
    fun reduceStock(quantity: Int) {
        if (this.stock < quantity) {
            throw InsufficientStockException(
                "상품 ID: $productId 의 재고가 부족합니다. " +
                "현재 재고: ${this.stock}, 요청 수량: $quantity"
            )
        }
        this.stock -= quantity
    }

    /**
     * 재고 예약 (주문창 진입 시)
     */
    fun reserveStock(quantity: Int): Boolean {
        val availableStock = stock - reservedStock
        if (availableStock < quantity) {
            return false
        }
        this.reservedStock += quantity
        return true
    }

    /**
     * 재고 예약 해제
     */
    fun releaseReservedStock(quantity: Int) {
        this.reservedStock = maxOf(0, this.reservedStock - quantity)
    }
}
```

## 4. 🗄️ 영속성 계층 (Infrastructure Layer): Repository

### **4.1 계층 분리 원칙**

- **Repository Interface**: `repository` 패키지 (Domain Layer) - Service가 의존하는 계약
- **Repository 구현체**: `repository` 패키지 (Infrastructure Layer) - 실제 DB 접근 로직
- **JPA Entity**: `entity` 패키지 (Infrastructure Layer) - DB 테이블과 매핑

Service는 **Repository Interface**에만 의존하여 DIP를 구현하고, 구현체는 Entity와 Domain Model 간 변환을 담당합니다.

### **코드 4.1: Repository Interface (Domain Layer - 계약 정의)**

```kotlin
// com/beanbliss/domain/product/repository/ProductRepository.kt
package com.beanbliss.domain.product.repository

import com.beanbliss.domain.product.domain.Product

/**
 * [책임]: 영속성 계층의 '계약' 정의
 * Service는 이 인터페이스에만 의존합니다. (DIP 준수)
 */
interface ProductRepository {
    fun findById(productId: Long): Product?
    fun findAll(category: String?, sort: String?): List<Product>
    fun findTopSelling(fromTimestamp: Long, limit: Int): List<Product>
    fun save(product: Product): Product
}
```

### **코드 4.2: JPA Entity (Infrastructure Layer - DB 매핑)**

```kotlin
// com/beanbliss/domain/product/entity/ProductEntity.kt
package com.beanbliss.domain.product.entity

import jakarta.persistence.*

/**
 * [책임]: DB 테이블과 매핑되는 JPA Entity
 * Infrastructure Layer에 속하며, 기술 종속적인 코드 포함
 */
@Entity
@Table(name = "products")
data class ProductEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(nullable = false)
    val name: String,

    @Column(nullable = false)
    val price: Int,

    @Column(nullable = false)
    var stock: Int,

    @Column(nullable = false)
    val category: String
) {
    /**
     * Entity → Domain Model 변환
     */
    fun toDomain(): com.beanbliss.domain.product.domain.Product {
        return com.beanbliss.domain.product.domain.Product(
            id = this.id,
            name = this.name,
            price = this.price,
            stock = this.stock,
            category = this.category
        )
    }

    companion object {
        /**
         * Domain Model → Entity 변환
         */
        fun fromDomain(product: com.beanbliss.domain.product.domain.Product): ProductEntity {
            return ProductEntity(
                id = product.id,
                name = product.name,
                price = product.price,
                stock = product.stock,
                category = product.category
            )
        }
    }
}
```

### **코드 4.3: Repository 구현체 (Infrastructure Layer - DB 접근)**

```kotlin
// com/beanbliss/domain/product/repository/ProductRepositoryImpl.kt
package com.beanbliss.domain.product.repository

import com.beanbliss.domain.product.domain.Product
import com.beanbliss.domain.product.entity.ProductEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

/**
 * [책임]: 실제 DB 접근 로직 구현
 * Infrastructure Layer에 속하며, JPA 기술에 종속적
 */
@Repository
class ProductRepositoryImpl(
    private val jpaRepository: ProductJpaRepository
) : ProductRepository {

    override fun findById(productId: Long): Product? {
        return jpaRepository.findById(productId)
            .map { it.toDomain() }
            .orElse(null)
    }

    override fun findAll(category: String?, sort: String?): List<Product> {
        // JPA 쿼리 로직 구현
        return jpaRepository.findAll()
            .map { it.toDomain() }
    }

    override fun findTopSelling(fromTimestamp: Long, limit: Int): List<Product> {
        // 복잡한 쿼리 로직 (QueryDSL 등 활용)
        return jpaRepository.findTopSelling(fromTimestamp, limit)
            .map { it.toDomain() }
    }

    override fun save(product: Product): Product {
        val entity = ProductEntity.fromDomain(product)
        val savedEntity = jpaRepository.save(entity)
        return savedEntity.toDomain()
    }
}

/**
 * Spring Data JPA Repository
 */
interface ProductJpaRepository : JpaRepository<ProductEntity, Long> {
    fun findTopSelling(fromTimestamp: Long, limit: Int): List<ProductEntity>
}
```

## 5. ⚙️ 비즈니스 계층 (Domain Layer): Service

Service는 **핵심 비즈니스 로직과 도메인 규칙 검증**을 담당합니다.
**Interface 없이 클래스로만 구현**하여 불필요한 추상화를 제거합니다. Repository는 DIP를 위해 Interface를 사용합니다.

### **5.1 Service 책임**

- ✅ 비즈니스 로직 수행
- ✅ 도메인 규칙 검증 (예: 재고 부족, 쿠폰 사용 가능 여부)
- ✅ Repository Interface를 통한 데이터 조회/저장
- ✅ 도메인 모델에 비즈니스 로직 위임
- ❌ HTTP 요청/응답 처리 (Controller의 책임)
- ❌ 여러 도메인 Service 조율 (UseCase의 책임)

### **코드 5.1: Service 클래스 (비즈니스 로직 포함)**

```kotlin
// com/beanbliss/domain/product/service/GetProductsService.kt
package com.beanbliss.domain.product.service

import com.beanbliss.domain.product.domain.Product
import com.beanbliss.domain.product.repository.ProductRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * [책임]: 상품 목록 조회 비즈니스 로직
 * Repository Interface에만 의존합니다. (DIP 준수)
 */
@Service
@Transactional(readOnly = true)
class GetProductsService(
    private val productRepository: ProductRepository
) {
    fun execute(category: String?, sort: String?): List<Product> {
        // 비즈니스 로직: 카테고리 및 정렬 조건으로 상품 조회
        return productRepository.findAll(category, sort)
    }
}
```

### **코드 5.2: 다른 Service 예시**

```kotlin
// com/beanbliss/domain/product/service/GetProductService.kt
@Service
@Transactional(readOnly = true)
class GetProductService(
    private val productRepository: ProductRepository
) {
    fun execute(productId: Long): Product {
        return productRepository.findById(productId)
            ?: throw ResourceNotFoundException("상품 ID: $productId 를 찾을 수 없습니다.")
    }
}

// com/beanbliss/domain/product/service/GetTopSellingProductsService.kt
@Service
@Transactional(readOnly = true)
class GetTopSellingProductsService(
    private val productRepository: ProductRepository
) {
    fun execute(days: Int, limit: Int): List<Product> {
        // 비즈니스 로직: 도메인 규칙 검증
        require(days > 0) { "조회 기간은 1일 이상이어야 합니다." }
        require(limit > 0) { "조회 개수는 1개 이상이어야 합니다." }

        // 비즈니스 로직: 조회 기간 계산
        val now = System.currentTimeMillis()
        val fromTimestamp = now - (days * 24L * 60 * 60 * 1000)

        return productRepository.findTopSelling(fromTimestamp, limit)
    }
}
```

### **코드 5.3: 도메인별 Service 예시 - Inventory Domain**

```kotlin
// com/beanbliss/domain/inventory/service/ReserveStockService.kt
package com.beanbliss.domain.inventory.service

import com.beanbliss.domain.inventory.repository.InventoryRepository
import com.beanbliss.common.exception.ResourceNotFoundException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class ReserveStockService(
    private val inventoryRepository: InventoryRepository
) {
    fun execute(productId: Long, quantity: Int): Boolean {
        // 비즈니스 로직: 재고 예약
        val inventory = inventoryRepository.findById(productId)
            ?: throw ResourceNotFoundException("상품 ID: $productId 의 재고 정보를 찾을 수 없습니다.")

        // 도메인 모델에 비즈니스 로직 위임
        val reserved = inventory.reserveStock(quantity)

        if (reserved) {
            inventoryRepository.save(inventory)
        }

        return reserved
    }
}

// com/beanbliss/domain/inventory/service/ReleaseStockService.kt
@Service
@Transactional
class ReleaseStockService(
    private val inventoryRepository: InventoryRepository
) {
    fun execute(productId: Long, quantity: Int) {
        val inventory = inventoryRepository.findById(productId)
            ?: throw ResourceNotFoundException("상품 ID: $productId 의 재고 정보를 찾을 수 없습니다.")

        inventory.releaseReservedStock(quantity)
        inventoryRepository.save(inventory)
    }
}

// com/beanbliss/domain/inventory/service/ReduceStockService.kt
@Service
@Transactional
class ReduceStockService(
    private val inventoryRepository: InventoryRepository
) {
    fun execute(productId: Long, quantity: Int) {
        // 비즈니스 로직: 재고 차감
        val inventory = inventoryRepository.findById(productId)
            ?: throw ResourceNotFoundException("상품 ID: $productId 의 재고 정보를 찾을 수 없습니다.")

        // 도메인 모델에 비즈니스 로직 위임 (재고 부족 시 예외 발생)
        inventory.reduceStock(quantity)

        inventoryRepository.save(inventory)
    }
}
```

## 5-1. 🎯 애플리케이션 계층 (Application Layer): UseCase

UseCase는 **여러 Service를 조율하는 Facade 패턴**을 적용하여, 복합적인 비즈니스 흐름을 완성합니다.

### **5-1.1 UseCase 책임**

- ✅ 여러 도메인 Service의 조율 (Orchestration)
- ✅ 복합 트랜잭션 경계 설정
- ✅ Service 호출 순서 제어
- ❌ 비즈니스 로직 구현 (Service의 책임)
- ❌ 도메인 규칙 검증 (Service의 책임)
- ❌ 입력 유효성 검증 (Controller의 책임)

### **코드 5-1.1: UseCase 예시 - 주문 생성**

```kotlin
// com/beanbliss/domain/order/usecase/CreateOrderUseCase.kt
package com.beanbliss.domain.order.usecase

import com.beanbliss.domain.order.service.CreateOrderService
import com.beanbliss.domain.inventory.service.ReduceStockService
import com.beanbliss.domain.user.service.DeductBalanceService
import com.beanbliss.domain.coupon.service.UseCouponService
import com.beanbliss.domain.order.dto.CreateOrderRequest
import com.beanbliss.domain.order.dto.CreateOrderResponse
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

/**
 * [책임]: 주문 생성 시 여러 Service를 조율하는 Facade
 * - 비즈니스 로직과 검증은 각 Service에 위임
 * - UseCase는 Service 호출 순서만 제어
 * - Service는 클래스이므로 직접 의존
 */
@Component
class CreateOrderUseCase(
    private val createOrderService: CreateOrderService,
    private val reduceStockService: ReduceStockService,
    private val deductBalanceService: DeductBalanceService,
    private val useCouponService: UseCouponService
) {

    @Transactional
    fun execute(userId: Long, request: CreateOrderRequest): CreateOrderResponse {
        // 1. 재고 차감 (ReduceStockService 클래스에 위임)
        reduceStockService.execute(request.productId, request.quantity)

        // 2. 쿠폰 사용 처리 (UseCouponService 클래스에 위임)
        request.couponId?.let { couponId ->
            useCouponService.execute(userId, couponId)
        }

        // 3. 주문 생성 (CreateOrderService 클래스에 위임)
        val order = createOrderService.execute(
            userId = userId,
            productId = request.productId,
            quantity = request.quantity,
            couponId = request.couponId
        )

        // 4. 사용자 잔액 차감 (DeductBalanceService 클래스에 위임)
        deductBalanceService.execute(userId, order.totalAmount)

        return CreateOrderResponse.from(order)
    }
}
```

### **코드 5-1.2: UseCase와 Service의 차이 예시**

```kotlin
// ❌ 잘못된 UseCase (비즈니스 로직과 검증 포함)
@Component
class WrongCreateOrderUseCase(
    private val productRepository: ProductRepository,
    private val orderRepository: OrderRepository
) {
    fun execute(productId: Long, quantity: Int) {
        // ❌ UseCase에서 직접 검증 로직 수행 (Service의 책임)
        if (quantity <= 0) {
            throw IllegalArgumentException("수량은 1개 이상이어야 합니다.")
        }

        // ❌ UseCase에서 직접 Repository 호출 (Service의 책임)
        val product = productRepository.findById(productId)
            ?: throw ResourceNotFoundException("상품을 찾을 수 없습니다.")

        // ❌ UseCase에서 비즈니스 로직 수행 (Service의 책임)
        if (!product.hasStock(quantity)) {
            throw InsufficientStockException("재고가 부족합니다.")
        }

        product.reduceStock(quantity)
        productRepository.save(product)
    }
}

// ✅ 올바른 UseCase (Service 조율만 수행)
@Component
class CreateOrderUseCase(
    private val reduceStockService: ReduceStockService,
    private val createOrderService: CreateOrderService
) {
    @Transactional
    fun execute(userId: Long, productId: Long, quantity: Int): OrderResponse {
        // ✅ Service에 비즈니스 로직 위임 (검증, Repository 호출 모두 Service에서 수행)
        reduceStockService.execute(productId, quantity)

        // ✅ 여러 Service를 조율만 수행
        val order = createOrderService.execute(userId, productId, quantity)

        return OrderResponse.from(order)
    }
}
```

### **코드 5.4: TDD 기반 책임 검증 (Service Test)**

Service 계층의 단위 테스트는 **Repository Interface**를 Mocking하여 비즈니스 로직의 올바른 수행과 책임 분산 여부를 검증합니다.

```kotlin
// src/test/kotlin/com/beanbliss/domain/inventory/service/ReduceStockServiceTest.kt
package com.beanbliss.domain.inventory.service

import com.beanbliss.domain.inventory.repository.InventoryRepository
import com.beanbliss.domain.inventory.domain.Inventory
import com.beanbliss.common.exception.ResourceNotFoundException
import io.mockk.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("재고 차감 Service 테스트")
class ReduceStockServiceTest {

    // Repository Interface를 Mockk으로 Mocking
    private val inventoryRepository: InventoryRepository = mockk()
    private lateinit var reduceStockService: ReduceStockService

    @BeforeEach
    fun setup() {
        // Service는 클래스이므로 직접 생성
        reduceStockService = ReduceStockService(inventoryRepository)
    }

    @Test
    fun `재고 감소 성공 시_Repository의 findById와 save가 호출되어야 한다`() {
        // Given
        val productId = 1L
        val mockInventory = Inventory(productId, 10)

        // Mocking 설정
        every { inventoryRepository.findById(productId) } returns mockInventory
        every { inventoryRepository.save(any()) } returns mockInventory

        // When
        reduceStockService.execute(productId, 3)

        // Then
        // [TDD 검증 목표 1]: Service는 Repository의 계약(Interface)을 올바르게 사용했는가?
        verify(exactly = 1) { inventoryRepository.findById(productId) }
        verify(exactly = 1) { inventoryRepository.save(any()) }

        // [TDD 검증 목표 2]: 비즈니스 로직 검증 - 도메인 모델의 상태가 올바르게 변경되었는가?
        assertEquals(7, mockInventory.stock)
    }

    @Test
    fun `존재하지 않는 상품 재고 감소 요청 시_ResourceNotFoundException이 발생해야 하며_save는 호출되지 않아야 한다`() {
        // Given
        val productId = 99L
        every { inventoryRepository.findById(productId) } returns null

        // When & Then
        assertThrows<ResourceNotFoundException> {
            reduceStockService.execute(productId, 1)
        }

        // [TDD 검증 목표 3]: SRP 준수 - 예외 발생 시, 불필요한 save 로직은 호출되지 않았는가?
        verify(exactly = 0) { inventoryRepository.save(any()) }
    }
}
```


## 6. 🌐 프레젠테이션 계층 (Presentation Layer): Controller

Controller는 **HTTP 요청/응답 처리, 입력 유효성 검증, DTO 변환**을 담당합니다.
비즈니스 로직은 Service나 UseCase에 위임합니다.

### **6.1 Controller 책임**

- ✅ HTTP 요청/응답 처리
- ✅ 입력 유효성 검증 (`@Valid`, `@RequestParam` validation)
- ✅ DTO ↔ Domain Model 변환 (필요시)
- ✅ Service 또는 UseCase에 비즈니스 로직 위임
- ❌ 비즈니스 로직 수행 (Service의 책임)
- ❌ Repository 직접 호출 (Service의 책임)

### **코드 6.1: DTO 정의 (dto 패키지)**

```kotlin
// com/beanbliss/domain/product/dto/ProductRequest.kt
package com.beanbliss.domain.product.dto

import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank

/**
 * 상품 목록 조회 Request DTO
 */
data class GetProductsRequest(
    val category: String? = null,
    val sort: String? = "created_at"
)

/**
 * 인기 상품 조회 Request DTO
 */
data class GetTopProductsRequest(
    @field:Min(1, message = "조회 기간은 1일 이상이어야 합니다.")
    val days: Int = 7,

    @field:Min(1, message = "조회 개수는 1개 이상이어야 합니다.")
    val limit: Int = 10
)

// com/beanbliss/domain/product/dto/ProductResponse.kt
package com.beanbliss.domain.product.dto

import com.beanbliss.domain.product.domain.Product

/**
 * 상품 Response DTO
 */
data class ProductResponse(
    val id: Long,
    val name: String,
    val price: Int,
    val stock: Int,
    val category: String
) {
    companion object {
        fun from(product: Product): ProductResponse {
            return ProductResponse(
                id = product.id,
                name = product.name,
                price = product.price,
                stock = product.stock,
                category = product.category
            )
        }
    }
}

data class ProductListResponse(
    val products: List<ProductResponse>,
    val total: Int
)
```

### **코드 6.2: Controller 구현 - Service 직접 호출**

```kotlin
// com/beanbliss/domain/product/controller/ProductController.kt
package com.beanbliss.domain.product.controller

import com.beanbliss.domain.product.service.GetProductsService
import com.beanbliss.domain.product.service.GetProductService
import com.beanbliss.domain.product.service.GetTopSellingProductsService
import com.beanbliss.domain.product.dto.*
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import jakarta.validation.Valid

/**
 * [책임]: HTTP 요청 처리 및 입력 유효성 검증 후 Service에 위임
 * - 단일 도메인 조회는 Service 직접 호출
 */
@RestController
@RequestMapping("/api/v1/products")
class ProductController(
    private val getProductsService: GetProductsService,
    private val getProductService: GetProductService,
    private val getTopSellingProductsService: GetTopSellingProductsService
) {

    /**
     * 상품 목록 조회
     */
    @GetMapping
    fun getProducts(
        @RequestParam(required = false) category: String?,
        @RequestParam(defaultValue = "created_at") sort: String
    ): ResponseEntity<ProductListResponse> {
        // Service에 비즈니스 로직 위임
        val products = getProductsService.execute(category, sort)

        val response = ProductListResponse(
            products = products.map { ProductResponse.from(it) },
            total = products.size
        )

        return ResponseEntity.ok(response)
    }

    /**
     * 인기 상품 조회
     */
    @GetMapping("/top")
    fun getTopProducts(
        @Valid @ModelAttribute request: GetTopProductsRequest
    ): ResponseEntity<List<ProductResponse>> {
        // 입력 유효성 검증은 @Valid로 자동 수행
        val products = getTopSellingProductsService.execute(request.days, request.limit)

        return ResponseEntity.ok(
            products.map { ProductResponse.from(it) }
        )
    }

    /**
     * 상품 상세 조회
     */
    @GetMapping("/{productId}")
    fun getProduct(@PathVariable productId: Long): ResponseEntity<ProductResponse> {
        val product = getProductService.execute(productId)
        return ResponseEntity.ok(ProductResponse.from(product))
    }
}
```

### **코드 6.3: Controller 구현 - UseCase 호출**

```kotlin
// com/beanbliss/domain/order/controller/OrderController.kt
package com.beanbliss.domain.order.controller

import com.beanbliss.domain.order.usecase.CreateOrderUseCase
import com.beanbliss.domain.order.dto.CreateOrderRequest
import com.beanbliss.domain.order.dto.CreateOrderResponse
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import jakarta.validation.Valid

/**
 * [책임]: HTTP 요청 처리 및 입력 유효성 검증 후 UseCase에 위임
 * - 복합 도메인 조율이 필요한 경우 UseCase 호출
 */
@RestController
@RequestMapping("/api/v1/orders")
class OrderController(
    private val createOrderUseCase: CreateOrderUseCase
) {

    /**
     * 주문 생성 (여러 Service 조율 필요 → UseCase 호출)
     */
    @PostMapping
    fun createOrder(
        @RequestHeader("X-User-Id") userId: Long,
        @Valid @RequestBody request: CreateOrderRequest
    ): ResponseEntity<CreateOrderResponse> {
        // UseCase에 복합 트랜잭션 위임
        val response = createOrderUseCase.execute(userId, request)

        return ResponseEntity.status(HttpStatus.CREATED).body(response)
    }
}
```

### **코드 6.4: Request DTO 유효성 검증 예시**

```kotlin
// com/beanbliss/domain/order/dto/CreateOrderRequest.kt
package com.beanbliss.domain.order.dto

import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotNull

data class CreateOrderRequest(
    @field:NotNull(message = "상품 ID는 필수입니다.")
    val productId: Long?,

    @field:Min(1, message = "수량은 1개 이상이어야 합니다.")
    val quantity: Int,

    val couponId: Long? = null
)
```

## 7. ⚠️ 공통 예외 처리 (Common Layer)

예외 처리를 공통 모듈과 도메인별로 분리하여 관심사를 명확히 분리합니다.

### **7.1 예외 분류 원칙**

| 분류 | 위치 | 예시 |
|------|------|------|
| **공통 예외** | `com.beanbliss.common.exception` | `ResourceNotFoundException`, `InvalidPageNumberException`, `InvalidPageSizeException` |
| **도메인 예외** | `com.beanbliss.domain.{도메인}.exception` | `InsufficientStockException` (inventory), `CouponExpiredException` (coupon), `OrderCancellationException` (order) |

```kotlin
// 공통 예외 예시
// com/beanbliss/common/exception/CommonExceptions.kt
package com.beanbliss.common.exception

class ResourceNotFoundException(message: String) : RuntimeException(message)
class InvalidPageNumberException(message: String) : RuntimeException(message)
class InvalidPageSizeException(message: String) : RuntimeException(message)

// 도메인 예외 예시
// com/beanbliss/domain/inventory/exception/InventoryExceptions.kt
package com.beanbliss.domain.inventory.exception

class InsufficientStockException(message: String) : RuntimeException(message)
class StockReservationFailedException(message: String) : RuntimeException(message)
```

### **7.2 ExceptionHandler 우선순위**

`@Order` 애노테이션으로 예외 처리 우선순위를 제어합니다.

```kotlin
// 도메인별 ExceptionHandler (높은 우선순위)
@ControllerAdvice
@Order(10)
class InventoryExceptionHandler {
    @ExceptionHandler(InsufficientStockException::class)
    fun handleInsufficientStock(ex: InsufficientStockException): ResponseEntity<ErrorResponse> { ... }
}

// 공통 ExceptionHandler (낮은 우선순위 - Fallback)
@ControllerAdvice
@Order(100)
class CommonExceptionHandler {
    @ExceptionHandler(InvalidPageNumberException::class)
    fun handleInvalidPageNumber(ex: InvalidPageNumberException): ResponseEntity<ErrorResponse> { ... }

    @ExceptionHandler(InvalidPageSizeException::class)
    fun handleInvalidPageSize(ex: InvalidPageSizeException): ResponseEntity<ErrorResponse> { ... }
}
```

**처리 순서**: 도메인 핸들러(`@Order(10)`) → 공통 핸들러(`@Order(100)`)

### **7.3 베스트 프랙티스**

1. **공통 예외는 common 패키지**, 도메인 예외는 도메인 패키지에 정의
2. **@Order 로 우선순위 관리**: 도메인(`@Order(10)`), 공통(`@Order(100)`)
3. **일관된 에러 응답**: `ErrorResponse(status, code, message)` 사용

## 8. 📄 페이지네이션 공통 처리 (Common Layer)

### **8.1 PageCalculator 사용**

페이지네이션 계산은 공통 유틸리티 `PageCalculator`를 사용합니다.

```kotlin
// Service에서 사용 예시
import com.beanbliss.common.pagination.PageCalculator

val totalPages = PageCalculator.calculateTotalPages(totalElements, size)
```

### **8.2 Service 구현 패턴**

```kotlin
@Service
class InventoryServiceImpl(
    private val inventoryRepository: InventoryRepository
) : InventoryService {

    override fun getInventories(page: Int, size: Int): InventoryListResponse {
        // 1. 유효성 검증
        if (page < 1) throw InvalidPageNumberException("페이지 번호는 1 이상이어야 합니다.")
        if (size !in 1..100) throw InvalidPageSizeException("페이지 크기는 1 이상 100 이하여야 합니다.")

        // 2. 데이터 조회
        val inventories = inventoryRepository.findAllWithProductInfo(page, size, "created_at", "DESC")
        val totalElements = inventoryRepository.count()

        // 3. 페이지 계산 (PageCalculator 사용)
        val totalPages = PageCalculator.calculateTotalPages(totalElements, size)

        // 4. 응답 조립
        return InventoryListResponse(
            content = inventories,
            pageable = PageableResponse(page, size, totalElements, totalPages)
        )
    }
}
```

### **8.3 베스트 프랙티스**

1. **PageCalculator 필수 사용**: 직접 계산 금지 (`(total + size - 1) / size` ❌)
2. **공통 예외 사용**: `InvalidPageNumberException`, `InvalidPageSizeException`
3. **Controller 기본값**: `@RequestParam(defaultValue = "1")` page, `(defaultValue = "10")` size
4. **응답 통일**: 모든 페이지네이션 API는 `PageableResponse` 사용

## 9. 📝 파일 네이밍 가이드

파일명은 **기능, 도메인, 계층**을 명확하게 표현하여 직관적으로 파악할 수 있도록 작성합니다.

### **9.1 기본 파일 네이밍 규칙**

기본 패턴: **`[Feature][Domain][Layer].kt`**

- **Feature**: 기능 동작 (예: `Get`, `Create`, `Update`, `Delete`, `Reduce`, `Reserve`)
- **Domain**: 도메인 이름 (예: `Products`, `Product`, `Order`, `Inventory`, `Stock`)
- **Layer**: 계층 이름 (예: `Controller`, `Service`, `UseCase`, `Repository`)

**예시**:
```
GetProductsService.kt           // 상품 목록 조회 Service (클래스)
CreateOrderUseCase.kt           // 주문 생성 UseCase (클래스)
ReduceStockService.kt           // 재고 차감 Service (클래스)
ProductRepository.kt            // 상품 Repository Interface
ProductRepositoryImpl.kt        // 상품 Repository 구현체
```

### **9.2 DIP 적용 시 네이밍 규칙 (Repository만 적용)**

**Repository 계층만 Interface와 구현체 분리**:
- **Repository Interface**: `[Domain]Repository.kt`
- **Repository 구현체**: `[Domain]RepositoryImpl.kt`

**Service, UseCase, Controller는 클래스만 사용** (Interface 없음):
- **Service**: `[Feature][Domain]Service.kt` (클래스)
- **UseCase**: `[Feature][Domain]UseCase.kt` (클래스)
- **Controller**: `[Domain]Controller.kt` (클래스)

**예시**:
```kotlin
// Repository Interface (DIP 적용)
// ProductRepository.kt
package com.beanbliss.domain.product.repository

interface ProductRepository {
    fun findAll(category: String?, sort: String?): List<Product>
    fun findById(productId: Long): Product?
}

// Repository 구현체
// ProductRepositoryImpl.kt
package com.beanbliss.domain.product.repository

@Repository
class ProductRepositoryImpl(
    private val jpaRepository: ProductJpaRepository
) : ProductRepository {
    override fun findAll(category: String?, sort: String?): List<Product> {
        return jpaRepository.findAll().map { it.toDomain() }
    }
}

// Service 클래스 (Interface 없음)
// GetProductsService.kt
package com.beanbliss.domain.product.service

@Service
class GetProductsService(
    private val productRepository: ProductRepository  // Repository Interface에만 의존
) {
    fun execute(category: String?, sort: String?): List<Product> {
        return productRepository.findAll(category, sort)
    }
}
```

### **9.3 Controller 네이밍 규칙**

Controller는 **도메인별로 하나의 파일**로 유지하고, 여러 기능을 메서드로 포함합니다.

**패턴**: `[Domain]Controller.kt`

**예시**:
```
ProductController.kt            // 상품 관련 모든 API 엔드포인트
OrderController.kt              // 주문 관련 모든 API 엔드포인트
CouponController.kt             // 쿠폰 관련 모든 API 엔드포인트
```

### **9.4 UseCase 네이밍 규칙**

UseCase는 **단일 기능**을 수행하므로, 일반적으로 Interface 없이 클래스로 정의합니다.

**패턴**: `[Feature][Domain]UseCase.kt`

**예시**:
```
CreateOrderUseCase.kt           // 주문 생성 UseCase
IssueCouponUseCase.kt           // 쿠폰 발급 UseCase
ProcessPaymentUseCase.kt        // 결제 처리 UseCase
```

### **9.5 테스트 파일 네이밍 규칙**

테스트 파일은 **테스트 대상 파일명 + `Test.kt`** 형식으로 작성합니다.

**패턴**:
- Interface 테스트: `[Feature][Domain][Layer]Test.kt`
- 구현체 테스트: `[Feature][Domain][Layer]ImplTest.kt` 또는 `[Feature][Domain][Layer]Test.kt`

**일반적으로 구현체를 테스트**하므로, `Impl`을 생략하고 `Test.kt`만 붙입니다.

### **9.6 테스트 파일 네이밍 예시**

#### **Controller 계층 테스트**
```
GetProductsControllerTest.kt          // 상품 목록 조회 API 테스트
GetProductControllerTest.kt           // 상품 상세 조회 API 테스트
CreateProductControllerTest.kt        // 상품 생성 API 테스트
CreateOrderControllerTest.kt          // 주문 생성 API 테스트
```

#### **UseCase 계층 테스트**
```
CreateOrderUseCaseTest.kt             // 주문 생성 UseCase 테스트
IssueCouponUseCaseTest.kt             // 쿠폰 발급 UseCase 테스트
ProcessPaymentUseCaseTest.kt          // 결제 처리 UseCase 테스트
```

#### **Service 계층 테스트**
```
GetProductsServiceTest.kt             // 상품 목록 조회 Service 테스트 (구현체 테스트)
CreateProductServiceTest.kt           // 상품 생성 Service 테스트
ReduceStockServiceTest.kt             // 재고 차감 Service 테스트
ReserveStockServiceTest.kt            // 재고 예약 Service 테스트
```

#### **Repository 계층 테스트**
```
ProductRepositoryTest.kt              // 상품 Repository 전반적인 기능 테스트
InventoryRepositoryTest.kt            // 재고 Repository 전반적인 기능 테스트
OrderRepositoryTest.kt                // 주문 Repository 전반적인 기능 테스트
```

### **9.7 클래스명 및 DisplayName**

테스트 파일명과 일치하도록 클래스명과 `@DisplayName`을 작성합니다.

```kotlin
// 파일명: GetProductsControllerTest.kt
@WebMvcTest(ProductController::class)
@DisplayName("상품 목록 조회 Controller 테스트")
class GetProductsControllerTest {
    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockkBean
    private lateinit var getProductsService: GetProductsService  // Service 클래스를 Mocking

    @Test
    fun `상품 목록 조회 성공`() {
        // Given
        val mockProducts = listOf(Product(1L, "에티오피아 예가체프", 15000, 100, "원두"))
        every { getProductsService.execute(any(), any()) } returns mockProducts

        // When & Then
        mockMvc.perform(get("/api/v1/products"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.products").isArray)
    }
}

// 파일명: CreateOrderUseCaseTest.kt
@DisplayName("주문 생성 UseCase 테스트")
class CreateOrderUseCaseTest {
    // Service 클래스들을 Mocking
    @MockkBean
    private lateinit var createOrderService: CreateOrderService

    @MockkBean
    private lateinit var reduceStockService: ReduceStockService

    @MockkBean
    private lateinit var deductBalanceService: DeductBalanceService

    @MockkBean
    private lateinit var useCouponService: UseCouponService

    private lateinit var createOrderUseCase: CreateOrderUseCase

    @BeforeEach
    fun setup() {
        // UseCase는 클래스이므로 직접 생성
        createOrderUseCase = CreateOrderUseCase(
            createOrderService,
            reduceStockService,
            deductBalanceService,
            useCouponService
        )
    }

    @Test
    fun `주문 생성 시 재고 차감과 주문 생성이 순서대로 호출되어야 한다`() {
        // Given
        every { reduceStockService.execute(any(), any()) } just Runs
        every { createOrderService.execute(any(), any(), any(), any()) } returns mockOrder
        every { deductBalanceService.execute(any(), any()) } just Runs

        // When
        createOrderUseCase.execute(userId = 1L, request = mockRequest)

        // Then - Service 조율 순서 검증
        verifyOrder {
            reduceStockService.execute(any(), any())
            createOrderService.execute(any(), any(), any(), any())
            deductBalanceService.execute(any(), any())
        }
    }
}

// 파일명: GetProductsServiceTest.kt
@DisplayName("상품 목록 조회 Service 테스트")
class GetProductsServiceTest {
    @MockkBean
    private lateinit var productRepository: ProductRepository

    private lateinit var getProductsService: GetProductsService

    @BeforeEach
    fun setup() {
        // Service는 클래스이므로 직접 생성
        getProductsService = GetProductsService(productRepository)
    }

    @Test
    fun `카테고리별 상품 조회 시 Repository에 올바른 파라미터가 전달되어야 한다`() {
        // Given
        val category = "원두"
        every { productRepository.findAll(category, any()) } returns emptyList()

        // When
        getProductsService.execute(category, "price")

        // Then
        verify { productRepository.findAll(category, "price") }
    }
}

// 파일명: ProductRepositoryTest.kt
@DataJpaTest
@Testcontainers
@DisplayName("상품 Repository 테스트")
class ProductRepositoryTest {

    companion object {
        @Container
        @JvmStatic
        val postgresContainer = PostgreSQLContainer<Nothing>("postgres:15-alpine").apply {
            withDatabaseName("testdb")
            withUsername("test")
            withPassword("test")
        }

        @DynamicPropertySource
        @JvmStatic
        fun configureProperties(registry: DynamicPropertyRegistry) {
            registry.add("spring.datasource.url", postgresContainer::getJdbcUrl)
            registry.add("spring.datasource.username", postgresContainer::getUsername)
            registry.add("spring.datasource.password", postgresContainer::getPassword)
        }
    }

    @Autowired
    private lateinit var productRepository: ProductRepository

    @Test
    fun `상품 저장 후 조회 시 동일한 데이터가 반환되어야 한다`() {
        // Given
        val product = Product(
            id = 0L,
            name = "에티오피아 예가체프",
            price = 15000,
            stock = 100,
            category = "원두"
        )

        // When
        val savedProduct = productRepository.save(product)
        val foundProduct = productRepository.findById(savedProduct.id)

        // Then
        assertNotNull(foundProduct)
        assertEquals(product.name, foundProduct?.name)
        assertEquals(product.price, foundProduct?.price)
    }

    @Test
    fun `카테고리별 상품 조회 시 올바른 결과가 반환되어야 한다`() {
        // TestContainers를 사용한 실제 DB 쿼리 테스트
        val products = productRepository.findAll(category = "원두", sort = "price")

        // 실제 프로덕션 DB와 동일한 환경에서 쿼리 동작 검증
        assertTrue(products.all { it.category == "원두" })
    }
}
```

### **9.8 계층별 테스트 전략**

#### **Controller 테스트**
- `@WebMvcTest` 사용
- Service/UseCase를 Mocking하여 HTTP 계층만 테스트
- 입력 유효성 검증 테스트
- HTTP 상태 코드 및 응답 형식 검증

#### **UseCase 테스트**
- 여러 Service를 Mocking
- Service 조율 로직 테스트 (호출 순서, 횟수)
- 트랜잭션 경계 테스트
- 예외 전파 테스트

#### **Service 테스트**
- Repository를 Mocking
- 비즈니스 로직과 도메인 규칙 검증 테스트
- 예외 처리 테스트
- Repository 호출 검증 (verify)

#### **Repository 테스트**
- `@DataJpaTest` + **TestContainers** 사용
- 실제 프로덕션 DB와 동일한 환경에서 테스트 (PostgreSQL, MySQL 등)
- 쿼리 동작 검증
- Entity ↔ Domain Model 변환 검증
- DB 격리 환경 제공으로 테스트 신뢰성 향상

### **9.9 파일 구조 전체 예시**

```
src/main/kotlin/com/beanbliss/
├── domain/
│   ├── product/                              # 상품 도메인
│   │   ├── controller/
│   │   │   └── ProductController.kt          # 도메인별 Controller
│   │   ├── dto/
│   │   │   ├── GetProductsRequest.kt
│   │   │   ├── GetProductsResponse.kt
│   │   │   ├── ProductResponse.kt
│   │   │   └── ProductListResponse.kt
│   │   ├── service/
│   │   │   ├── GetProductsService.kt         # Service 클래스
│   │   │   ├── GetProductService.kt          # Service 클래스
│   │   │   └── GetTopSellingProductsService.kt
│   │   ├── domain/
│   │   │   └── Product.kt                    # 도메인 모델
│   │   ├── repository/
│   │   │   ├── ProductRepository.kt          # Repository Interface (Domain Layer)
│   │   │   └── ProductRepositoryImpl.kt      # Repository 구현체 (Infrastructure Layer)
│   │   └── entity/
│   │       └── ProductEntity.kt              # JPA Entity (Infrastructure Layer)
│   │
│   ├── inventory/                            # 재고 도메인
│   │   ├── service/
│   │   │   ├── ReserveStockService.kt        # Service 클래스
│   │   │   ├── ReduceStockService.kt         # Service 클래스
│   │   │   └── ReleaseStockService.kt        # Service 클래스
│   │   ├── domain/
│   │   │   └── Inventory.kt
│   │   ├── repository/
│   │   │   ├── InventoryRepository.kt
│   │   │   └── InventoryRepositoryImpl.kt
│   │   └── entity/
│   │       └── InventoryEntity.kt
│   │
│   └── order/                                # 주문 도메인
│       ├── controller/
│       │   └── OrderController.kt
│       ├── dto/
│       │   ├── CreateOrderRequest.kt
│       │   └── CreateOrderResponse.kt
│       ├── usecase/
│       │   └── CreateOrderUseCase.kt         # UseCase 클래스 (여러 Service 조율)
│       ├── service/
│       │   └── CreateOrderService.kt         # Service 클래스
│       ├── domain/
│       │   └── Order.kt
│       ├── repository/
│       │   ├── OrderRepository.kt
│       │   └── OrderRepositoryImpl.kt
│       └── entity/
│           └── OrderEntity.kt
│
└── common/                                   # 공통 모듈
    ├── dto/
    │   └── PageableResponse.kt
    ├── pagination/
    │   └── PageCalculator.kt
    └── exception/
        ├── ResourceNotFoundException.kt
        └── CommonExceptionHandler.kt

src/test/kotlin/com/beanbliss/
├── common/
│   └── test/
│       └── RepositoryTestBase.kt             # TestContainers 공통 설정
│
└── domain/
    ├── product/
    │   ├── controller/
    │   │   ├── GetProductsControllerTest.kt      # 상품 목록 조회 API 테스트
    │   │   ├── GetProductControllerTest.kt       # 상품 상세 조회 API 테스트
    │   │   └── GetTopProductsControllerTest.kt   # 인기 상품 조회 API 테스트
    │   ├── service/
    │   │   ├── GetProductsServiceTest.kt         # 상품 목록 조회 Service 테스트
    │   │   ├── GetProductServiceTest.kt          # 상품 상세 조회 Service 테스트
    │   │   └── GetTopSellingProductsServiceTest.kt
    │   └── repository/
    │       └── ProductRepositoryTest.kt          # TestContainers 기반 Repository 테스트
    │
    ├── inventory/
    │   ├── service/
    │   │   ├── ReserveStockServiceTest.kt
    │   │   ├── ReduceStockServiceTest.kt
    │   │   └── ReleaseStockServiceTest.kt
    │   └── repository/
    │       └── InventoryRepositoryTest.kt
    │
    └── order/
        ├── controller/
        │   └── CreateOrderControllerTest.kt
        ├── usecase/
        │   └── CreateOrderUseCaseTest.kt
        ├── service/
        │   └── CreateOrderServiceTest.kt
        └── repository/
            └── OrderRepositoryTest.kt
```

### **9.10 네이밍 규칙 요약**

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

**핵심 원칙**:
- **Repository만 DIP 적용**: Interface + 구현체 분리
- **Service, UseCase, Controller**: 클래스만 사용 (Interface 없음)
- **Service는 Repository Interface에만 의존**하여 DIP 준수
