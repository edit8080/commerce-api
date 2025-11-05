package com.beanbliss.domain.product.usecase

import com.beanbliss.common.dto.PageableResponse
import com.beanbliss.common.pagination.PageCalculator
import com.beanbliss.domain.inventory.service.InventoryService
import com.beanbliss.domain.product.dto.ProductListResponse
import com.beanbliss.domain.product.service.ProductService
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

/**
 * 상품 목록 조회 UseCase
 *
 * [책임]:
 * - ProductService와 InventoryService를 오케스트레이션
 * - 상품 데이터와 재고 데이터 병합
 * - 응답 DTO 조립
 *
 * [DIP 준수]:
 * - ProductService, InventoryService 인터페이스에만 의존
 *
 * [SRP 준수]:
 * - UseCase는 오케스트레이션만 담당 (비즈니스 로직 없음)
 * - ProductService: 상품 도메인 로직
 * - InventoryService: 재고 도메인 로직
 */
@Component
class GetProductsUseCase(
    private val productService: ProductService,
    private val inventoryService: InventoryService
) {

    /**
     * 상품 목록 조회 (페이징)
     *
     * @param page 페이지 번호 (1부터 시작)
     * @param size 페이지 크기
     * @return 상품 목록 + 가용 재고 + 페이징 정보
     */
    @Transactional(readOnly = true)
    fun getProducts(page: Int, size: Int): ProductListResponse {
        // 1. ProductService를 통해 활성 상품 조회 (created_at DESC 정렬)
        val products = productService.getActiveProducts(
            page = page,
            size = size,
            sortBy = "created_at",
            sortDirection = "DESC"
        )

        // 2. ProductService를 통해 활성 상품 총 개수 조회
        val totalElements = productService.countActiveProducts()

        // 3. 빈 목록인 경우 조기 반환
        if (products.isEmpty()) {
            val totalPages = PageCalculator.calculateTotalPages(totalElements, size)
            val pageable = PageableResponse(
                pageNumber = page,
                pageSize = size,
                totalElements = totalElements,
                totalPages = totalPages
            )
            return ProductListResponse(
                content = emptyList(),
                pageable = pageable
            )
        }

        // 4. 모든 optionId 수집 (Batch 재고 조회 준비)
        val allOptionIds = products.flatMap { product ->
            product.options.map { option -> option.optionId }
        }

        // 5. InventoryService를 통해 가용 재고 Batch 조회
        val stockMap = inventoryService.calculateAvailableStockBatch(allOptionIds)

        // 6. 상품 데이터 + 재고 데이터 결합
        val productsWithStock = products.map { product ->
            val optionsWithStock = product.options.map { option ->
                option.copy(availableStock = stockMap[option.optionId] ?: 0)
            }
            product.copy(options = optionsWithStock)
        }

        // 7. 페이징 정보 조립
        val totalPages = PageCalculator.calculateTotalPages(totalElements, size)
        val pageable = PageableResponse(
            pageNumber = page,
            pageSize = size,
            totalElements = totalElements,
            totalPages = totalPages
        )

        // 8. 응답 반환
        return ProductListResponse(
            content = productsWithStock,
            pageable = pageable
        )
    }
}
