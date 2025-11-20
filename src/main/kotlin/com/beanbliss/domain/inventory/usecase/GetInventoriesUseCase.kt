package com.beanbliss.domain.inventory.usecase

import com.beanbliss.domain.inventory.service.InventoryService
import com.beanbliss.domain.product.service.ProductOptionService
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

/**
 * [책임]: 재고 목록 조회 UseCase (도메인 간 조율)
 *
 * [설계 원칙]:
 * - 여러 도메인 Service 조율
 * - 데이터 조합 및 변환
 * - 복합 도메인 비즈니스 로직
 *
 * [조율하는 Service]:
 * - InventoryService: INVENTORY 도메인
 * - ProductOptionService: PRODUCT 도메인
 *
 * [트랜잭션]:
 * - 읽기 전용 트랜잭션
 */
@Component
class GetInventoriesUseCase(
    private val inventoryService: InventoryService,
    private val productOptionService: ProductOptionService
) {

    /**
     * 재고 목록 조회 결과 (INVENTORY + PRODUCT 조합)
     */
    data class InventoriesUseCaseResult(
        val inventories: List<InventoryDetail>,
        val totalElements: Long
    )

    /**
     * 재고 목록 조회 (상품 정보 포함)
     *
     * [비즈니스 로직]:
     * 1. INVENTORY 도메인에서 재고 목록 조회
     * 2. PRODUCT 도메인에서 상품 옵션 정보 Batch 조회
     * 3. 데이터 조합하여 InventoryDetail 생성
     *
     * [성능 최적화]:
     * - N+1 문제 방지: 상품 옵션 Batch 조회
     *
     * @param page 페이지 번호
     * @param size 페이지 크기
     * @return 재고 목록 (상품 정보 포함) + 전체 개수
     */
    @Transactional(readOnly = true)
    fun getInventories(page: Int, size: Int): InventoriesUseCaseResult {
        // 1. INVENTORY 도메인: 재고 목록 조회
        val serviceResult = inventoryService.getInventories(page, size)
        val inventories = serviceResult.inventories

        // 2. 빈 목록인 경우 조기 반환
        if (inventories.isEmpty()) {
            return InventoriesUseCaseResult(
                inventories = emptyList(),
                totalElements = serviceResult.totalElements
            )
        }

        // 3. PRODUCT 도메인: 상품 옵션 정보 Batch 조회
        val optionIds = inventories.map { it.productOptionId }
        val productOptions = productOptionService.getOptionsBatch(optionIds)

        // 4. 데이터 조합
        val inventoryDetails = inventories.mapNotNull { inventory ->
            val productOption = productOptions[inventory.productOptionId]
                ?: return@mapNotNull null // 옵션 정보가 없으면 제외 (비활성화된 옵션)

            InventoryDetail(
                inventoryId = 0L, // Note: Inventory 도메인 모델에는 ID가 없음
                productId = productOption.productId,
                productName = productOption.productName,
                productOptionId = inventory.productOptionId,
                optionCode = productOption.optionCode,
                optionName = "${productOption.grindType} ${productOption.weightGrams}g",
                price = productOption.price,
                stockQuantity = inventory.stockQuantity,
                createdAt = LocalDateTime.now() // Note: Inventory 도메인 모델에는 createdAt이 없음
            )
        }

        // 5. 결과 반환
        return InventoriesUseCaseResult(
            inventories = inventoryDetails,
            totalElements = serviceResult.totalElements
        )
    }
}
