# 상품 목록 조회 API 설계

## 1. 개요
- **목적**: 주문 가능한 상품 목록과 각 상품의 옵션 정보, 가용 재고를 함께 조회
- **사용 시나리오**: 고객이 쇼핑몰 메인 페이지 또는 상품 목록 페이지에서 상품을 탐색할 때
- **PRD 참고**: PROD-001, PROD-002, PROD-003
- **연관 테이블**: PRODUCT, PRODUCT_OPTION, INVENTORY, INVENTORY_RESERVATION

---

## 2. API 명세

### 2.1 Endpoint
```
GET /api/products
```

### 2.2 Request Parameters
| 파라미터 | 타입 | 필수 | 설명 | 기본값 |
|---------|------|------|------|-----|
| page | Integer | N | 페이지 번호 (1부터 시작) | 1   |
| size | Integer | N | 페이지 크기 | 10  |

### 2.3 Request Example
```http
GET /api/products?page=1&size=10
```

### 2.4 Response (Success)
```json
{
  "data": {
    "content": [
      {
        "productId": 1,
        "name": "에티오피아 예가체프 G1",
        "description": "플로럴한 향과 밝은 산미가 특징인 에티오피아 대표 원두",
        "brand": "Bean Bliss",
        "createdAt": "2025-01-15T10:30:00",
        "options": [
          {
            "optionId": 1,
            "optionCode": "ETH-WB-200",
            "origin": "Ethiopia",
            "grindType": "WHOLE_BEANS",
            "weightGrams": 200,
            "price": 18000,
            "availableStock": 50
          },
          {
            "optionId": 2,
            "optionCode": "ETH-HD-200",
            "origin": "Ethiopia",
            "grindType": "HAND_DRIP",
            "weightGrams": 200,
            "price": 21000,
            "availableStock": 8
          },
          {
            "optionId": 3,
            "optionCode": "ETH-WB-500",
            "origin": "Ethiopia",
            "grindType": "WHOLE_BEANS",
            "weightGrams": 500,
            "price": 42000,
            "availableStock": 0
          }
        ]
      },
      {
        "productId": 2,
        "name": "콜롬비아 수프리모",
        "description": "균형잡힌 바디감과 부드러운 맛",
        "brand": "Bean Bliss",
        "createdAt": "2025-01-14T09:20:00",
        "options": [
          {
            "optionId": 4,
            "optionCode": "COL-HD-500",
            "origin": "Colombia",
            "grindType": "HAND_DRIP",
            "weightGrams": 500,
            "price": 38000,
            "availableStock": 25
          }
        ]
      }
    ],
    "pageable": {
      "pageNumber": 1,
      "pageSize": 10,
      "totalElements": 2,
      "totalPages": 1
    }
  }
}
```

### 2.5 Response Schema
```
{
  "data": {
    "content": [
      {
        "productId": long,           // 상품 ID
        "name": string,              // 상품명
        "description": string,       // 상품 설명
        "brand": string,             // 브랜드명
        "createdAt": datetime,       // 상품 등록일시
        "options": [
          {
            "optionId": long,        // 옵션 ID
            "optionCode": string,    // 옵션 코드 (SKU)
            "origin": string,        // 원산지
            "grindType": string,     // 분쇄 타입 (WHOLE_BEANS, HAND_DRIP, ESPRESSO)
            "weightGrams": int,      // 용량 (그램)
            "price": int,            // 가격 (원)
            "availableStock": int    // 가용 재고 수량
          }
        ]
      }
    ],
    "pageable": {
      "pageNumber": int,             // 현재 페이지 번호
      "pageSize": int,               // 페이지 크기
      "totalElements": long,         // 전체 상품 수
      "totalPages": int              // 전체 페이지 수
    }
  }
}
```

### 2.6 HTTP Status Codes
| 상태 코드 | 설명 |
|-----------|------|
| 200 OK | 정상 조회 |
| 400 Bad Request | 잘못된 요청 파라미터 |
| 500 Internal Server Error | 서버 내부 오류 |

### 2.7 Error Codes
| 에러 코드 | 설명 | HTTP Status |
|-----------|------|-------------|
| INVALID_PARAMETER | 요청 파라미터가 유효하지 않음 | 400 |

---

## 3. 비즈니스 로직

### 3.1 가용 재고 계산
- `availableStock`은 실시간으로 계산되어 반환됩니다
- 계산식: `INVENTORY.stock_quantity - SUM(INVENTORY_RESERVATION.quantity WHERE status = 'RESERVED')`
- **성능 최적화**: 모든 옵션의 재고를 한 번의 쿼리로 일괄 조회 (Batch Query)
  - `InventoryRepository.calculateAvailableStockBatch(optionIds)` 사용
  - N+1 문제 방지
- 참고: `docs/concurrency/inventory.md`

### 3.2 정렬 규칙
- 기본 정렬: `created_at DESC` (최신 등록 상품이 먼저 노출)
- 정렬 파라미터는 `sortBy`와 `sortDirection`으로 제어 가능 (향후 확장 가능)
- 현재 구현: `findActiveProducts(page, size, sortBy = "created_at", sortDirection = "DESC")`

### 3.3 필터링 조건
- **옵션 레벨**: `PRODUCT_OPTION`의 `is_active = true`인 옵션만 포함
- **활성 옵션 존재 여부**: 최소 1개 이상의 활성 옵션이 있는 상품만 포함
  - 모든 옵션이 비활성(`is_active = false`)인 상품은 구매 불가능하므로 목록에서 제외
  - 예: 상품 A가 활성(`is_active = true`)이더라도, 모든 옵션이 비활성이면 목록에 노출되지 않음
