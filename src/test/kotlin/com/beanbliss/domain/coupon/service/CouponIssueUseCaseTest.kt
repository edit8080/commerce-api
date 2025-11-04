package com.beanbliss.domain.coupon.service

import com.beanbliss.common.exception.ResourceNotFoundException
import com.beanbliss.domain.coupon.dto.IssueCouponResponse
import com.beanbliss.domain.coupon.entity.CouponEntity
import com.beanbliss.domain.coupon.entity.CouponTicketEntity
import com.beanbliss.domain.coupon.entity.UserCouponEntity
import com.beanbliss.domain.coupon.enums.UserCouponStatus
import com.beanbliss.domain.coupon.exception.*
import com.beanbliss.domain.coupon.repository.CouponRepository
import com.beanbliss.domain.coupon.repository.CouponTicketRepository
import com.beanbliss.domain.coupon.repository.UserCouponRepository
import io.mockk.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.LocalDateTime

@DisplayName("쿠폰 발급 UseCase 테스트")
class CouponIssueUseCaseTest {

    private lateinit var couponRepository: CouponRepository
    private lateinit var couponTicketRepository: CouponTicketRepository
    private lateinit var userCouponRepository: UserCouponRepository
    private lateinit var couponIssueUseCase: CouponIssueUseCase

    @BeforeEach
    fun setUp() {
        couponRepository = mockk()
        couponTicketRepository = mockk()
        userCouponRepository = mockk()
        couponIssueUseCase = CouponIssueUseCase(couponRepository, couponTicketRepository, userCouponRepository)
    }

    @Test
    fun `쿠폰 발급 성공 시_정상적으로 UserCoupon이 생성되고 CouponTicket 상태가 ISSUED로 변경되어야 한다`() {
        // Given
        val couponId = 1L
        val userId = 100L
        val now = LocalDateTime.now()

        val validCoupon = CouponEntity(
            id = couponId,
            name = "테스트 쿠폰",
            discountType = "PERCENTAGE",
            discountValue = 10,
            minOrderAmount = 10000,
            maxDiscountAmount = 5000,
            totalQuantity = 100,
            validFrom = now.minusDays(1),
            validUntil = now.plusDays(7),
            createdAt = now.minusDays(2),
            updatedAt = now.minusDays(2)
        )

        val availableTicket = CouponTicketEntity(
            id = 1L,
            couponId = couponId,
            status = "AVAILABLE",
            userId = null,
            userCouponId = null,
            issuedAt = null,
            createdAt = now,
            updatedAt = now
        )

        val savedUserCoupon = UserCouponEntity(
            id = 1L,
            userId = userId,
            couponId = couponId,
            status = UserCouponStatus.ISSUED,
            usedOrderId = null,
            usedAt = null,
            createdAt = now,
            updatedAt = now
        )

        every { couponRepository.findById(couponId) } returns validCoupon
        every { userCouponRepository.existsByUserIdAndCouponId(userId, couponId) } returns false
        every { couponTicketRepository.findAvailableTicketWithLock(couponId) } returns availableTicket
        every { userCouponRepository.save(userId, couponId) } returns savedUserCoupon
        every { couponTicketRepository.updateTicketAsIssued(1L, userId, 1L) } just Runs

        // When
        val response = couponIssueUseCase.issueCoupon(couponId, userId)

        // Then - Repository 메서드 호출 검증
        verify(exactly = 1) { couponRepository.findById(couponId) }
        verify(exactly = 1) { userCouponRepository.existsByUserIdAndCouponId(userId, couponId) }
        verify(exactly = 1) { couponTicketRepository.findAvailableTicketWithLock(couponId) }
        verify(exactly = 1) { couponTicketRepository.updateTicketAsIssued(1L, userId, 1L) }
        verify(exactly = 1) { userCouponRepository.save(userId, couponId) }

        // Then - 응답 데이터 검증
        assertNotNull(response)
        assertEquals(1L, response.userCouponId)
        assertEquals(couponId, response.couponId)
        assertEquals(userId, response.userId)
        assertEquals("테스트 쿠폰", response.couponName)
        assertEquals(UserCouponStatus.ISSUED, response.status)
    }

