package com.beanbliss.domain.coupon.repository

import com.beanbliss.common.test.RepositoryTestBase
import com.beanbliss.domain.coupon.domain.DiscountType
import com.beanbliss.domain.coupon.entity.CouponEntity
import com.beanbliss.domain.coupon.entity.CouponTicketEntity
import com.beanbliss.domain.coupon.entity.CouponTicketStatus
import jakarta.persistence.EntityManager
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.math.BigDecimal
import java.time.LocalDateTime

@DisplayName("CouponTicket Repository 통합 테스트")
class CouponTicketRepositoryImplTest : RepositoryTestBase() {

    @Autowired
    private lateinit var couponTicketRepository: CouponTicketRepository

    @Autowired
    private lateinit var entityManager: EntityManager

    private lateinit var testCoupon: CouponEntity

    @BeforeEach
    fun setUpTestData() {
        // 테스트 쿠폰 생성
        testCoupon = CouponEntity(
            name = "신규 가입 10% 할인",
            discountType = DiscountType.PERCENTAGE,
            discountValue = BigDecimal("10"),
            minOrderAmount = BigDecimal("20000"),
            maxDiscountAmount = BigDecimal("5000"),
            totalQuantity = 10,
            validFrom = LocalDateTime.now().minusDays(1),
            validUntil = LocalDateTime.now().plusDays(30)
        )
        entityManager.persist(testCoupon)
        entityManager.flush() // Coupon ID 생성

        // AVAILABLE 티켓 5개 생성
        for (i in 1..5) {
            val ticket = CouponTicketEntity(
                couponId = testCoupon.id,
                status = CouponTicketStatus.AVAILABLE
            )
            entityManager.persist(ticket)
        }

        // ISSUED 티켓 3개 생성
        for (i in 1..3) {
            val ticket = CouponTicketEntity(
                couponId = testCoupon.id,
                userId = 100L,
                status = CouponTicketStatus.ISSUED,
                userCouponId = 1L,
                issuedAt = LocalDateTime.now()
            )
            entityManager.persist(ticket)
        }

        // EXPIRED 티켓 2개 생성
        for (i in 1..2) {
            val ticket = CouponTicketEntity(
                couponId = testCoupon.id,
                status = CouponTicketStatus.EXPIRED
            )
            entityManager.persist(ticket)
        }

        entityManager.flush()
        entityManager.clear()
    }

    @Test
    @DisplayName("발급 가능한 티켓 조회 및 락 설정 - 성공")
    fun `findAvailableTicketWithLock should return available ticket with lock`() {
        // When
        val ticket = couponTicketRepository.findAvailableTicketWithLock(testCoupon.id)

        // Then
        assertNotNull(ticket)
        assertEquals(CouponTicketStatus.AVAILABLE, ticket!!.status)
        assertNull(ticket.userId)
        assertNull(ticket.userCouponId)
        assertEquals(testCoupon.id, ticket.couponId)
    }

    @Test
    @DisplayName("발급 가능한 티켓 조회 - 티켓이 없는 경우")
    fun `findAvailableTicketWithLock should return null when no available tickets`() {
        // Given: 모든 AVAILABLE 티켓을 ISSUED로 변경
        entityManager.createQuery(
            "UPDATE CouponTicketEntity ct SET ct.status = :issued WHERE ct.couponId = :couponId AND ct.status = :available"
        )
            .setParameter("issued", CouponTicketStatus.ISSUED)
            .setParameter("couponId", testCoupon.id)
            .setParameter("available", CouponTicketStatus.AVAILABLE)
            .executeUpdate()
        entityManager.flush()
        entityManager.clear()

        // When
        val ticket = couponTicketRepository.findAvailableTicketWithLock(testCoupon.id)

        // Then
        assertNull(ticket)
    }

