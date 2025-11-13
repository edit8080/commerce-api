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
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
        UPDATE CouponTicketEntity ct
        SET ct.status = :status,
            ct.userId = :userId,
            ct.userCouponId = :userCouponId,
            ct.issuedAt = :issuedAt
        WHERE ct.id = :ticketId
    """)
    fun updateTicketAsIssued(
        @Param("ticketId") ticketId: Long,
        @Param("userId") userId: Long,
        @Param("userCouponId") userCouponId: Long,
        @Param("status") status: CouponTicketStatus,
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

    @Transactional
    override fun findAvailableTicketWithLock(couponId: Long): CouponTicketEntity? {
        return couponTicketJpaRepository.findAvailableTicketWithLock(couponId)
    }

    @Transactional
    override fun updateTicketAsIssued(ticketId: Long, userId: Long, userCouponId: Long) {
        // getReferenceById를 사용하여 프록시 대신 실제 엔티티 가져오기
        val ticket = couponTicketJpaRepository.getReferenceById(ticketId)

        // 엔티티 필드 직접 수정 (JPA dirty checking)
        ticket.userId = userId
        ticket.userCouponId = userCouponId
        ticket.status = CouponTicketStatus.ISSUED
        ticket.issuedAt = java.time.LocalDateTime.now()

        // save() 호출 불필요 - JPA가 자동으로 UPDATE 수행
        couponTicketJpaRepository.flush()  // 명시적 flush로 즉시 DB 반영
    }

    override fun countAvailableTickets(couponId: Long): Int {
        return couponTicketJpaRepository.countAvailableTickets(couponId).toInt()
    }

    override fun saveAll(tickets: List<CouponTicketEntity>): List<CouponTicketEntity> {
        return couponTicketJpaRepository.saveAll(tickets).toList()
    }
}