    @Test
    fun `존재하지 않는 쿠폰 발급 요청 시_ResourceNotFoundException이 발생해야 한다`() {
        // Given
        val couponId = 999L
        val userId = 100L

        every { couponRepository.findById(couponId) } returns null

        // When & Then
        assertThrows<ResourceNotFoundException> {
            couponIssueUseCase.issueCoupon(couponId, userId)
        }

        // 쿠폰이 없으면 티켓 조회 및 발급 로직은 실행되지 않아야 한다
        verify(exactly = 1) { couponRepository.findById(couponId) }
        verify(exactly = 0) { couponTicketRepository.findAvailableTicketWithLock(any()) }
        verify(exactly = 0) { userCouponRepository.save(any(), any()) }
    }

    @Test
    fun `유효기간이 지난 쿠폰 발급 요청 시_IllegalStateException이 발생해야 한다`() {
        // Given
        val couponId = 1L
        val userId = 100L
        val now = LocalDateTime.now()

        val expiredCoupon = CouponEntity(
            id = couponId,
            name = "만료된 쿠폰",
            discountType = "PERCENTAGE",
            discountValue = 10,
            minOrderAmount = 10000,
            maxDiscountAmount = 5000,
            totalQuantity = 100,
            validFrom = now.minusDays(10),
            validUntil = now.minusDays(1), // 어제 만료
            createdAt = now.minusDays(11),
            updatedAt = now.minusDays(11)
        )

        every { couponRepository.findById(couponId) } returns expiredCoupon

        // When & Then
        val exception = assertThrows<CouponExpiredException> {
            couponIssueUseCase.issueCoupon(couponId, userId)
        }

        assertTrue(exception.message?.contains("만료") == true || exception.message?.contains("유효기간") == true)
        verify(exactly = 0) { couponTicketRepository.findAvailableTicketWithLock(any()) }
    }

    @Test
    fun `아직 시작하지 않은 쿠폰 발급 요청 시_IllegalStateException이 발생해야 한다`() {
        // Given
        val couponId = 1L
        val userId = 100L
        val now = LocalDateTime.now()

        val notStartedCoupon = CouponEntity(
            id = couponId,
            name = "미시작 쿠폰",
            discountType = "PERCENTAGE",
            discountValue = 10,
            minOrderAmount = 10000,
            maxDiscountAmount = 5000,
            totalQuantity = 100,
            validFrom = now.plusDays(1), // 내일 시작
            validUntil = now.plusDays(10),
            createdAt = now.minusDays(1),
            updatedAt = now.minusDays(1)
        )

        every { couponRepository.findById(couponId) } returns notStartedCoupon

        // When & Then
        val exception = assertThrows<CouponNotStartedException> {
            couponIssueUseCase.issueCoupon(couponId, userId)
        }

        assertTrue(exception.message?.contains("아직") == true || exception.message?.contains("시작") == true)
        verify(exactly = 0) { couponTicketRepository.findAvailableTicketWithLock(any()) }
    }

    @Test
    fun `이미 발급받은 쿠폰 재발급 요청 시_IllegalStateException이 발생해야 한다`() {
        // Given
        val couponId = 1L
        val userId = 100L
        val now = LocalDateTime.now()

        val validCoupon = CouponEntity(
            id = couponId,
            name = "테스트 쿠폰",
            discountType = "PERCENTAGE",
            discountValue = 10,
            minOrderAmount = 10000,
            maxDiscountAmount = 5000,
            totalQuantity = 100,
            validFrom = now.minusDays(1),
            validUntil = now.plusDays(7),
            createdAt = now.minusDays(2),
            updatedAt = now.minusDays(2)
        )

        every { couponRepository.findById(couponId) } returns validCoupon
        every { userCouponRepository.existsByUserIdAndCouponId(userId, couponId) } returns true

        // When & Then
        val exception = assertThrows<CouponAlreadyIssuedException> {
            couponIssueUseCase.issueCoupon(couponId, userId)
        }

        assertTrue(exception.message?.contains("이미") == true)
        verify(exactly = 0) { couponTicketRepository.findAvailableTicketWithLock(any()) }
        verify(exactly = 0) { userCouponRepository.save(any(), any()) }
    }

