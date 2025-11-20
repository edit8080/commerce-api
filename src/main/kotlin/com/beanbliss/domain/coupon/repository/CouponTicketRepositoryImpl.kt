package com.beanbliss.domain.coupon.repository

import com.beanbliss.domain.coupon.entity.CouponTicketEntity
import com.beanbliss.domain.coupon.entity.CouponTicketStatus
import jakarta.persistence.LockModeType
import jakarta.persistence.QueryHint
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.jpa.repository.QueryHints
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
     * - status = 'AVAILABLE'인 티켓 중 하나를 선점
     * - PESSIMISTIC_WRITE 락으로 동시 접근 제어
     * - 이미 락이 걸린 티켓은 SKIP LOCKED(value='-2')로 다음 티켓 자동 조회
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints(value = [
        QueryHint(name = "jakarta.persistence.lock.timeout", value = "-2")
    ])
    @Query("""
        SELECT ct
        FROM CouponTicketEntity ct
        WHERE ct.couponId = :couponId
        AND ct.status = 'AVAILABLE'
        ORDER BY ct.id ASC
        LIMIT 1
    """)
    fun findAvailableTicketWithLock(@Param("couponId") couponId: Long): CouponTicketEntity?

    /**
     * 티켓을 사용자에게 발급
     * - status: 'AVAILABLE' → 'ISSUED'
     * - userId, userCouponId, issuedAt 설정
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
        UPDATE CouponTicketEntity ct
        SET ct.status = 'ISSUED',
            ct.userId = :userId,
            ct.userCouponId = :userCouponId,
            ct.issuedAt = :issuedAt
        WHERE ct.id = :ticketId
    """)
    fun issueTicketToUser(
        @Param("ticketId") ticketId: Long,
        @Param("userId") userId: Long,
        @Param("userCouponId") userCouponId: Long,
        @Param("issuedAt") issuedAt: java.time.LocalDateTime
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

    override fun findAvailableTicketWithLock(couponId: Long): CouponTicketEntity? {
        return couponTicketJpaRepository.findAvailableTicketWithLock(couponId)
    }

    override fun issueTicketToUser(ticketId: Long, userId: Long, userCouponId: Long) {
        couponTicketJpaRepository.issueTicketToUser(
            ticketId = ticketId,
            userId = userId,
            userCouponId = userCouponId,
            issuedAt = java.time.LocalDateTime.now()
        )
    }

    override fun countAvailableTickets(couponId: Long): Int {
        return couponTicketJpaRepository.countAvailableTickets(couponId).toInt()
    }

    override fun saveAll(tickets: List<CouponTicketEntity>): List<CouponTicketEntity> {
        return couponTicketJpaRepository.saveAll(tickets).toList()
    }
}