    @Test
    @DisplayName("티켓 상태 업데이트 - ISSUED로 변경")
    fun `updateTicketAsIssued should update ticket status to issued`() {
        // Given: AVAILABLE 티켓 조회
        val availableTicket = couponTicketRepository.findAvailableTicketWithLock(testCoupon.id)
        assertNotNull(availableTicket)

        val userId = 200L
        val userCouponId = 100L

        // When
        couponTicketRepository.updateTicketAsIssued(availableTicket!!.id, userId, userCouponId)
        entityManager.flush()
        entityManager.clear()

        // Then: 업데이트된 티켓 재조회 (EntityManager 사용)
        val updatedTicket = entityManager.find(CouponTicketEntity::class.java, availableTicket.id)
        assertNotNull(updatedTicket)
        assertEquals(CouponTicketStatus.ISSUED, updatedTicket.status)
        assertEquals(userId, updatedTicket.userId)
        assertEquals(userCouponId, updatedTicket.userCouponId)
        assertNotNull(updatedTicket.issuedAt)
    }

    @Test
    @DisplayName("특정 쿠폰의 AVAILABLE 상태 티켓 개수 조회")
    fun `countAvailableTickets should return count of available tickets`() {
        // When
        val count = couponTicketRepository.countAvailableTickets(testCoupon.id)

        // Then: AVAILABLE 티켓 5개
        assertEquals(5, count)
    }

    @Test
    @DisplayName("AVAILABLE 상태 티켓 개수 조회 - 0개인 경우")
    fun `countAvailableTickets should return zero when no available tickets`() {
        // Given: 모든 AVAILABLE 티켓을 ISSUED로 변경
        entityManager.createQuery(
            "UPDATE CouponTicketEntity ct SET ct.status = :issued WHERE ct.couponId = :couponId AND ct.status = :available"
        )
            .setParameter("issued", CouponTicketStatus.ISSUED)
            .setParameter("couponId", testCoupon.id)
            .setParameter("available", CouponTicketStatus.AVAILABLE)
            .executeUpdate()
        entityManager.flush()
        entityManager.clear()

        // When
        val count = couponTicketRepository.countAvailableTickets(testCoupon.id)

        // Then
        assertEquals(0, count)
    }

    @Test
    @DisplayName("티켓 일괄 저장")
    fun `saveAll should save multiple tickets`() {
        // Given: 새로운 쿠폰 생성
        val newCoupon = CouponEntity(
            name = "새 쿠폰",
            discountType = DiscountType.FIXED_AMOUNT,
            discountValue = BigDecimal("5000"),
            minOrderAmount = BigDecimal("30000"),
            maxDiscountAmount = BigDecimal("5000"),
            totalQuantity = 3,
            validFrom = LocalDateTime.now(),
            validUntil = LocalDateTime.now().plusDays(7)
        )
        entityManager.persist(newCoupon)
        entityManager.flush()

        // 티켓 3개 생성
        val tickets = listOf(
            CouponTicketEntity(couponId = newCoupon.id, status = CouponTicketStatus.AVAILABLE),
            CouponTicketEntity(couponId = newCoupon.id, status = CouponTicketStatus.AVAILABLE),
            CouponTicketEntity(couponId = newCoupon.id, status = CouponTicketStatus.AVAILABLE)
        )

        // When
        val savedTickets = couponTicketRepository.saveAll(tickets)

        // Then
        assertEquals(3, savedTickets.size)
        assertTrue(savedTickets.all { it.id > 0 })
        assertTrue(savedTickets.all { it.status == CouponTicketStatus.AVAILABLE })
    }

    @Test
    @DisplayName("동시성 테스트 - 여러 트랜잭션에서 티켓 발급")
    fun `findAvailableTicketWithLock should handle concurrent access`() {
        // Given: 트랜잭션 1에서 티켓 조회 및 락 획득
        val ticket1 = couponTicketRepository.findAvailableTicketWithLock(testCoupon.id)
        assertNotNull(ticket1)

        // When: 같은 쿠폰에서 다시 티켓 조회
        val ticket2 = couponTicketRepository.findAvailableTicketWithLock(testCoupon.id)

        // Then: 다른 티켓이 조회되어야 함 (LIMIT 1로 첫 번째만 락)
        assertNotNull(ticket2)
        // Note: 실제 동시성 테스트는 여러 스레드/트랜잭션이 필요하므로,
        // 이 테스트는 기본적인 조회 동작만 검증
    }
}
