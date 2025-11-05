package com.beanbliss.domain.product.usecase

import com.beanbliss.domain.inventory.service.InventoryService
import com.beanbliss.domain.product.dto.ProductResponse
import com.beanbliss.domain.product.service.ProductService
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

/**
 * 상품 상세 조회 UseCase
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
class GetProductDetailUseCase(
    private val productService: ProductService,
    private val inventoryService: InventoryService
) {

    /**
     * 상품 상세 조회 (옵션 + 가용 재고 포함)
     *
     * @param productId 상품 ID
     * @return 상품 상세 정보 (옵션 포함, 가용 재고 계산됨)
     * @throws ResourceNotFoundException 상품이 존재하지 않거나 활성 옵션이 없는 경우
     */
    @Transactional(readOnly = true)
    fun getProductDetail(productId: Long): ProductResponse {
        // 1. ProductService를 통해 상품 + 옵션 조회 (재고 제외)
        val product = productService.getProductWithOptions(productId)

        // 2. 옵션 ID 목록 추출 (Batch 재고 조회 준비)
        val optionIds = product.options.map { it.optionId }

        // 3. InventoryService를 통해 가용 재고 Batch 조회
        val stockMap = inventoryService.calculateAvailableStockBatch(optionIds)

        // 4. 상품 데이터 + 재고 데이터 결합
        val optionsWithStock = product.options.map { option ->
            option.copy(availableStock = stockMap[option.optionId] ?: 0)
        }

        // 5. 응답 반환
        return product.copy(options = optionsWithStock)
    }
}