    @Test
    fun `발급 가능한 티켓이 없을 때_IllegalStateException이 발생해야 한다`() {
        // Given
        val couponId = 1L
        val userId = 100L
        val now = LocalDateTime.now()

        val validCoupon = CouponEntity(
            id = couponId,
            name = "품절 쿠폰",
            discountType = "PERCENTAGE",
            discountValue = 10,
            minOrderAmount = 10000,
            maxDiscountAmount = 5000,
            totalQuantity = 100,
            validFrom = now.minusDays(1),
            validUntil = now.plusDays(7),
            createdAt = now.minusDays(2),
            updatedAt = now.minusDays(2)
        )

        every { couponRepository.findById(couponId) } returns validCoupon
        every { userCouponRepository.existsByUserIdAndCouponId(userId, couponId) } returns false
        every { couponTicketRepository.findAvailableTicketWithLock(couponId) } returns null

        // When & Then
        val exception = assertThrows<CouponOutOfStockException> {
            couponIssueUseCase.issueCoupon(couponId, userId)
        }

        assertTrue(exception.message?.contains("재고") == true || exception.message?.contains("품절") == true || exception.message?.contains("소진") == true)
        verify(exactly = 0) { userCouponRepository.save(any(), any()) }
    }

    @Test
    fun `쿠폰 발급 시_모든 Repository 메서드가 올바른 순서로 호출되어야 한다`() {
        // Given
        val couponId = 1L
        val userId = 100L
        val now = LocalDateTime.now()

        val validCoupon = CouponEntity(
            id = couponId,
            name = "테스트 쿠폰",
            discountType = "PERCENTAGE",
            discountValue = 10,
            minOrderAmount = 10000,
            maxDiscountAmount = 5000,
            totalQuantity = 100,
            validFrom = now.minusDays(1),
            validUntil = now.plusDays(7),
            createdAt = now.minusDays(2),
            updatedAt = now.minusDays(2)
        )

        val availableTicket = CouponTicketEntity(
            id = 1L,
            couponId = couponId,
            status = "AVAILABLE",
            userId = null,
            userCouponId = null,
            issuedAt = null,
            createdAt = now,
            updatedAt = now
        )

        val savedUserCoupon = UserCouponEntity(
            id = 1L,
            userId = userId,
            couponId = couponId,
            status = UserCouponStatus.ISSUED,
            usedOrderId = null,
            usedAt = null,
            createdAt = now,
            updatedAt = now
        )

        every { couponRepository.findById(couponId) } returns validCoupon
        every { userCouponRepository.existsByUserIdAndCouponId(userId, couponId) } returns false
        every { couponTicketRepository.findAvailableTicketWithLock(couponId) } returns availableTicket
        every { userCouponRepository.save(userId, couponId) } returns savedUserCoupon
        every { couponTicketRepository.updateTicketAsIssued(1L, userId, 1L) } just Runs

        // When
        couponIssueUseCase.issueCoupon(couponId, userId)

        // Then - Repository 메서드가 올바른 순서로 호출되어야 함
        verifyOrder {
            couponRepository.findById(couponId)
            userCouponRepository.existsByUserIdAndCouponId(userId, couponId)
            couponTicketRepository.findAvailableTicketWithLock(couponId)
            userCouponRepository.save(userId, couponId)
            couponTicketRepository.updateTicketAsIssued(1L, userId, 1L)
        }
    }
}
