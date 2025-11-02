package com.beanbliss.domain.inventory.repository

/**
 * [책임]: 재고 영속성 계층의 계약 정의
 * Service는 이 인터페이스에만 의존합니다 (DIP 준수)
 */
interface InventoryRepository {
    /**
     * 특정 상품 옵션의 가용 재고 계산
     *
     * 계산식: INVENTORY.stock_quantity - SUM(INVENTORY_RESERVATION.quantity WHERE status = 'RESERVED')
     *
     * @param productOptionId 상품 옵션 ID
     * @return 가용 재고 수량
     */
    fun calculateAvailableStock(productOptionId: Long): Int

    /**
     * 여러 상품 옵션의 가용 재고를 한 번에 계산 (Batch 조회)
     *
     * N+1 문제를 방지하기 위해 여러 optionId를 한 번의 쿼리로 조회합니다.
     * 계산식: INVENTORY.stock_quantity - SUM(INVENTORY_RESERVATION.quantity WHERE status = 'RESERVED')
     *
     * @param productOptionIds 상품 옵션 ID 리스트
     * @return Map<옵션ID, 가용 재고 수량>
     */
    fun calculateAvailableStockBatch(productOptionIds: List<Long>): Map<Long, Int>
}
