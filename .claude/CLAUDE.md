# 🚀 CLAUDE_Layered.md: Kotlin Spring Boot E-commerce API 개발 가이드

이 문서는 전통적인 **레이어드 아키텍처**를 기반으로 E-commerce API를 개발하는 가이드입니다. 
각 계층의 책임을 명확히 분리하고 **DIP (의존성 역전 원칙)** 를 준수하여 견고한 시스템을 구축하는 데 집중합니다.

## 0. 🎯 핵심 개발 원칙 및 목표

1. **레이어드 아키텍처 준수**: `Controller`, `Service`, `Repository` 계층의 명확한 분리. Presentation Layer → Business Layer → Persistence Layer → Infrastructure Layer  계층 순으로 상위 계층은 하위 계층에만 의존하고, 하위 계층은 상위 계층을 알지 말아야 점에 유의합니다.
2. **DIP 적용**: 모든 계층 간의 의존성은 **인터페이스**를 통해 역전됩니다.
3. **TDD 접근**: 단위 테스트를 통해 비즈니스 로직과 책임 분산을 검증합니다. 기능 로직이 올바르게 구현되었는지를 검증하는 것보다 클래스의 책임이 올바르게 분산되고, 로직이 올바르게 추상화되었는지 설계를 검증하는 목적에 집중합니다.
4. **패키지 경로**: `com.beanbliss.domain`, `com.beanbliss.common` 을 사용합니다. 자세한 내용은 프로젝트 구조 내용을 참고하세요.
5. **테스트 파일 네이밍 규칙**: 기능별로 명확한 네이밍을 사용하여 테스트의 목적과 범위를 명시합니다. 자세한 내용은 "테스트 네이밍 가이드" 섹션을 참고하세요.

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
   - 확장성

5. **레이어드 아키텍처 흐름**
   - Mermaid 시퀀스 다이어그램으로 표현
   - JPA 기반 추상화된 쿼리나 메서드 호출로 작성
   - 트랜잭션 범위와 격리 수준 명시
   - 예외 처리 흐름 명시

- 설계 문서 내용은 한국어로 작성합니다.

## 2. 📂 프로젝트 구조 (패키지 가이드)

계층별로 패키지를 분리하여 관심사를 분리합니다. 다중 서비스 조율 시, UseCase 를 사용합니다.

```
src/main/kotlin/com/beanbliss
└── domain
    └── inventory
        ├── controller  // Web Layer (API 엔드포인트)
        ├── service     // Application/Business Layer (Service Interface + Implementation)
        └── repository  // Persistence Layer (Repository Interface + Implementation)
└── common
    └── exception   // 공통 예외 클래스 및 핸들러

```

## 3. 📦 도메인 모델 (Domain Layer) 가이드: Inventory

재고 수량 변경 및 규칙을 담당하는 핵심 도메인 객체입니다.

### **코드 3.1: Inventory.kt (도메인 모델)**

```
// com/beanbliss/domain/inventory/domain/Inventory.kt
package com.beanbliss.domain.inventory.domain

import com.beanbliss.common.exception.InsufficientStockException

/**
 * [책임]: 재고 수량의 상태 관리 및 재고 부족 규칙 수행.
 * SOLID - SRP: 재고 상태 변경에 대한 책임만 가짐.
 */
data class Inventory(
    val productId: Long, // 불변 ID
    var stock: Int       // 가변 재고 상태
) {
    /**
     * 재고를 감소시키는 핵심 비즈니스 로직.
     */
    fun reduceStock(quantity: Int) {
        if (this.stock < quantity) {
            throw InsufficientStockException("상품 ID: $productId 의 재고가 부족합니다. 현재 재고: ${this.stock}, 요청 수량: $quantity")
        }
        this.stock -= quantity
    }
}

```

## 4. 🗄️ 영속성 계층 (Persistence Layer): Repository

Service가 의존할 **Repository Interface**를 정의하여 DIP를 구현하고, 그 구현체를 분리합니다.

### **코드 4.1: InventoryRepository Interface (추상화)**

