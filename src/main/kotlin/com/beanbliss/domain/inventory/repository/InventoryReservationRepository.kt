package com.beanbliss.domain.inventory.repository

import com.beanbliss.domain.inventory.entity.InventoryReservationEntity
import com.beanbliss.domain.inventory.entity.InventoryReservationStatus

/**
 * [책임]: 재고 예약 영속성 계층의 계약 정의
 * UseCase는 이 인터페이스에만 의존합니다 (DIP 준수)
 */
interface InventoryReservationRepository {
    /**
     * 사용자의 활성 예약 개수 조회
     * - status IN ('RESERVED', 'CONFIRMED')
     * - expires_at > NOW()
     *
     * @param userId 사용자 ID
     * @return 활성 예약 개수
     */
    fun countActiveReservations(userId: Long): Int

    /**
     * 상품 옵션별 예약 수량 합계 조회
     * - 특정 상태의 예약 수량 합계
     *
     * @param productOptionId 상품 옵션 ID
     * @param statuses 상태 목록 (예: [RESERVED, CONFIRMED])
     * @return 예약 수량 합계 (예약이 없으면 0)
     */
    fun sumQuantityByProductOptionIdAndStatus(
        productOptionId: Long,
        statuses: List<InventoryReservationStatus>
    ): Int

    /**
     * 재고 예약 저장
     *
     * @param reservation 저장할 예약 정보
     * @return 저장된 InventoryReservationEntity
     */
    fun save(reservation: InventoryReservationEntity): InventoryReservationEntity

    /**
     * 사용자 ID로 활성 예약 목록 조회
     * - status IN ('RESERVED', 'CONFIRMED')
     * - expires_at > NOW()
     *
     * @param userId 사용자 ID
     * @return 활성 예약 목록
     */
    fun findActiveReservationsByUserId(userId: Long): List<InventoryReservationEntity>
}
