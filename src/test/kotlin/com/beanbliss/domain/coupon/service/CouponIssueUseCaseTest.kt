package com.beanbliss.domain.coupon.service

import com.beanbliss.common.exception.ResourceNotFoundException
import com.beanbliss.domain.coupon.entity.CouponEntity
import com.beanbliss.domain.coupon.entity.CouponTicketEntity
import com.beanbliss.domain.coupon.entity.UserCouponEntity
import com.beanbliss.domain.coupon.enums.UserCouponStatus
import com.beanbliss.domain.coupon.exception.*
import com.beanbliss.domain.user.service.UserService
import io.mockk.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.LocalDateTime

/**
 * CouponIssueUseCase의 멀티 도메인 오케스트레이션 로직을 검증하는 테스트
 *
 * [테스트 목표]:
 * 1. UserService, CouponService, UserCouponService, CouponTicketService를 올바르게 조율하는가?
 * 2. 각 Service 메서드가 순차적으로 수행되는가?
 * 3. 예외 상황에서 적절히 전파하는가?
 *
 * [UseCase의 책임]:
 * - UserService: 사용자 존재 여부 확인
 * - CouponService: 쿠폰 유효성 검증
 * - UserCouponService: 중복 발급 확인 및 사용자 쿠폰 생성
 * - CouponTicketService: 티켓 선점 (FOR UPDATE SKIP LOCKED) 및 발급 (상태 변경 + 사용자 정보 기록)
 */
@DisplayName("쿠폰 발급 UseCase 테스트")
class CouponIssueUseCaseTest {

    private lateinit var userService: UserService
    private lateinit var couponService: CouponService
    private lateinit var userCouponService: UserCouponService
    private lateinit var couponTicketService: CouponTicketService
    private lateinit var couponIssueUseCase: CouponIssueUseCase

    @BeforeEach
    fun setUp() {
        userService = mockk()
        couponService = mockk()
        userCouponService = mockk()
        couponTicketService = mockk()
        couponIssueUseCase = CouponIssueUseCase(userService, couponService, userCouponService, couponTicketService)
    }

