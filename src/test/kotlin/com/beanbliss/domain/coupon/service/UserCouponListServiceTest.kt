package com.beanbliss.domain.coupon.service

import com.beanbliss.domain.coupon.enums.UserCouponStatus
import com.beanbliss.domain.coupon.repository.UserCouponRepository
import com.beanbliss.domain.coupon.repository.UserCouponWithCoupon
import io.mockk.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

/**
 * [책임]: UserCouponService의 비즈니스 로직 검증
 * - Repository 호출 검증
 * - isAvailable은 Repository에서 계산되므로 Service는 전달만 확인
 * - DTO 변환 검증
 * - 페이징 정보 계산 검증
 */
@DisplayName("사용자 쿠폰 목록 조회 Service 테스트")
class UserCouponListServiceTest {

    private lateinit var userCouponRepository: UserCouponRepository
    private lateinit var userCouponService: UserCouponService

    @BeforeEach
    fun setUp() {
        userCouponRepository = mockk()
        userCouponService = UserCouponService(userCouponRepository)
    }

    @Test
    fun `사용자 쿠폰 목록 조회 성공 시_Repository의 findByUserIdWithPaging과 countByUserId가 호출되어야 한다`() {
        // Given
        val userId = 123L
        val page = 1
        val size = 10
        val now = LocalDateTime.now()

        val mockUserCoupons = listOf(
            UserCouponWithCoupon(
                userCouponId = 1L,
                userId = userId,
                couponId = 1L,
                status = UserCouponStatus.ISSUED,
                usedOrderId = null,
                usedAt = null,
                issuedAt = now.minusHours(2),
                couponName = "테스트 쿠폰",
                discountType = "PERCENTAGE",
                discountValue = 10,
                minOrderAmount = 10000,
                maxDiscountAmount = 5000,
                validFrom = now.minusDays(1),
                validUntil = now.plusDays(30),
                isAvailable = true  // Repository에서 계산된 값
            )
        )

        every { userCouponRepository.findByUserIdWithPaging(userId, page, size, any()) } returns mockUserCoupons
        every { userCouponRepository.countByUserId(userId) } returns 1L

        // When
        val response = userCouponService.getUserCoupons(userId, page, size)

        // Then
        verify(exactly = 1) { userCouponRepository.findByUserIdWithPaging(userId, page, size, any()) }
        verify(exactly = 1) { userCouponRepository.countByUserId(userId) }

        assertNotNull(response)
        assertEquals(1, response.userCoupons.size)
        assertEquals(1L, response.totalCount)
    }

    @Test
    fun `isAvailable 계산_status가 ISSUED이고 유효기간 내이면 true를 반환해야 한다`() {
        // Given
        val userId = 123L
        val now = LocalDateTime.now()

        val mockUserCoupon = UserCouponWithCoupon(
            userCouponId = 1L,
            userId = userId,
            couponId = 1L,
            status = UserCouponStatus.ISSUED,
            usedOrderId = null,
            usedAt = null,
            issuedAt = now.minusHours(2),
            couponName = "사용 가능한 쿠폰",
            discountType = "PERCENTAGE",
            discountValue = 10,
            minOrderAmount = 10000,
            maxDiscountAmount = 5000,
            validFrom = now.minusDays(1),
            validUntil = now.plusDays(30),
            isAvailable = true  // Repository에서 계산된 값
        )

        every { userCouponRepository.findByUserIdWithPaging(userId, 1, 10, any()) } returns listOf(mockUserCoupon)
        every { userCouponRepository.countByUserId(userId) } returns 1L

        // When
        val response = userCouponService.getUserCoupons(userId, 1, 10)

        // Then
        val coupon = response.userCoupons[0]
        assertTrue(coupon.isAvailable, "status가 ISSUED이고 유효기간 내이므로 isAvailable은 true여야 합니다")
    }

    @Test
    fun `isAvailable 계산_status가 USED이면 false를 반환해야 한다`() {
        // Given
        val userId = 123L
        val now = LocalDateTime.now()

        val mockUserCoupon = UserCouponWithCoupon(
            userCouponId = 1L,
            userId = userId,
            couponId = 1L,
            status = UserCouponStatus.USED,
            usedOrderId = 789L,
            usedAt = now.minusDays(1),
            issuedAt = now.minusDays(5),
            couponName = "사용한 쿠폰",
            discountType = "PERCENTAGE",
            discountValue = 10,
            minOrderAmount = 10000,
            maxDiscountAmount = 5000,
            validFrom = now.minusDays(10),
            validUntil = now.plusDays(20),
            isAvailable = false  // Repository에서 계산된 값
        )

        every { userCouponRepository.findByUserIdWithPaging(userId, 1, 10, any()) } returns listOf(mockUserCoupon)
        every { userCouponRepository.countByUserId(userId) } returns 1L

        // When
        val response = userCouponService.getUserCoupons(userId, 1, 10)

        // Then
        val coupon = response.userCoupons[0]
        assertFalse(coupon.isAvailable, "status가 USED이므로 isAvailable은 false여야 합니다")
    }

