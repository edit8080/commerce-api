package com.beanbliss.domain.inventory.service

import com.beanbliss.domain.inventory.dto.InventoryListResponse

/**
 * [책임]: 재고 관리 기능의 계약 정의
 * Controller는 이 인터페이스에만 의존합니다 (DIP 준수)
 */
interface InventoryService {
    /**
     * 재고 목록 조회
     *
     * [비즈니스 로직]:
     * 1. 파라미터 유효성 검증 (page >= 1, 1 <= size <= 100)
     * 2. Repository에서 재고 목록 조회 (created_at DESC 정렬)
     * 3. Repository에서 전체 재고 개수 조회
     * 4. 페이지 정보 조립 (totalPages 계산)
     *
     * @param page 페이지 번호 (1부터 시작)
     * @param size 페이지 크기 (1~100)
     * @return 재고 목록 + 페이징 정보
     * @throws InvalidPageNumberException 페이지 번호가 1 미만인 경우
     * @throws InvalidPageSizeException 페이지 크기가 범위를 벗어난 경우
     */
    fun getInventories(page: Int, size: Int): InventoryListResponse

    /**
     * 재고 추가
     *
     * [비즈니스 로직]:
     * 1. Repository에서 재고 조회 (비관적 락)
     * 2. 도메인 모델의 addStock() 메서드 호출 (최대 재고 수량 검증 포함)
     * 3. 변경된 재고 저장
     *
     * @param productOptionId 상품 옵션 ID (SKU 기반)
     * @param quantity 추가할 재고 수량
     * @return 추가 후 현재 재고 수량
     * @throws ResourceNotFoundException 재고 정보를 찾을 수 없는 경우
     * @throws MaxStockExceededException 최대 재고 수량을 초과하는 경우
     */
    fun addStock(productOptionId: Long, quantity: Int): Int

    /**
     * 여러 상품 옵션의 가용 재고를 일괄 조회 (Batch 쿼리)
     *
     * [비즈니스 로직]:
     * 1. Repository에서 모든 옵션의 재고 정보를 한 번의 쿼리로 조회
     * 2. 가용 재고 = 총 재고 - 예약 재고 계산
     * 3. Map<optionId, availableStock> 형태로 반환
     *
     * [성능 최적화]:
     * - N+1 문제 방지: WHERE product_option_id IN (...) 사용
     * - 단일 쿼리로 모든 재고 정보 조회
     *
     * @param optionIds 조회할 상품 옵션 ID 목록
     * @return 옵션 ID를 키로, 가용 재고 수량을 값으로 하는 Map
     */
    fun calculateAvailableStockBatch(optionIds: List<Long>): Map<Long, Int>
}
