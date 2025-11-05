package com.beanbliss.domain.inventory.controller

import com.beanbliss.common.dto.ApiResponse
import com.beanbliss.domain.inventory.dto.InventoryAddStockRequest
import com.beanbliss.domain.inventory.dto.InventoryAddStockResponse
import com.beanbliss.domain.inventory.dto.InventoryListResponse
import com.beanbliss.domain.inventory.service.InventoryService
import com.beanbliss.domain.inventory.usecase.InventoryAddStockUseCase
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

/**
 * 재고 관리 Controller
 *
 * [책임]:
 * - HTTP 요청 수신 및 응답 처리
 * - 요청 파라미터 바인딩
 * - Service 계층 호출 및 결과 반환
 *
 * [DIP 준수]:
 * - InventoryService Interface에만 의존
 */
@RestController
@RequestMapping("/api/inventories")
class InventoryController(
    private val inventoryService: InventoryService,
    private val inventoryAddStockUseCase: InventoryAddStockUseCase
) {

    /**
     * 재고 목록 조회 API
     *
     * GET /api/inventories?page={page}&size={size}
     *
     * @param page 페이지 번호 (기본값: 1)
     * @param size 페이지 크기 (기본값: 10)
     * @return 재고 목록 + 페이징 정보
     */
    @GetMapping
    fun getInventories(
        @RequestParam(defaultValue = "1") page: Int,
        @RequestParam(defaultValue = "10") size: Int
    ): ResponseEntity<ApiResponse<InventoryListResponse>> {
        // Service 계층에 위임
        val result = inventoryService.getInventories(page, size)

        // 200 OK 응답
        return ResponseEntity.ok(ApiResponse(data = result))
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
    fun addStock(
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
