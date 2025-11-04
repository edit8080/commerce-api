package com.beanbliss.domain.inventory.repository

import com.beanbliss.domain.inventory.entity.InventoryReservationEntity
import com.beanbliss.domain.inventory.entity.InventoryReservationStatus
import java.time.LocalDateTime

/**
 * [책임]: InventoryReservationRepository의 In-Memory 테스트 구현체
 * - 테스트에서 실제 DB 없이 Repository 계약을 검증
 * - 단위 테스트의 독립성 보장
 */
class FakeInventoryReservationRepository : InventoryReservationRepository {

    private val reservations = mutableListOf<InventoryReservationEntity>()
    private var currentId = 1L

    override fun countActiveReservations(userId: Long): Int {
        val now = LocalDateTime.now()
        return reservations.count { reservation ->
            reservation.userId == userId &&
            reservation.status in InventoryReservationStatus.activeStatuses() &&
            reservation.expiresAt > now
        }
    }

    override fun sumQuantityByProductOptionIdAndStatus(
        productOptionId: Long,
        statuses: List<InventoryReservationStatus>
    ): Int {
        return reservations
            .filter { it.productOptionId == productOptionId && it.status in statuses }
            .sumOf { it.quantity }
    }

    override fun save(reservation: InventoryReservationEntity): InventoryReservationEntity {
        val savedReservation = if (reservation.id == 0L) {
            // 신규 저장: ID 할당
            reservation.copy(id = currentId++)
        } else {
            // 업데이트: 기존 예약 제거 후 새로운 예약 추가
            reservations.removeIf { it.id == reservation.id }
            reservation
        }

        reservations.add(savedReservation)
        return savedReservation
    }

    override fun findActiveReservationsByUserId(userId: Long): List<InventoryReservationEntity> {
        val now = LocalDateTime.now()
        return reservations.filter { reservation ->
            reservation.userId == userId &&
            reservation.status in InventoryReservationStatus.activeStatuses() &&
            reservation.expiresAt > now
        }
    }

    /**
     * 테스트 헬퍼: 모든 데이터 초기화
     */
    fun clear() {
        reservations.clear()
        currentId = 1L
    }

    /**
     * 테스트 헬퍼: 저장된 예약 목록 조회
     */
    fun findAll(): List<InventoryReservationEntity> {
        return reservations.toList()
    }
}
