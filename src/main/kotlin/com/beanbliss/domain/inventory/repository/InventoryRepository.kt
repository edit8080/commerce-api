package com.beanbliss.domain.inventory.repository

import com.beanbliss.domain.inventory.domain.Inventory
import com.beanbliss.domain.inventory.dto.InventoryResponse

/**
 * [책임]: 재고 영속성 계층의 계약 정의
 * Service는 이 인터페이스에만 의존합니다 (DIP 준수)
 */
interface InventoryRepository {
    /**
     * 상품 옵션 ID로 재고 조회
     *
     * @param productOptionId 상품 옵션 ID
     * @return 재고 도메인 모델 (없으면 null)
     */
    fun findByProductOptionId(productOptionId: Long): Inventory?

    /**
     * 여러 상품 옵션 ID로 재고 일괄 조회 (Bulk 조회)
     *
     * [성능 최적화]:
     * - N+1 문제 방지: WHERE product_option_id IN (...) 사용
     * - 단일 쿼리로 모든 재고 조회
     *
     * @param productOptionIds 상품 옵션 ID 리스트
     * @return 재고 도메인 모델 리스트
     */
    fun findAllByProductOptionIds(productOptionIds: List<Long>): List<Inventory>

    /**
     * 재고 저장 (생성 또는 수정)
     *
     * @param inventory 재고 도메인 모델
     * @return 저장된 재고 도메인 모델
     */
    fun save(inventory: Inventory): Inventory

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

    /**
     * 재고 목록 조회 (상품 정보 포함)
     *
     * INVENTORY + PRODUCT_OPTION + PRODUCT를 JOIN하여 조회합니다.
     * N+1 문제를 방지하기 위해 단일 쿼리로 처리합니다.
     *
     * @param page 페이지 번호 (1부터 시작)
     * @param size 페이지 크기
     * @param sortBy 정렬 기준 (예: "created_at")
     * @param sortDirection 정렬 방향 ("ASC" 또는 "DESC")
     * @return 재고 목록
     */
    fun findAllWithProductInfo(
        page: Int,
        size: Int,
        sortBy: String,
        sortDirection: String
    ): List<InventoryResponse>

    /**
     * 전체 재고 개수 조회
     *
     * 페이징 처리를 위한 전체 재고 개수를 반환합니다.
     *
     * @return 전체 재고 개수
     */
    fun count(): Long
}
