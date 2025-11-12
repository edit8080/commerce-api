package com.beanbliss.domain.inventory.controller

import com.beanbliss.common.dto.ApiResponse
import com.beanbliss.common.dto.PageableResponse
import com.beanbliss.common.pagination.PageCalculator
import com.beanbliss.domain.inventory.dto.InventoryAddStockRequest
import com.beanbliss.domain.inventory.dto.InventoryAddStockResponse
import com.beanbliss.domain.inventory.dto.InventoryListResponse
import com.beanbliss.domain.inventory.dto.InventoryResponse
import com.beanbliss.domain.inventory.usecase.GetInventoriesUseCase
import com.beanbliss.domain.inventory.usecase.InventoryAddStockUseCase
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

/**
 * 재고 관리 Controller
 *
 * [책임]:
 * - HTTP 요청 수신 및 응답 처리
 * - 요청 파라미터 바인딩
 * - UseCase 계층 호출 및 결과 반환
 *
 * [DIP 준수]:
 * - UseCase에만 의존
 */
@RestController
@RequestMapping("/api/inventories")
@Tag(name = "재고 관리", description = "재고 조회 및 추가 API")
class InventoryController(
    private val getInventoriesUseCase: GetInventoriesUseCase,
    private val inventoryAddStockUseCase: InventoryAddStockUseCase
) {

    /**
     * 재고 목록 조회 API
     *
     * GET /api/inventories?page={page}&size={size}
     *
     * @param page 페이지 번호 (기본값: 1, 최소: 1)
     * @param size 페이지 크기 (기본값: 10, 범위: 1~100)
     * @return 재고 목록 + 페이징 정보
     */
    @GetMapping
    @Operation(summary = "재고 목록 조회", description = "상품별 재고 목록 조회 (페이지네이션)")
    fun getInventories(
        @Parameter(description = "페이지 번호 (1부터 시작)", example = "1")
        @RequestParam(defaultValue = "1")
        @Min(value = 1, message = "페이지 번호는 1 이상이어야 합니다.")
        page: Int,

        @Parameter(description = "페이지 크기 (1-100)", example = "10")
        @RequestParam(defaultValue = "10")
        @Min(value = 1, message = "페이지 크기는 1 이상이어야 합니다.")
        @Max(value = 100, message = "페이지 크기는 100 이하여야 합니다.")
        size: Int
    ): ResponseEntity<ApiResponse<InventoryListResponse>> {
        // 1. UseCase 계층에 위임 (INVENTORY + PRODUCT 조합 데이터 반환)
        val useCaseResult = getInventoriesUseCase.getInventories(page, size)

        // 2. UseCase DTO → Presentation DTO 변환 (Controller 책임)
        val inventoryResponses = useCaseResult.inventories.map { inventory ->
            InventoryResponse(
                inventoryId = inventory.inventoryId,
                productId = inventory.productId,
                productName = inventory.productName,
                productOptionId = inventory.productOptionId,
                optionCode = inventory.optionCode,
                optionName = inventory.optionName,
                price = inventory.price,
                stockQuantity = inventory.stockQuantity,
                createdAt = inventory.createdAt
            )
        }

        // 3. 페이징 정보 계산
        val totalPages = PageCalculator.calculateTotalPages(useCaseResult.totalElements, size)
        val pageable = PageableResponse(
            pageNumber = page,
            pageSize = size,
            totalElements = useCaseResult.totalElements,
            totalPages = totalPages
        )
        val response = InventoryListResponse(
            content = inventoryResponses,
            pageable = pageable
        )

        // 4. 200 OK 응답
        return ResponseEntity.ok(ApiResponse(data = response))
    }

    /**
     * 재고 추가 API
     *
     * POST /api/inventories/{productOptionId}/add
     *
     * @param productOptionId 상품 옵션 ID
     * @param request 재고 추가 요청 (추가할 수량)
     * @return 재고 추가 결과 (상품 옵션 ID, 현재 재고 수량)
     */
    @PostMapping("/{productOptionId}/add")
    @Operation(summary = "재고 추가", description = "상품 옵션의 재고를 추가")
    fun addStock(
        @Parameter(description = "상품 옵션 ID", example = "1")
        @PathVariable productOptionId: Long,
        @Valid @RequestBody request: InventoryAddStockRequest
    ): ResponseEntity<ApiResponse<InventoryAddStockResponse>> {
        // UseCase 계층에 위임
        val result = inventoryAddStockUseCase.addStock(productOptionId, request.quantity)

        // 응답 DTO 생성
        val response = InventoryAddStockResponse(
            productOptionId = result.productOptionId,
            currentStock = result.currentStock
        )

        // 200 OK 응답
        return ResponseEntity.ok(ApiResponse(data = response))
    }
}