```
// com/beanbliss/domain/inventory/repository/InventoryRepository.kt
package com.beanbliss.domain.inventory.repository

import com.beanbliss.domain.inventory.domain.Inventory

/**
 * [책임]: 영속성 계층의 '계약' 정의.
 * Service는 이 인터페이스에만 의존합니다. (DIP 준수)
 */
interface InventoryRepository {
    fun findById(productId: Long): Inventory?
    fun save(inventory: Inventory): Inventory
}

```

### **코드 4.2: InventoryRepositoryImpl (구현체)**

```
// com/beanbliss/domain/inventory/repository/InventoryRepositoryImpl.kt
package com.beanbliss.domain.inventory.repository

import com.beanbliss.domain.inventory.domain.Inventory
import org.springframework.stereotype.Repository

@Repository // 스프링 빈으로 등록
class InventoryRepositoryImpl(
    // private val jpaRepository: SpringDataJpaInventoryRepository // 실제 JPA 연동 객체 주입 가정
) : InventoryRepository {

    override fun findById(productId: Long): Inventory? {
        // [TODO: 실제 DB 조회 및 DB Entity를 Domain Model로 변환 로직 구현]
        // 현재는 임시 Mock 데이터를 반환합니다.
        return Inventory(productId, 100)
    }

    override fun save(inventory: Inventory): Inventory {
        // [TODO: Domain Model을 DB Entity로 변환 후 저장 로직 구현]
        return inventory
    }
}

```

## 5. ⚙️ 비즈니스 계층 (Application Layer): Service

Controller가 의존할 **Service Interface**를 정의하여 DIP를 구현하고, 핵심 비즈니스 로직을 처리합니다.
여러 Service, Repository, infrastructure 컴포넌트를 조합하여 복잡한 비즈니스 트랜잭션 또는 특정 흐름을 완성하기 위해서는 UseCase 를 사용합니다.

### **코드 5.1: InventoryService Interface (추상화)**

```
// com/beanbliss/domain/inventory/service/InventoryService.kt
package com.beanbliss.domain.inventory.service

/**
 * [책임]: 재고 관리 기능의 '계약' 정의.
 * Controller는 이 인터페이스에만 의존합니다. (DIP 준수)
 */
interface InventoryService {
    fun reduceStock(productId: Long, quantity: Int)
}

```

### **코드 5.2: InventoryServiceImpl (Service 구현체)**

```
// com/beanbliss/domain/inventory/service/InventoryServiceImpl.kt
package com.beanbliss.domain.inventory.service

import com.beanbliss.domain.inventory.repository.InventoryRepository // Repository Interface 임포트
import com.beanbliss.common.exception.ResourceNotFoundException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class InventoryServiceImpl(
    // DIP 준수: 구현체가 아닌 Repository Interface에 의존합니다.
    private val inventoryRepository: InventoryRepository
) : InventoryService {

    override fun reduceStock(productId: Long, quantity: Int) {
        // 1. 재고 조회 (Repository Interface 호출)
        val inventory = inventoryRepository.findById(productId)
            ?: throw ResourceNotFoundException("상품 ID: $productId 의 재고 정보를 찾을 수 없습니다.")

        // 2. 도메인 모델에 비즈니스 로직 위임 (SRP 준수)
        inventory.reduceStock(quantity)

        // 3. 변경 사항 저장
        inventoryRepository.save(inventory)
    }
}

```


