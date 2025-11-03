package com.beanbliss.domain.coupon.repository

import com.beanbliss.domain.coupon.entity.CouponTicketEntity
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

@DisplayName("쿠폰 티켓 Repository 테스트")
class CouponTicketRepositoryTest {

    private lateinit var repository: FakeCouponTicketRepository

    @BeforeEach
    fun setUp() {
        repository = FakeCouponTicketRepository()
    }

    @Test
    fun `발급 가능한 티켓 조회 시_AVAILABLE 상태의 티켓을 반환해야 한다`() {
        // Given
        val couponId = 1L
        val now = LocalDateTime.now()

        val ticket1 = CouponTicketEntity(
            id = 1L,
            couponId = couponId,
            status = "AVAILABLE",
            userId = null,
            userCouponId = null,
            issuedAt = null,
            createdAt = now,
            updatedAt = now
        )

        val ticket2 = CouponTicketEntity(
            id = 2L,
            couponId = couponId,
            status = "AVAILABLE",
            userId = null,
            userCouponId = null,
            issuedAt = null,
            createdAt = now,
            updatedAt = now
        )

        repository.addTicket(ticket1)
        repository.addTicket(ticket2)

        // When
        val result = repository.findAvailableTicketWithLock(couponId)

        // Then
        assertNotNull(result)
        assertEquals("AVAILABLE", result?.status)
        assertNull(result?.userId)
        assertNull(result?.userCouponId)
    }

    @Test
    fun `발급 가능한 티켓이 없을 경우_null을 반환해야 한다`() {
        // Given
        val couponId = 1L
        val now = LocalDateTime.now()

        val issuedTicket = CouponTicketEntity(
            id = 1L,
            couponId = couponId,
            status = "ISSUED",
            userId = 100L,
            userCouponId = 1L,
            issuedAt = now,
            createdAt = now,
            updatedAt = now
        )

        repository.addTicket(issuedTicket)

        // When
        val result = repository.findAvailableTicketWithLock(couponId)

        // Then
        assertNull(result)
    }

    @Test
    fun `여러 티켓 중_ID가 가장 작은 티켓을 먼저 선점해야 한다`() {
        // Given
        val couponId = 1L
        val now = LocalDateTime.now()

        val ticket1 = CouponTicketEntity(
            id = 3L,
            couponId = couponId,
            status = "AVAILABLE",
            userId = null,
            userCouponId = null,
            issuedAt = null,
            createdAt = now,
            updatedAt = now
        )

        val ticket2 = CouponTicketEntity(
            id = 1L,
            couponId = couponId,
            status = "AVAILABLE",
            userId = null,
            userCouponId = null,
            issuedAt = null,
            createdAt = now,
            updatedAt = now
        )

        val ticket3 = CouponTicketEntity(
            id = 2L,
            couponId = couponId,
            status = "AVAILABLE",
            userId = null,
            userCouponId = null,
            issuedAt = null,
            createdAt = now,
            updatedAt = now
        )

        repository.addTicket(ticket1)
        repository.addTicket(ticket2)
        repository.addTicket(ticket3)

        // When
        val result = repository.findAvailableTicketWithLock(couponId)

        // Then
        assertNotNull(result)
        assertEquals(1L, result?.id) // ID가 가장 작은 티켓이 선택되어야 함
    }

    @Test
    fun `티켓 상태를 ISSUED로 업데이트 시_userId와 userCouponId가 설정되어야 한다`() {
        // Given
        val couponId = 1L
        val ticketId = 1L
        val userId = 100L
        val userCouponId = 500L
        val now = LocalDateTime.now()

        val ticket = CouponTicketEntity(
            id = ticketId,
            couponId = couponId,
            status = "AVAILABLE",
            userId = null,
            userCouponId = null,
            issuedAt = null,
            createdAt = now,
            updatedAt = now
        )

        repository.addTicket(ticket)

        // When
        repository.updateTicketAsIssued(ticketId, userId, userCouponId)

        // Then
        val updatedTicket = repository.getTicketById(ticketId)
        assertNotNull(updatedTicket)
        assertEquals("ISSUED", updatedTicket?.status)
        assertEquals(userId, updatedTicket?.userId)
        assertEquals(userCouponId, updatedTicket?.userCouponId)
        assertNotNull(updatedTicket?.issuedAt)
    }

    @Test
    fun `존재하지 않는 티켓 업데이트 시_예외가 발생해야 한다`() {
        // Given
        val nonExistentTicketId = 999L
        val userId = 100L
        val userCouponId = 500L

        // When & Then
        assertThrows(IllegalArgumentException::class.java) {
            repository.updateTicketAsIssued(nonExistentTicketId, userId, userCouponId)
        }
    }

    @Test
    fun `특정 쿠폰의 AVAILABLE 티켓 개수를 정확히 조회해야 한다`() {
        // Given
        val couponId = 1L
        val now = LocalDateTime.now()

        val availableTicket1 = CouponTicketEntity(
            id = 1L,
            couponId = couponId,
            status = "AVAILABLE",
            userId = null,
            userCouponId = null,
            issuedAt = null,
            createdAt = now,
            updatedAt = now
        )

        val availableTicket2 = CouponTicketEntity(
            id = 2L,
            couponId = couponId,
            status = "AVAILABLE",
            userId = null,
            userCouponId = null,
            issuedAt = null,
            createdAt = now,
            updatedAt = now
        )

        val issuedTicket = CouponTicketEntity(
            id = 3L,
            couponId = couponId,
            status = "ISSUED",
            userId = 100L,
            userCouponId = 1L,
            issuedAt = now,
            createdAt = now,
            updatedAt = now
        )

        repository.addTicket(availableTicket1)
        repository.addTicket(availableTicket2)
        repository.addTicket(issuedTicket)

        // When
        val count = repository.countAvailableTickets(couponId)

        // Then
        assertEquals(2, count)
    }

    @Test
    fun `다른 쿠폰의 티켓은 조회되지 않아야 한다`() {
        // Given
        val couponId1 = 1L
        val couponId2 = 2L
        val now = LocalDateTime.now()

        val ticket1 = CouponTicketEntity(
            id = 1L,
            couponId = couponId1,
            status = "AVAILABLE",
            userId = null,
            userCouponId = null,
            issuedAt = null,
            createdAt = now,
            updatedAt = now
        )

        val ticket2 = CouponTicketEntity(
            id = 2L,
            couponId = couponId2,
            status = "AVAILABLE",
            userId = null,
            userCouponId = null,
            issuedAt = null,
            createdAt = now,
            updatedAt = now
        )

        repository.addTicket(ticket1)
        repository.addTicket(ticket2)

        // When
        val result = repository.findAvailableTicketWithLock(couponId1)

        // Then
        assertNotNull(result)
        assertEquals(couponId1, result?.couponId)
        assertEquals(1L, result?.id)
    }
}
