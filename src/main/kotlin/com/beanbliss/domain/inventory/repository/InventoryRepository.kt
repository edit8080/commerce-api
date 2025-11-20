package com.beanbliss.domain.inventory.repository

import com.beanbliss.domain.inventory.domain.Inventory

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
     * 상품 옵션 ID로 재고 조회 (비관적 락)
     *
     * [동시성 제어]: FOR UPDATE 쿼리 사용
     * - addStock() 시 Lost Update 방지
     *
     * @param productOptionId 상품 옵션 ID
     * @return 재고 도메인 모델 (없으면 null)
     */
    fun findByProductOptionIdWithLock(productOptionId: Long): Inventory?

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
     * 여러 상품 옵션 ID로 재고 일괄 조회 (Bulk 조회 + 비관적 락)
     *
     * [성능 최적화]:
     * - N+1 문제 방지: WHERE product_option_id IN (...) 사용
     * - 단일 쿼리로 모든 재고 조회
     *
     * [동시성 제어]:
     * - FOR UPDATE 쿼리 사용
     * - ORDER BY product_option_id로 Deadlock 방지
     * - reduceStockForOrder() 시 Lost Update 방지
     *
     * @param productOptionIds 상품 옵션 ID 리스트
     * @return 재고 도메인 모델 리스트
     */
    fun findAllByProductOptionIdsWithLock(productOptionIds: List<Long>): List<Inventory>

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
     * 재고 목록 조회 (INVENTORY 도메인만)
     *
     * [설계 변경]:
     * - PRODUCT_OPTION, PRODUCT와의 JOIN 제거
     * - INVENTORY 테이블만 조회
     * - UseCase에서 PRODUCT 정보와 조합
     *
     * @param page 페이지 번호 (1부터 시작)
     * @param size 페이지 크기
     * @param sortBy 정렬 기준 (예: "product_option_id")
     * @param sortDirection 정렬 방향 ("ASC" 또는 "DESC")
     * @return 재고 목록 (INVENTORY 도메인만)
     */
    fun findAll(
        page: Int,
        size: Int,
        sortBy: String,
        sortDirection: String
    ): List<Inventory>

    /**
     * 전체 재고 개수 조회
     *
     * 페이징 처리를 위한 전체 재고 개수를 반환합니다.
     *
     * @return 전체 재고 개수
     */
    fun count(): Long
}
