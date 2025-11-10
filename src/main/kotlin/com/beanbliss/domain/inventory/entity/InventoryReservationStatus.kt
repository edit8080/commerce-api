package com.beanbliss.domain.inventory.entity

/**
 * 재고 예약 상태
 *
 * [상태 설명]:
 * - RESERVED: 예약됨 (주문창 진입 시)
 * - CONFIRMED: 확정됨 (결제 완료 시)
 * - CANCELLED: 취소됨 (사용자가 주문 취소)
 * - EXPIRED: 만료됨 (30분 초과)
 */
enum class InventoryReservationStatus {
    /**
     * 예약됨 - 주문창 진입 시 재고 예약
     */
    RESERVED,

    /**
     * 확정됨 - 결제 완료 후 예약 확정
     */
    CONFIRMED,

    /**
     * 취소됨 - 사용자가 주문 취소
     */
    CANCELLED,

    /**
     * 만료됨 - 30분 내 결제하지 않아 예약 만료
     */
    EXPIRED;

    companion object {
        /**
         * 활성 상태 목록 (가용 재고 계산 시 차감 대상)
         */
        fun activeStatuses() = listOf(RESERVED, CONFIRMED)
    }
}
