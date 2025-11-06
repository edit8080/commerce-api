package com.beanbliss.domain.inventory.service

import com.beanbliss.domain.cart.dto.CartItemResponse
import com.beanbliss.domain.inventory.dto.InventoryResponse
import com.beanbliss.domain.inventory.entity.InventoryReservationEntity

/**
 * [책임]: 재고 관리 기능의 계약 정의
 * Controller는 이 인터페이스에만 의존합니다 (DIP 준수)
 */
interface InventoryService {
    /**
     * 재고 목록 조회 결과 (도메인 데이터)
     */
    data class InventoriesResult(
        val inventories: List<InventoryResponse>,
        val totalElements: Long
    )

    /**
     * 재고 예약 정보 (도메인 데이터)
     */
    data class ReservationItem(
        val reservationEntity: InventoryReservationEntity,
        val productName: String,
        val optionCode: String,
        val availableStockAfterReservation: Int
    )

    /**
     * 재고 목록 조회
     *
     * [비즈니스 로직]:
     * 1. Repository에서 재고 목록 조회 (created_at DESC 정렬)
     * 2. Repository에서 전체 재고 개수 조회
     *
     * [참고]:
     * - 파라미터 유효성 검증은 Controller에서 Jakarta Validator로 수행됨
     *
     * @param page 페이지 번호 (1부터 시작, Controller에서 검증됨)
     * @param size 페이지 크기 (1~100, Controller에서 검증됨)
     * @return 재고 목록 + 총 개수
     */
    fun getInventories(page: Int, size: Int): InventoriesResult

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

    /**
     * 재고 예약 생성
     *
     * [비즈니스 로직]:
     * 1. 중복 예약 방지: 사용자의 활성 예약 존재 여부 확인
     * 2. 가용 재고 계산 및 충분성 검증
     * 3. 예약 엔티티 생성 (30분 만료)
     *
     * [트랜잭션]:
     * - @Transactional로 원자성 보장
     * - 재고 부족 시 롤백
     *
     * @param userId 사용자 ID
     * @param cartItems 장바구니 아이템 목록 (상품 정보 포함)
     * @return 생성된 재고 예약 정보 목록 (도메인 데이터)
     * @throws DuplicateReservationException 이미 활성 예약이 존재하는 경우
     * @throws InsufficientAvailableStockException 가용 재고가 부족한 경우
     */
    fun reserveInventory(userId: Long, cartItems: List<CartItemResponse>): List<ReservationItem>

    /**
     * 주문을 위한 재고 확인 및 차감 (비관적 락)
     *
     * [비즈니스 로직]:
     * 1. 장바구니 아이템의 모든 상품 옵션 ID 추출
     * 2. 재고 일괄 조회 (비관적 락 - FOR UPDATE)
     * 3. 재고 검증 (수량 확인)
     * 4. 재고 일괄 차감 (Batch Update)
     *
     * [트랜잭션]:
     * - @Transactional로 원자성 보장
     * - 비관적 락으로 동시성 제어
     * - 재고 부족 시 롤백
     *
     * @param cartItems 장바구니 아이템 목록
     * @throws InsufficientStockException 재고가 부족한 경우
     */
    fun reduceStockForOrder(cartItems: List<CartItemResponse>)
}