### **코드 5.3: UseCase 기반 복합 도메인 의존성 결합**
```
package com.beanbliss.domain.product.usecases

import com.beanbliss.domain.product.domain.Product
import com.beanbliss.domain.product.repository.ProductRepository
import com.beanbliss.domain.product.repository.CacheService
import org.springframework.stereotype.Component

// UseCase 구현체는 UseCase 인터페이스를 구현하거나 (더 엄격한 DIP), 
// Facade 역할을 위해 클래스 자체로 정의될 수 있습니다. 여기서는 클래스 자체로 정의합니다.
@Component
class ProductUseCase(
    // ProductRepository: DB 관련 도메인 의존성
    private val productRepository: ProductRepository, 
    // CacheService: 인프라 관련 도메인 외 의존성
    private val cacheService: CacheService 
) {
    /**
     * 상품 목록 조회: 캐시 사용 여부를 결정하고, 없으면 DB에서 조회 후 캐시에 저장합니다.
     */
    fun getProducts(category: String?, sort: String?): List<Product> {
        val cacheKey = "products:" + (category ?: "all") + ":" + sort

        // 1. 인프라 컴포넌트(Cache) 확인
        val cached = cacheService.get(cacheKey) as? List<Product>
        if (cached != null) {
            println("Cache Hit: $cacheKey")
            return cached
        }

        // 2. 도메인 Repository(DB) 조회
        val products = productRepository.findAll(category, sort)
        
        // 3. 인프라 컴포넌트(Cache) 저장 (TTL = 60초)
        cacheService.set(cacheKey, products, 60)
        
        return products
    }

    /**
     * 인기 상품 조회: 복잡한 비즈니스 로직(기간 계산, 상위 판매 상품 조회)을 수행합니다.
     */
    fun getTopProducts(days: Int, limit: Int): TopProductResponse {
        // 조회 기간 계산 (복잡한 로직)
        val now = System.currentTimeMillis()
        val from = now - (days * 24L * 60 * 60 * 1000)

        // 상위 판매 상품 조회
        val topProducts = productRepository.findTopSelling(from, limit)

        return TopProductResponse("${days} days", topProducts)
    }

    /**
     * 재고 확인: ProductService가 아닌, ProductRepository를 직접 사용하여 빠르게 재고 정보만 반환합니다.
     */
    fun checkStock(productId: String, quantity: Int): StockCheckResponse {
        val product = productRepository.findById(productId)
            ?: throw IllegalStateException("상품을 찾을 수 없습니다.")

        // 재고 정보 반환
        return StockCheckResponse(
            product.hasStock(quantity),
            product.stock,
            quantity
        )
    }
}
```

### **코드 5.4: TDD 기반 책임 검증 (InventoryService Test)**

Service 계층의 단위 테스트는 **Repository Interface**를 Mocking하여 비즈니스 로직의 올바른 수행과 책임 분산 여부를 검증합니다.

```
// src/test/kotlin/com/beanbliss/domain/inventory/service/InventoryServiceTest.kt
// (Mockk, JUnit 5 사용 가정)

import com.beanbliss.domain.inventory.repository.InventoryRepository
import com.beanbliss.domain.inventory.domain.Inventory
import com.beanbliss.common.exception.*
import io.mockk.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class InventoryServiceTest {
    // Repository Interface를 Mockk으로 Mocking
    private val inventoryRepository: InventoryRepository = mockk()
    private val inventoryService: InventoryService = InventoryServiceImpl(inventoryRepository)

    @Test
    fun `재고 감소 성공 시_Repository의 findById와 save가 호출되어야 한다`() {
        // Given
        val productId = 1L
        val mockInventory = Inventory(productId, 10)

        // Mocking 설정
        every { inventoryRepository.findById(productId) } returns mockInventory
        every { inventoryRepository.save(any()) } returns mockInventory

        // When
        inventoryService.reduceStock(productId, 3)

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
            inventoryService.reduceStock(productId, 1)
        }

        // [TDD 검증 목표 3]: SRP 준수 - 예외 발생 시, 불필요한 save 로직은 호출되지 않았는가?
        verify(exactly = 0) { inventoryRepository.save(any()) }
    }
}

```


## 6. 🌐 웹 계층 (Web Layer): Controller

클라이언트의 요청을 받고 유효성 검사 후, **Service Interface**에 위임하는 역할만 수행합니다.

### **코드 6.1: InventoryController**

```
// com/beanbliss/domain/inventory/controller/InventoryController.kt
package com.beanbliss.domain.inventory.controller

import com.beanbliss.domain.inventory.service.InventoryService // Service Interface 임포트
import org.springframework.web.bind.annotation.*
import org.springframework.http.ResponseEntity
import jakarta.validation.Valid
import jakarta.validation.constraints.Min

// API 요청 DTO는 data class를 사용
data class ReduceStockRequest(
    @field:Min(1, message = "수량은 1개 이상이어야 합니다.") // 유효성 검사 추가
    val quantity: Int
)

@RestController
@RequestMapping("/api/v1/inventories")
class InventoryController(
    // DIP 준수: 구현체가 아닌 Service Interface에 의존합니다.
    private val inventoryService: InventoryService
) {

    @PostMapping("/{productId}/reduce")
    fun reduceStock(@PathVariable productId: Long, @Valid @RequestBody request: ReduceStockRequest): ResponseEntity<Unit> {

        // Service 계층에 위임
        inventoryService.reduceStock(productId, request.quantity)

        // 204 No Content 반환
        return ResponseEntity.noContent().build()
    }
}

```