    @Test
    fun `isAvailable 계산_status가 EXPIRED이면 false를 반환해야 한다`() {
        // Given
        val userId = 123L
        val now = LocalDateTime.now()

        val mockUserCoupon = UserCouponWithCoupon(
            userCouponId = 1L,
            userId = userId,
            couponId = 1L,
            status = UserCouponStatus.EXPIRED,
            usedOrderId = null,
            usedAt = null,
            issuedAt = now.minusDays(100),
            couponName = "만료된 쿠폰",
            discountType = "PERCENTAGE",
            discountValue = 10,
            minOrderAmount = 10000,
            maxDiscountAmount = 5000,
            validFrom = now.minusDays(100),
            validUntil = now.minusDays(10),
            isAvailable = false  // Repository에서 계산된 값
        )

        every { userCouponRepository.findByUserIdWithPaging(userId, 1, 10, any()) } returns listOf(mockUserCoupon)
        every { userCouponRepository.countByUserId(userId) } returns 1L

        // When
        val response = userCouponService.getUserCoupons(userId, 1, 10)

        // Then
        val coupon = response.userCoupons[0]
        assertFalse(coupon.isAvailable, "status가 EXPIRED이므로 isAvailable은 false여야 합니다")
    }

    @Test
    fun `isAvailable 계산_status가 ISSUED이지만 유효기간 전이면 false를 반환해야 한다`() {
        // Given
        val userId = 123L
        val now = LocalDateTime.now()

        val mockUserCoupon = UserCouponWithCoupon(
            userCouponId = 1L,
            userId = userId,
            couponId = 1L,
            status = UserCouponStatus.ISSUED,
            usedOrderId = null,
            usedAt = null,
            issuedAt = now.minusHours(1),
            couponName = "아직 시작 안 한 쿠폰",
            discountType = "PERCENTAGE",
            discountValue = 10,
            minOrderAmount = 10000,
            maxDiscountAmount = 5000,
            validFrom = now.plusDays(1),  // 내일부터
            validUntil = now.plusDays(30),
            isAvailable = false  // Repository에서 계산된 값
        )

        every { userCouponRepository.findByUserIdWithPaging(userId, 1, 10, any()) } returns listOf(mockUserCoupon)
        every { userCouponRepository.countByUserId(userId) } returns 1L

        // When
        val response = userCouponService.getUserCoupons(userId, 1, 10)

        // Then
        val coupon = response.userCoupons[0]
        assertFalse(coupon.isAvailable, "status가 ISSUED이지만 유효기간 전이므로 isAvailable은 false여야 합니다")
    }

    @Test
    fun `isAvailable 계산_status가 ISSUED이지만 유효기간이 지났으면 false를 반환해야 한다`() {
        // Given
        val userId = 123L
        val now = LocalDateTime.now()

        val mockUserCoupon = UserCouponWithCoupon(
            userCouponId = 1L,
            userId = userId,
            couponId = 1L,
            status = UserCouponStatus.ISSUED,
            usedOrderId = null,
            usedAt = null,
            issuedAt = now.minusDays(100),
            couponName = "기한 지난 쿠폰",
            discountType = "PERCENTAGE",
            discountValue = 10,
            minOrderAmount = 10000,
            maxDiscountAmount = 5000,
            validFrom = now.minusDays(100),
            validUntil = now.minusDays(1),  // 어제 만료
            isAvailable = false  // Repository에서 계산된 값
        )

        every { userCouponRepository.findByUserIdWithPaging(userId, 1, 10, any()) } returns listOf(mockUserCoupon)
        every { userCouponRepository.countByUserId(userId) } returns 1L

        // When
        val response = userCouponService.getUserCoupons(userId, 1, 10)

        // Then
        val coupon = response.userCoupons[0]
        assertFalse(coupon.isAvailable, "status가 ISSUED이지만 유효기간이 지났으므로 isAvailable은 false여야 합니다")
    }

    @Test
    fun `빈 목록 조회 시_빈 배열과 totalElements 0을 반환해야 한다`() {
        // Given
        val userId = 123L

        every { userCouponRepository.findByUserIdWithPaging(userId, 1, 10, any()) } returns emptyList()
        every { userCouponRepository.countByUserId(userId) } returns 0L

        // When
        val response = userCouponService.getUserCoupons(userId, 1, 10)

        // Then
        assertTrue(response.userCoupons.isEmpty())
        assertEquals(0L, response.totalCount)
    }

    @Test
    fun `DTO 변환_모든 필드가 올바르게 매핑되어야 한다`() {
        // Given
        val userId = 123L
        val now = LocalDateTime.now()

        val mockUserCoupon = UserCouponWithCoupon(
            userCouponId = 456L,
            userId = userId,
            couponId = 789L,
            status = UserCouponStatus.ISSUED,
            usedOrderId = null,
            usedAt = null,
            issuedAt = now.minusHours(2),
            couponName = "테스트 쿠폰",
            discountType = "FIXED_AMOUNT",
            discountValue = 5000,
            minOrderAmount = 30000,
            maxDiscountAmount = 5000,
            validFrom = now.minusDays(1),
            validUntil = now.plusDays(30),
            isAvailable = true  // Repository에서 계산된 값
        )

        every { userCouponRepository.findByUserIdWithPaging(userId, 1, 10, any()) } returns listOf(mockUserCoupon)
        every { userCouponRepository.countByUserId(userId) } returns 1L

        // When
        val response = userCouponService.getUserCoupons(userId, 1, 10)

        // Then
        val coupon = response.userCoupons[0]
        assertEquals(456L, coupon.userCouponId)
        assertEquals(789L, coupon.couponId)
        assertEquals("테스트 쿠폰", coupon.couponName)
        assertEquals("FIXED_AMOUNT", coupon.discountType)
        assertEquals(5000, coupon.discountValue)
        assertEquals(30000, coupon.minOrderAmount)
        assertEquals(5000, coupon.maxDiscountAmount)
        assertEquals(UserCouponStatus.ISSUED, coupon.status)
        assertNull(coupon.usedOrderId)
        assertNull(coupon.usedAt)
        assertNotNull(coupon.issuedAt)
    }
}
