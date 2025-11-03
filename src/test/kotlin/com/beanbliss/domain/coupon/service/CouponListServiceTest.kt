package com.beanbliss.domain.coupon.service

import com.beanbliss.domain.coupon.dto.CouponListResponse
import com.beanbliss.domain.coupon.repository.CouponRepository
import com.beanbliss.domain.coupon.repository.CouponWithQuantity
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

/**
 * [책임]: CouponService의 쿠폰 목록 조회 비즈니스 로직 검증
 * - Repository 호출 검증
 * - isIssuable 계산 로직 검증
 * - DTO 변환 검증
 * - 페이징 정보 구성 검증
 */
@DisplayName("쿠폰 목록 조회 Service 테스트")
class CouponListServiceTest {

    private val couponRepository: CouponRepository = mockk()
    private lateinit var couponService: CouponService

    @BeforeEach
    fun setUp() {
        // TODO: CouponServiceImpl 구현 후 초기화
        // couponService = CouponServiceImpl(couponRepository)
    }

    @Test
    @DisplayName("쿠폰 목록 조회 시 Repository의 findAllCoupons와 countAllCoupons를 호출해야 한다")
    fun `쿠폰 목록 조회 시 Repository의 findAllCoupons와 countAllCoupons를 호출해야 한다`() {
        // Given
        val page = 1
        val size = 10
        val now = LocalDateTime.now()

        val mockCoupons = listOf(
            CouponWithQuantity(
                id = 1L,
                name = "테스트 쿠폰",
                discountType = "PERCENTAGE",
                discountValue = 10,
                minOrderAmount = 10000,
                maxDiscountAmount = 5000,
                totalQuantity = 100,
                validFrom = now.minusDays(1),
                validUntil = now.plusDays(30),
                createdAt = now,
                updatedAt = now,
                remainingQuantity = 50
            )
        )

        every { couponRepository.findAllCoupons(page, size, "created_at", "DESC") } returns mockCoupons
        every { couponRepository.countAllCoupons() } returns 1L

        // When
        val result = couponService.getCoupons(page, size)

        // Then
        verify(exactly = 1) { couponRepository.findAllCoupons(page, size, "created_at", "DESC") }
        verify(exactly = 1) { couponRepository.countAllCoupons() }
        assertNotNull(result)
    }

    @Test
    @DisplayName("유효기간 내이고 남은 수량이 있으면 isIssuable은 true여야 한다")
    fun `유효기간 내이고 남은 수량이 있으면 isIssuable은 true여야 한다`() {
        // Given
        val now = LocalDateTime.now()
        val mockCoupons = listOf(
            CouponWithQuantity(
                id = 1L,
                name = "발급 가능한 쿠폰",
                discountType = "PERCENTAGE",
                discountValue = 10,
                minOrderAmount = 10000,
                maxDiscountAmount = 5000,
                totalQuantity = 100,
                validFrom = now.minusDays(1), // 유효 시작: 어제
                validUntil = now.plusDays(30), // 유효 종료: 30일 후
                createdAt = now,
                updatedAt = now,
                remainingQuantity = 50 // 남은 수량: 50개
            )
        )

        every { couponRepository.findAllCoupons(1, 10, "created_at", "DESC") } returns mockCoupons
        every { couponRepository.countAllCoupons() } returns 1L

        // When
        val result = couponService.getCoupons(1, 10)

        // Then
        val coupon = result.data.content[0]
        assertTrue(coupon.isIssuable)
    }

    @Test
    @DisplayName("유효기간이 지났으면 isIssuable은 false여야 한다")
    fun `유효기간이 지났으면 isIssuable은 false여야 한다`() {
        // Given
        val now = LocalDateTime.now()
        val mockCoupons = listOf(
            CouponWithQuantity(
                id = 1L,
                name = "만료된 쿠폰",
                discountType = "PERCENTAGE",
                discountValue = 10,
                minOrderAmount = 10000,
                maxDiscountAmount = 5000,
                totalQuantity = 100,
                validFrom = now.minusDays(60), // 유효 시작: 60일 전
                validUntil = now.minusDays(30), // 유효 종료: 30일 전 (만료됨)
                createdAt = now.minusDays(60),
                updatedAt = now.minusDays(60),
                remainingQuantity = 100 // 남은 수량은 있음
            )
        )

        every { couponRepository.findAllCoupons(1, 10, "created_at", "DESC") } returns mockCoupons
        every { couponRepository.countAllCoupons() } returns 1L

        // When
        val result = couponService.getCoupons(1, 10)

        // Then
        val coupon = result.data.content[0]
        assertFalse(coupon.isIssuable) // 유효기간 지남 → false
    }

    @Test
    @DisplayName("남은 수량이 0이면 isIssuable은 false여야 한다")
    fun `남은 수량이 0이면 isIssuable은 false여야 한다`() {
        // Given
        val now = LocalDateTime.now()
        val mockCoupons = listOf(
            CouponWithQuantity(
                id = 1L,
                name = "소진된 쿠폰",
                discountType = "PERCENTAGE",
                discountValue = 10,
                minOrderAmount = 10000,
                maxDiscountAmount = 5000,
                totalQuantity = 100,
                validFrom = now.minusDays(1), // 유효기간 내
                validUntil = now.plusDays(30), // 유효기간 내
                createdAt = now,
                updatedAt = now,
                remainingQuantity = 0 // 남은 수량: 0개
            )
        )

        every { couponRepository.findAllCoupons(1, 10, "created_at", "DESC") } returns mockCoupons
        every { couponRepository.countAllCoupons() } returns 1L

        // When
        val result = couponService.getCoupons(1, 10)

        // Then
        val coupon = result.data.content[0]
        assertFalse(coupon.isIssuable) // 남은 수량 0 → false
    }

    @Test
    @DisplayName("유효기간 전이면 isIssuable은 false여야 한다")
    fun `유효기간 전이면 isIssuable은 false여야 한다`() {
        // Given
        val now = LocalDateTime.now()
        val mockCoupons = listOf(
            CouponWithQuantity(
                id = 1L,
                name = "아직 시작 안 된 쿠폰",
                discountType = "PERCENTAGE",
                discountValue = 10,
                minOrderAmount = 10000,
                maxDiscountAmount = 5000,
                totalQuantity = 100,
                validFrom = now.plusDays(1), // 유효 시작: 내일 (아직 시작 안 함)
                validUntil = now.plusDays(30), // 유효 종료: 30일 후
                createdAt = now,
                updatedAt = now,
                remainingQuantity = 100
            )
        )

        every { couponRepository.findAllCoupons(1, 10, "created_at", "DESC") } returns mockCoupons
        every { couponRepository.countAllCoupons() } returns 1L

        // When
        val result = couponService.getCoupons(1, 10)

        // Then
        val coupon = result.data.content[0]
        assertFalse(coupon.isIssuable) // 유효기간 전 → false
    }

    @Test
    @DisplayName("쿠폰이 없을 경우 빈 리스트를 반환해야 한다")
    fun `쿠폰이 없을 경우 빈 리스트를 반환해야 한다`() {
        // Given
        every { couponRepository.findAllCoupons(1, 10, "created_at", "DESC") } returns emptyList()
        every { couponRepository.countAllCoupons() } returns 0L

        // When
        val result = couponService.getCoupons(1, 10)

        // Then
        assertTrue(result.data.content.isEmpty())
    }
}