## 7. ⚠️ 공통 예외 처리 (Common Layer)

예외 처리를 공통 모듈로 분리하여 API의 일관성을 확보합니다.

### **코드 7.1: GlobalExceptionHandler**

```
// com/beanbliss/common/exception/GlobalExceptionHandler.kt
package com.beanbliss.common.exception

import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.MethodArgumentNotValidException

// 사용자 정의 예외 클래스
class ResourceNotFoundException(message: String) : RuntimeException(message)
class InsufficientStockException(message: String) : RuntimeException(message)

// 공통 예외 응답 DTO
data class ErrorResponse(val status: Int, val code: String, val message: String)

@ControllerAdvice
class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException::class)
    fun handleResourceNotFound(ex: ResourceNotFoundException): ResponseEntity<ErrorResponse> {
        val response = ErrorResponse(HttpStatus.NOT_FOUND.value(), "RESOURCE_NOT_FOUND", ex.message ?: "요청한 자원을 찾을 수 없습니다.")
        return ResponseEntity(response, HttpStatus.NOT_FOUND) // 404
    }

    @ExceptionHandler(InsufficientStockException::class)
    fun handleInsufficientStock(ex: InsufficientStockException): ResponseEntity<ErrorResponse> {
        val response = ErrorResponse(HttpStatus.BAD_REQUEST.value(), "INSUFFICIENT_STOCK", ex.message ?: "재고가 부족합니다.")
        return ResponseEntity(response, HttpStatus.BAD_REQUEST) // 400
    }

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleValidationExceptions(ex: MethodArgumentNotValidException): ResponseEntity<ErrorResponse> {
        val errorMessage = ex.bindingResult.fieldErrors.firstOrNull()?.defaultMessage ?: "유효성 검사에 실패했습니다."

        val response = ErrorResponse(HttpStatus.BAD_REQUEST.value(), "INVALID_INPUT", errorMessage)
        return ResponseEntity(response, HttpStatus.BAD_REQUEST) // 400
    }
}

```

## 8. 📝 테스트 네이밍 가이드

테스트 파일의 이름은 **테스트 대상의 도메인, 기능, 그리고 계층**을 명확하게 표현해야 합니다. 이를 통해 프로젝트가 성장하더라도 각 테스트의 목적과 범위를 즉시 파악할 수 있습니다.

### **8.1 테스트 파일 네이밍 규칙**

기본 패턴: **`{Domain}{Feature}{Layer}Test.kt`**

- **Domain**: 도메인 이름 (예: `Product`, `Order`, `Inventory`)
- **Feature**: 테스트하는 기능 (예: `List`, `Create`, `Update`, `Delete`, `Top`)
- **Layer**: 계층 이름 (예: `Controller`, `Service`, `Repository`)

### **8.2 네이밍 예시**

#### **Controller, Service 계층 테스트**
```
ProductListControllerTest.kt          // 상품 목록 조회 API 테스트
ProductCreateControllerTest.kt        // 상품 생성 API 테스트
ProductListServiceTest.kt             // 상품 목록 조회 비즈니스 로직 테스트
ProductCreateServiceTest.kt           // 상품 생성 비즈니스 로직 테스트
```

#### **Repository 계층 테스트**
```
ProductRepositoryTest.kt              // 상품 Repository 전반적인 기능 테스트
```

### **8.3 클래스명 및 DisplayName**

테스트 파일명과 일치하도록 클래스명과 `@DisplayName`을 작성합니다.

```kotlin
// 파일명: ProductListControllerTest.kt
@WebMvcTest(ProductController::class)
@DisplayName("상품 목록 조회 Controller 테스트")
class ProductListControllerTest {
    // ...
}

// 파일명: ProductListServiceTest.kt
@DisplayName("상품 목록 조회 Service 테스트")
class ProductListServiceTest {
    // ...
}
```
