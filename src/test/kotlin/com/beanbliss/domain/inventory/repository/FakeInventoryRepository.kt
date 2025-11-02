package com.beanbliss.domain.inventory.repository

/**
 * Fake InventoryRepository 구현체 (테스트용)
 *
 * [책임]:
 * 특정 옵션의 가용 재고 수량을 반환
 */
class FakeInventoryRepository : InventoryRepository {

    // 옵션 ID -> 가용 재고 매핑
    private val stockMap = mutableMapOf<Long, Int>()

    /**
     * 테스트 데이터 추가용 헬퍼 메서드
     */
    fun setAvailableStock(productOptionId: Long, availableStock: Int) {
        stockMap[productOptionId] = availableStock
    }

    /**
     * 테스트 데이터 초기화
     */
    fun clear() {
        stockMap.clear()
    }

    override fun calculateAvailableStock(productOptionId: Long): Int {
        // 등록되지 않은 옵션은 재고 0으로 처리
        return stockMap[productOptionId] ?: 0
    }

    override fun calculateAvailableStockBatch(productOptionIds: List<Long>): Map<Long, Int> {
        // 모든 optionId에 대해 재고를 조회하여 Map으로 반환
        return productOptionIds.associateWith { optionId ->
            stockMap[optionId] ?: 0
        }
    }
}