- 비활성 상품 및 옵션은 응답에 포함되지 않음
- 페이지네이션은 활성 상품에 대해서만 적용

---

## 4. 구현 시 고려사항

### 4.1 성능 최적화
- **상품 조회**: 상품과 옵션을 Fetch Join으로 한 번에 조회 (N+1 문제 방지)
- **재고 조회**: Bulk 쿼리로 모든 옵션의 재고를 한 번에 조회 (N+1 문제 방지)
  - `WHERE product_option_id IN (...)` 사용
  - Map<optionId, availableStock> 형태로 반환
- 페이징 처리를 위한 인덱스 활용 (created_at, is_active)

### 4.2 동시성 제어
- 가용 재고는 조회 시점 기준 계산값
- 실제 주문 시 재고 재검증 필요

### 4.3 데이터 일관성
- 비활성 옵션(`PRODUCT_OPTION.is_active = false`)은 응답에서 제외
- 활성 상품이지만 활성 옵션이 없는 경우 해당 상품을 목록에서 제외

---

## 5. 레이어드 아키텍처 흐름

```mermaid
sequenceDiagram
    participant Client as Client
    participant Controller as ProductController
    participant UseCase as GetProductsUseCase
    participant ProdService as ProductService
    participant InvService as InventoryService
    participant ProdRepo as ProductRepository
    participant InvRepo as InventoryRepository
    participant DB as Database

    Client->>Controller: GET /api/products?page=1&size=10
    activate Controller

    Note over Controller: 페이징 파라미터 검증

    Controller->>UseCase: getProducts(page, size)
    activate UseCase

    Note over UseCase: UseCase 오케스트레이션 시작

    UseCase->>ProdService: getActiveProducts(page, size, sortBy, sortDirection)
    activate ProdService

    ProdService->>ProdRepo: findActiveProducts(page, size, "created_at", "DESC")
    activate ProdRepo

    Note over ProdRepo,DB: 상품 + 옵션 조회<br/>- Fetch Join (Product + Options)<br/>- is_active = true 필터링<br/>- sortBy, sortDirection 기준 정렬<br/>- 페이징 적용

    ProdRepo-->>ProdService: List<ProductResponse>
    deactivate ProdRepo

    ProdService-->>UseCase: List<ProductResponse>
    deactivate ProdService

    UseCase->>ProdService: countActiveProducts()
    activate ProdService

    ProdService->>ProdRepo: countActiveProducts()
    activate ProdRepo

    Note over ProdRepo,DB: 활성 상품 총 개수<br/>- is_active = true 필터링

    ProdRepo-->>ProdService: Long (totalElements)
    deactivate ProdRepo

    ProdService-->>UseCase: Long (totalElements)
    deactivate ProdService

    Note over UseCase: 옵션 ID 목록 추출<br/>(Batch 재고 조회 준비)

    UseCase->>InvService: calculateAvailableStockBatch(optionIds)
    activate InvService

    Note over InvService: Batch 재고 조회 요청

    InvService->>InvRepo: calculateAvailableStockBatch(optionIds)
    activate InvRepo

    Note over InvRepo,DB: 재고 정보 일괄 조회<br/>- WHERE product_option_id IN (...)<br/>- stock - reserved 계산<br/>- Map<optionId, availableStock> 반환

    InvRepo-->>InvService: Map<optionId, availableStock>
    deactivate InvRepo

    InvService-->>UseCase: Map<optionId, availableStock>
    deactivate InvService

    Note over UseCase: 상품 데이터 + 재고 데이터 결합<br/>옵션 없는 상품 필터링<br/>응답 DTO 조립

    UseCase-->>Controller: ProductListResponse
    deactivate UseCase

    Controller-->>Client: 200 OK (JSON)
    deactivate Controller
```

### 5.1 성능 최적화 전략

#### Batch 재고 조회 (실제 구현 기준)
```kotlin
// ❌ 이전 방식 (N+1 문제)
options.forEach { option ->
    val stock = inventoryRepository.calculateAvailableStock(option.id)
}

// ✅ 현재 구현 (Batch 조회)
// 1. 모든 optionId 수집
val allOptionIds = products.flatMap { it.options.map { option -> option.optionId } }

// 2. 한 번의 쿼리로 모든 재고 조회
val stockMap = inventoryRepository.calculateAvailableStockBatch(allOptionIds)

// 3. Map 기반 매칭
val optionsWithStock = product.options.map { option ->
    option.copy(availableStock = stockMap[option.optionId] ?: 0)
}
```

- 옵션 개수만큼 쿼리를 날리는 대신, **단 1번의 쿼리**로 모든 재고 조회
- `WHERE product_option_id IN (1, 2, 3, ...)`을 사용한 일괄 조회
- `Map<Long, Int>` 형태로 반환하여 빠른 매핑 가능
- 재고가 없는 경우 기본값 0 처리

### 5.2 트랜잭션 범위
- **격리 수준**: READ_COMMITTED
- **읽기 전용**: `@Transactional(readOnly = true)`
- UseCase 레벨에서 트랜잭션 시작
- 별도의 데이터 수정이 없으므로 트랜잭션 롤백 불필요

### 5.3 예외 처리
1. **파라미터 검증 실패** (Controller에서 처리)
   - `page`가 1 미만 → `INVALID_PARAMETER` (400)
   - `size`가 1 미만 또는 100 초과 → `INVALID_PARAMETER` (400)

2. **데이터베이스 오류** (GlobalExceptionHandler에서 처리)
   - DB 연결 실패 → `INTERNAL_SERVER_ERROR` (500)
   - 쿼리 실행 실패 → `INTERNAL_SERVER_ERROR` (500)
