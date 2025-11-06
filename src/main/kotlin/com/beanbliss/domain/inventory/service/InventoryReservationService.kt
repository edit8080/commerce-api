package com.beanbliss.domain.inventory.service

import com.beanbliss.domain.cart.dto.CartItemResponse

/**
 * [책임]: 재고 예약 관리 기능의 계약 정의
 * UseCase는 이 인터페이스에만 의존합니다 (DIP 준수)
 */
interface InventoryReservationService {
    /**
     * 재고 예약 검증
     *
     * [비즈니스 로직]:
     * 1. 사용자의 활성 재고 예약 조회
     * 2. 예약 존재 여부 확인
     * 3. 예약 만료 여부 확인 (30분)
     * 4. 예약 수량과 장바구니 수량 일치 확인
     *
     * @param userId 사용자 ID
     * @param cartItems 장바구니 아이템 목록
     * @throws InventoryReservationNotFoundException 재고 예약을 찾을 수 없는 경우
     * @throws InventoryReservationExpiredException 재고 예약이 만료된 경우
     * @throws InventoryReservationMismatchException 예약 수량과 장바구니 수량이 불일치하는 경우
     */
    fun validateReservations(userId: Long, cartItems: List<CartItemResponse>)

    /**
     * 재고 예약 확정 (RESERVED → CONFIRMED)
     *
     * [비즈니스 로직]:
     * 1. 사용자의 활성 재고 예약 조회
     * 2. 예약 상태를 CONFIRMED로 변경 (Bulk Update)
     *
     * @param userId 사용자 ID
     * @param cartItems 장바구니 아이템 목록
     */
    fun confirmReservations(userId: Long, cartItems: List<CartItemResponse>)
}
