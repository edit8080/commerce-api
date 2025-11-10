package com.beanbliss.domain.coupon.repository

import com.beanbliss.domain.coupon.entity.CouponTicketEntity
import com.beanbliss.domain.coupon.entity.CouponTicketStatus
import jakarta.persistence.LockModeType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional

/**
 * [책임]: Spring Data JPA를 활용한 CouponTicket 영속성 처리
 * Infrastructure Layer에 속하며, JPA 기술에 종속적
 */
interface CouponTicketJpaRepository : JpaRepository<CouponTicketEntity, Long> {
    /**
     * 발급 가능한 티켓 조회 및 락 설정 (FOR UPDATE SKIP LOCKED)
     * - status = 'AVAILABLE'
     * - userId IS NULL
     * - PESSIMISTIC_WRITE 락 사용
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        SELECT ct
        FROM CouponTicketEntity ct
        WHERE ct.couponId = :couponId
        AND ct.status = 'AVAILABLE'
        AND ct.userId IS NULL
        ORDER BY ct.id ASC
        LIMIT 1
    """)
    fun findAvailableTicketWithLock(@Param("couponId") couponId: Long): CouponTicketEntity?

    /**
     * 티켓 상태 업데이트
     */
    @Modifying
    @Query("""
        UPDATE CouponTicketEntity ct
        SET ct.status = :status,
            ct.userId = :userId,
            ct.userCouponId = :userCouponId,
            ct.issuedAt = CURRENT_TIMESTAMP
        WHERE ct.id = :ticketId
    """)
    fun updateTicketAsIssued(
        @Param("ticketId") ticketId: Long,
        @Param("userId") userId: Long,
        @Param("userCouponId") userCouponId: Long,
        @Param("status") status: CouponTicketStatus
    )

    /**
     * 특정 쿠폰의 AVAILABLE 상태 티켓 개수 조회
     */
    @Query("""
        SELECT COUNT(ct)
        FROM CouponTicketEntity ct
        WHERE ct.couponId = :couponId
        AND ct.status = 'AVAILABLE'
        AND ct.userId IS NULL
    """)
    fun countAvailableTickets(@Param("couponId") couponId: Long): Long

    /**
     * 쿠폰 ID와 상태로 티켓 개수 조회
     */
    @Query("""
        SELECT COUNT(ct)
        FROM CouponTicketEntity ct
        WHERE ct.couponId = :couponId
        AND ct.status = :status
        AND ct.userId IS NULL
    """)
    fun countByCouponIdAndStatus(
        @Param("couponId") couponId: Long,
        @Param("status") status: CouponTicketStatus
    ): Long
}

/**
 * [책임]: CouponTicketRepository 인터페이스 구현체
 * - CouponTicketJpaRepository를 활용하여 실제 DB 접근
 * - FOR UPDATE SKIP LOCKED 시뮬레이션
 */
@Repository
class CouponTicketRepositoryImpl(
    private val couponTicketJpaRepository: CouponTicketJpaRepository
) : CouponTicketRepository {

    @Transactional
    override fun findAvailableTicketWithLock(couponId: Long): CouponTicketEntity? {
        return couponTicketJpaRepository.findAvailableTicketWithLock(couponId)
    }

    @Transactional
    @Modifying
    override fun updateTicketAsIssued(ticketId: Long, userId: Long, userCouponId: Long) {
        couponTicketJpaRepository.updateTicketAsIssued(
            ticketId,
            userId,
            userCouponId,
            CouponTicketStatus.ISSUED
        )
    }

    override fun countAvailableTickets(couponId: Long): Int {
        return couponTicketJpaRepository.countAvailableTickets(couponId).toInt()
    }

    override fun saveAll(ticketList: List<CouponTicketEntity>): List<CouponTicketEntity> {
        return couponTicketJpaRepository.saveAll(ticketList).toList()
    }
}
