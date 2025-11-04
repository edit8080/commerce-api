package com.beanbliss.domain.inventory.repository

import com.beanbliss.domain.inventory.entity.InventoryReservationEntity
import com.beanbliss.domain.inventory.entity.InventoryReservationStatus
import org.springframework.stereotype.Repository
import java.time.LocalDateTime
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

/**
 * [책임]: InventoryReservationRepository의 In-Memory 구현체
 * - 재고 예약 정보 관리
 * - Thread-safe 보장
 *
 * [주의사항]:
 * - 애플리케이션 재시작 시 데이터 소실 (In-memory 특성)
 * - 실제 DB 사용 시 JPA 기반 구현체로 교체 필요
 */
@Repository
class InventoryReservationRepositoryImpl : InventoryReservationRepository {

    private val reservations = ConcurrentHashMap<Long, InventoryReservationEntity>()
    private val currentId = AtomicLong(1L)

    override fun countActiveReservations(userId: Long): Int {
        val now = LocalDateTime.now()
        return reservations.values.count { reservation ->
            reservation.userId == userId &&
            reservation.status in InventoryReservationStatus.activeStatuses() &&
            reservation.expiresAt > now
        }
    }

    override fun sumQuantityByProductOptionIdAndStatus(
        productOptionId: Long,
        statuses: List<InventoryReservationStatus>
    ): Int {
        return reservations.values
            .filter { it.productOptionId == productOptionId && it.status in statuses }
            .sumOf { it.quantity }
    }

    override fun save(reservation: InventoryReservationEntity): InventoryReservationEntity {
        val savedReservation = if (reservation.id == 0L) {
            // 신규 저장: ID 할당
            reservation.copy(id = currentId.getAndIncrement())
        } else {
            // 업데이트: 기존 ID 유지
            reservation
        }

        reservations[savedReservation.id] = savedReservation
        return savedReservation
    }

    override fun findActiveReservationsByUserId(userId: Long): List<InventoryReservationEntity> {
        val now = LocalDateTime.now()
        return reservations.values.filter { reservation ->
            reservation.userId == userId &&
            reservation.status in InventoryReservationStatus.activeStatuses() &&
            reservation.expiresAt > now
        }
    }

    /**
     * 테스트용 헬퍼: 모든 데이터 초기화
     */
    fun clear() {
        reservations.clear()
        currentId.set(1L)
    }

    /**
     * 테스트용 헬퍼: 저장된 예약 목록 조회
     */
    fun findAll(): List<InventoryReservationEntity> {
        return reservations.values.toList()
    }

    /**
     * 테스트용 헬퍼: ID로 예약 조회
     */
    fun findById(id: Long): InventoryReservationEntity? {
        return reservations[id]
    }
}
