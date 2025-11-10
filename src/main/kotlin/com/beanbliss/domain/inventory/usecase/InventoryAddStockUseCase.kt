package com.beanbliss.domain.inventory.usecase

import com.beanbliss.common.exception.ResourceNotFoundException
import com.beanbliss.domain.inventory.service.InventoryService
import com.beanbliss.domain.product.service.ProductOptionService
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

/**
 * 재고 추가 UseCase 결과 DTO
 *
 * @param productOptionId 상품 옵션 ID
 * @param currentStock 추가 후 현재 재고 수량
 */
data class AddStockResult(
    val productOptionId: Long,
    val currentStock: Int
)

/**
 * 재고 추가 UseCase
 *
 * [책임]:
 * - 여러 도메인 서비스(ProductOptionService, InventoryService)를 조율
 * - 트랜잭션 범위 관리
 * - 상품 옵션 검증 후 재고 추가 수행
 *
 * [Facade 패턴]:
 * - 복잡한 비즈니스 흐름을 단순한 인터페이스로 제공
 * - 도메인 서비스들 간의 의존성을 UseCase에서 관리
 */
@Component
class InventoryAddStockUseCase(
    private val productOptionService: ProductOptionService,
    private val inventoryService: InventoryService
) {
    /**
     * 재고 추가
     *
     * [실행 순서]:
     * 1. ProductOptionService를 통해 상품 옵션 존재 여부 검증
     * 2. InventoryService를 통해 재고 추가 수행
     * 3. 결과 반환
     *
     * @param productOptionId 상품 옵션 ID
     * @param quantity 추가할 재고 수량
     * @return 재고 추가 결과 (상품 옵션 ID, 현재 재고 수량)
     * @throws ResourceNotFoundException 상품 옵션이 존재하지 않는 경우
     * @throws MaxStockExceededException 최대 재고 수량을 초과하는 경우
     */
    @Transactional
    fun addStock(productOptionId: Long, quantity: Int): AddStockResult {
        // 1. 상품 옵션 존재 여부 검증
        if (!productOptionService.existsById(productOptionId)) {
            throw ResourceNotFoundException("상품 옵션 ID: $productOptionId 을(를) 찾을 수 없습니다.")
        }

        // 2. 재고 증가 처리
        val currentStock = inventoryService.addStock(productOptionId, quantity)

        // 3. 결과 반환
        return AddStockResult(
            productOptionId = productOptionId,
            currentStock = currentStock
        )
    }
}
