package com.beanbliss.domain.inventory.repository

import com.beanbliss.domain.inventory.entity.InventoryReservationEntity
import com.beanbliss.domain.inventory.entity.InventoryReservationStatus
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

/**
 * [책임]: Spring Data JPA를 활용한 InventoryReservation 영속성 처리
 * Infrastructure Layer에 속하며, JPA 기술에 종속적
 */
interface InventoryReservationJpaRepository : JpaRepository<InventoryReservationEntity, Long> {
    /**
     * 사용자의 활성 예약 개수 조회
     * - status IN ('RESERVED', 'CONFIRMED')
     * - expires_at > NOW()
     */
    @Query("""
        SELECT COUNT(ir)
        FROM InventoryReservationEntity ir
        WHERE ir.userId = :userId
        AND ir.status IN :statuses
        AND ir.expiresAt > :now
    """)
    fun countActiveReservations(
        @Param("userId") userId: Long,
        @Param("statuses") statuses: List<InventoryReservationStatus>,
        @Param("now") now: LocalDateTime
    ): Long

    /**
     * 상품 옵션별 예약 수량 합계 조회
     */
    @Query("""
        SELECT COALESCE(SUM(ir.quantity), 0)
        FROM InventoryReservationEntity ir
        WHERE ir.productOptionId = :productOptionId
        AND ir.status IN :statuses
    """)
    fun sumQuantityByProductOptionIdAndStatus(
        @Param("productOptionId") productOptionId: Long,
        @Param("statuses") statuses: List<InventoryReservationStatus>
    ): Int

    /**
     * 사용자 ID로 활성 예약 목록 조회
     * - status IN ('RESERVED', 'CONFIRMED')
     * - expires_at > NOW()
     */
    @Query("""
        SELECT ir
        FROM InventoryReservationEntity ir
        WHERE ir.userId = :userId
        AND ir.status IN :statuses
        AND ir.expiresAt > :now
    """)
    fun findActiveReservationsByUserId(
        @Param("userId") userId: Long,
        @Param("statuses") statuses: List<InventoryReservationStatus>,
        @Param("now") now: LocalDateTime
    ): List<InventoryReservationEntity>
}

/**
 * [책임]: InventoryReservationRepository 인터페이스 구현체
 * - InventoryReservationJpaRepository를 활용하여 실제 DB 접근
 */
@Repository
class InventoryReservationRepositoryImpl(
    private val inventoryReservationJpaRepository: InventoryReservationJpaRepository
) : InventoryReservationRepository {

    override fun countActiveReservations(userId: Long): Int {
        val now = LocalDateTime.now()
        return inventoryReservationJpaRepository.countActiveReservations(
            userId,
            InventoryReservationStatus.activeStatuses(),
            now
        ).toInt()
    }

    override fun sumQuantityByProductOptionIdAndStatus(
        productOptionId: Long,
        statuses: List<InventoryReservationStatus>
    ): Int {
        return inventoryReservationJpaRepository.sumQuantityByProductOptionIdAndStatus(
            productOptionId,
            statuses
        )
    }

    override fun save(reservation: InventoryReservationEntity): InventoryReservationEntity {
        return inventoryReservationJpaRepository.save(reservation)
    }

    override fun saveAll(reservations: List<InventoryReservationEntity>): List<InventoryReservationEntity> {
        return inventoryReservationJpaRepository.saveAll(reservations).toList()
    }

    override fun findActiveReservationsByUserId(userId: Long): List<InventoryReservationEntity> {
        val now = LocalDateTime.now()
        return inventoryReservationJpaRepository.findActiveReservationsByUserId(
            userId,
            InventoryReservationStatus.activeStatuses(),
            now
        )
    }
}