    @Test
    @DisplayName("쿠폰 발급 성공 시 모든 Service가 올바르게 조율되어야 한다")
    fun `쿠폰 발급 성공 시_모든 Service가 올바르게 조율되어야 한다`() {
        // Given
        val couponId = 1L
        val userId = 100L
        val now = LocalDateTime.now()

        val mockCouponInfo = CouponService.CouponInfo(
            id = couponId,
            name = "테스트 쿠폰",
            discountType = "PERCENTAGE",
            discountValue = 10,
            minOrderAmount = 10000,
            maxDiscountAmount = 5000,
            totalQuantity = 100,
            validFrom = now.minusDays(1),
            validUntil = now.plusDays(7),
            createdAt = now.minusDays(2)
        )

        val availableTicket = CouponTicketEntity(
            id = 1L,
            couponId = couponId,
            status = com.beanbliss.domain.coupon.entity.CouponTicketStatus.AVAILABLE,
            userId = null,
            userCouponId = null,
            issuedAt = null,
            createdAt = now
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

        // Service Mock 설정
        every { userService.validateUserExists(userId) } just Runs
        every { couponService.getValidCoupon(couponId) } returns mockCouponInfo
        every { userCouponService.validateNotAlreadyIssued(userId, couponId) } just Runs
        every { couponTicketService.findAvailableTicketWithLock(couponId) } returns availableTicket
        every { userCouponService.createUserCoupon(userId, couponId) } returns savedUserCoupon
        every { couponTicketService.issueTicketToUser(1L, userId, 1L) } just Runs

        // When
        val response = couponIssueUseCase.issueCoupon(couponId, userId)

        // Then - Service 메서드 호출 검증
        verify(exactly = 1) { userService.validateUserExists(userId) }
        verify(exactly = 1) { couponService.getValidCoupon(couponId) }
        verify(exactly = 1) { userCouponService.validateNotAlreadyIssued(userId, couponId) }
        verify(exactly = 1) { couponTicketService.findAvailableTicketWithLock(couponId) }
        verify(exactly = 1) { userCouponService.createUserCoupon(userId, couponId) }
        verify(exactly = 1) { couponTicketService.issueTicketToUser(1L, userId, 1L) }

        // Then - 응답 데이터 검증
        assertNotNull(response)
        assertEquals(1L, response.userCouponId)
        assertEquals(couponId, response.couponId)
        assertEquals(userId, response.userId)
        assertEquals("테스트 쿠폰", response.couponName)
        assertEquals(UserCouponStatus.ISSUED, response.status)
    }

    @Test
    @DisplayName("존재하지 않는 사용자 발급 요청 시 ResourceNotFoundException이 전파되어야 한다")
    fun `존재하지 않는 사용자 발급 요청 시_ResourceNotFoundException이 전파되어야 한다`() {
        // Given
        val couponId = 1L
        val userId = 999L

        every { userService.validateUserExists(userId) } throws ResourceNotFoundException("사용자를 찾을 수 없습니다.")

        // When & Then
        assertThrows<ResourceNotFoundException> {
            couponIssueUseCase.issueCoupon(couponId, userId)
        }

        // UserService 호출 후 예외 발생으로 나머지 Service는 호출되지 않아야 함
        verify(exactly = 1) { userService.validateUserExists(userId) }
        verify(exactly = 0) { couponService.getValidCoupon(any()) }
        verify(exactly = 0) { userCouponService.validateNotAlreadyIssued(any(), any()) }
        verify(exactly = 0) { couponTicketService.findAvailableTicketWithLock(any()) }
    }

    @Test
    @DisplayName("존재하지 않는 쿠폰 발급 요청 시 ResourceNotFoundException이 전파되어야 한다")
    fun `존재하지 않는 쿠폰 발급 요청 시_ResourceNotFoundException이 전파되어야 한다`() {
        // Given
        val couponId = 999L
        val userId = 100L

        every { userService.validateUserExists(userId) } just Runs
        every { couponService.getValidCoupon(couponId) } throws ResourceNotFoundException("쿠폰을 찾을 수 없습니다.")

        // When & Then
        assertThrows<ResourceNotFoundException> {
            couponIssueUseCase.issueCoupon(couponId, userId)
        }

        verify(exactly = 1) { userService.validateUserExists(userId) }
        verify(exactly = 1) { couponService.getValidCoupon(couponId) }
        verify(exactly = 0) { couponTicketService.findAvailableTicketWithLock(any()) }
    }

    @Test
    @DisplayName("유효기간이 지난 쿠폰 발급 요청 시 CouponExpiredException이 전파되어야 한다")
    fun `유효기간이 지난 쿠폰 발급 요청 시_CouponExpiredException이 전파되어야 한다`() {
        // Given
        val couponId = 1L
        val userId = 100L

        every { userService.validateUserExists(userId) } just Runs
        every { couponService.getValidCoupon(couponId) } throws CouponExpiredException("유효기간이 만료된 쿠폰입니다.")

        // When & Then
        val exception = assertThrows<CouponExpiredException> {
            couponIssueUseCase.issueCoupon(couponId, userId)
        }

        assertTrue(exception.message?.contains("만료") == true || exception.message?.contains("유효기간") == true)
        verify(exactly = 0) { couponTicketService.findAvailableTicketWithLock(any()) }
    }

    @Test
    @DisplayName("아직 시작하지 않은 쿠폰 발급 요청 시 CouponNotStartedException이 전파되어야 한다")
    fun `아직 시작하지 않은 쿠폰 발급 요청 시_CouponNotStartedException이 전파되어야 한다`() {
        // Given
        val couponId = 1L
        val userId = 100L

        every { userService.validateUserExists(userId) } just Runs
        every { couponService.getValidCoupon(couponId) } throws CouponNotStartedException("아직 사용할 수 없는 쿠폰입니다.")

        // When & Then
        val exception = assertThrows<CouponNotStartedException> {
            couponIssueUseCase.issueCoupon(couponId, userId)
        }

        assertTrue(exception.message?.contains("아직") == true || exception.message?.contains("시작") == true)
        verify(exactly = 0) { couponTicketService.findAvailableTicketWithLock(any()) }
    }

    @Test
    @DisplayName("이미 발급받은 쿠폰 재발급 요청 시 CouponAlreadyIssuedException이 전파되어야 한다")
    fun `이미 발급받은 쿠폰 재발급 요청 시_CouponAlreadyIssuedException이 전파되어야 한다`() {
        // Given
        val couponId = 1L
        val userId = 100L
        val now = LocalDateTime.now()

        val mockCouponInfo = CouponService.CouponInfo(
            id = couponId,
            name = "테스트 쿠폰",
            discountType = "PERCENTAGE",
            discountValue = 10,
            minOrderAmount = 10000,
            maxDiscountAmount = 5000,
            totalQuantity = 100,
            validFrom = now.minusDays(1),
            validUntil = now.plusDays(7),
            createdAt = now.minusDays(2)
        )

        every { userService.validateUserExists(userId) } just Runs
        every { couponService.getValidCoupon(couponId) } returns mockCouponInfo
        every { userCouponService.validateNotAlreadyIssued(userId, couponId) } throws CouponAlreadyIssuedException("이미 발급받은 쿠폰입니다.")

        // When & Then
        val exception = assertThrows<CouponAlreadyIssuedException> {
            couponIssueUseCase.issueCoupon(couponId, userId)
        }

        assertTrue(exception.message?.contains("이미") == true)
        verify(exactly = 0) { couponTicketService.findAvailableTicketWithLock(any()) }
        verify(exactly = 0) { userCouponService.createUserCoupon(any(), any()) }
    }

    @Test
    @DisplayName("발급 가능한 티켓이 없을 때 CouponOutOfStockException이 전파되어야 한다")
    fun `발급 가능한 티켓이 없을 때_CouponOutOfStockException이 전파되어야 한다`() {
        // Given
        val couponId = 1L
        val userId = 100L
        val now = LocalDateTime.now()

        val mockCouponInfo = CouponService.CouponInfo(
            id = couponId,
            name = "품절 쿠폰",
            discountType = "PERCENTAGE",
            discountValue = 10,
            minOrderAmount = 10000,
            maxDiscountAmount = 5000,
            totalQuantity = 100,
            validFrom = now.minusDays(1),
            validUntil = now.plusDays(7),
            createdAt = now.minusDays(2)
        )

        every { userService.validateUserExists(userId) } just Runs
        every { couponService.getValidCoupon(couponId) } returns mockCouponInfo
        every { userCouponService.validateNotAlreadyIssued(userId, couponId) } just Runs
        every { couponTicketService.findAvailableTicketWithLock(couponId) } throws CouponOutOfStockException("쿠폰 재고가 부족합니다.")

        // When & Then
        val exception = assertThrows<CouponOutOfStockException> {
            couponIssueUseCase.issueCoupon(couponId, userId)
        }

        assertTrue(exception.message?.contains("재고") == true || exception.message?.contains("품절") == true || exception.message?.contains("소진") == true)
        verify(exactly = 0) { userCouponService.createUserCoupon(any(), any()) }
    }

    @Test
    @DisplayName("쿠폰 발급 시 모든 Service 메서드가 올바른 순서로 호출되어야 한다")
    fun `쿠폰 발급 시_모든 Service 메서드가 올바른 순서로 호출되어야 한다`() {
        // Given
        val couponId = 1L
        val userId = 100L
        val now = LocalDateTime.now()

        val mockCouponInfo = CouponService.CouponInfo(
            id = couponId,
            name = "테스트 쿠폰",
            discountType = "PERCENTAGE",
            discountValue = 10,
            minOrderAmount = 10000,
            maxDiscountAmount = 5000,
            totalQuantity = 100,
            validFrom = now.minusDays(1),
            validUntil = now.plusDays(7),
            createdAt = now.minusDays(2)
        )

        val availableTicket = CouponTicketEntity(
            id = 1L,
            couponId = couponId,
            status = com.beanbliss.domain.coupon.entity.CouponTicketStatus.AVAILABLE,
            userId = null,
            userCouponId = null,
            issuedAt = null,
            createdAt = now
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

        every { userService.validateUserExists(userId) } just Runs
        every { couponService.getValidCoupon(couponId) } returns mockCouponInfo
        every { userCouponService.validateNotAlreadyIssued(userId, couponId) } just Runs
        every { couponTicketService.findAvailableTicketWithLock(couponId) } returns availableTicket
        every { userCouponService.createUserCoupon(userId, couponId) } returns savedUserCoupon
        every { couponTicketService.issueTicketToUser(1L, userId, 1L) } just Runs

        // When
        couponIssueUseCase.issueCoupon(couponId, userId)

        // Then - Service 메서드가 올바른 순서로 호출되어야 함
        verifyOrder {
            userService.validateUserExists(userId)
            couponService.getValidCoupon(couponId)
            userCouponService.validateNotAlreadyIssued(userId, couponId)
            couponTicketService.findAvailableTicketWithLock(couponId)
            userCouponService.createUserCoupon(userId, couponId)
            couponTicketService.issueTicketToUser(1L, userId, 1L)
        }
    }
}
