package com.beanbliss.domain.coupon.repository

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

/**
 * [책임]: CouponRepository의 쿠폰 목록 조회 기능 검증
 * - 모든 쿠폰 조회 (페이징 적용)
 * - 남은 수량(remainingQuantity) 계산
 * - 전체 쿠폰 개수 조회
 * - 정렬 순서 검증 (created_at DESC)
 */
@DisplayName("쿠폰 Repository 테스트")
class CouponRepositoryTest {

    private lateinit var couponRepository: FakeCouponRepository

    @BeforeEach
    fun setUp() {
        couponRepository = FakeCouponRepository()
    }

    @Test
    @DisplayName("쿠폰 목록 조회 시 모든 쿠폰이 조회되어야 한다")
    fun `쿠폰 목록 조회 시 모든 쿠폰이 조회되어야 한다`() {
        // Given
        val now = LocalDateTime.now()
        val coupon1 = FakeCouponRepository.Coupon(
            id = 1L,
            name = "오픈 기념 쿠폰",
            discountType = "PERCENTAGE",
            discountValue = 10,
            minOrderAmount = 10000,
            maxDiscountAmount = 5000,
            totalQuantity = 100,
            validFrom = now.minusDays(1),
            validUntil = now.plusDays(30),
            createdAt = now.minusDays(2),
            updatedAt = now.minusDays(2)
        )

        val coupon2 = FakeCouponRepository.Coupon(
            id = 2L,
            name = "신규 회원 쿠폰",
            discountType = "FIXED_AMOUNT",
            discountValue = 5000,
            minOrderAmount = 30000,
            maxDiscountAmount = 5000,
            totalQuantity = 500,
            validFrom = now.minusDays(1),
            validUntil = now.plusDays(60),
            createdAt = now.minusDays(1), // 더 최근 생성
            updatedAt = now.minusDays(1)
        )

        couponRepository.addCoupon(coupon1)
        couponRepository.addCoupon(coupon2)

        // When
        val result = couponRepository.findAllCoupons(
            page = 1,
            size = 10,
            sortBy = "created_at",
            sortDirection = "DESC"
        )

        // Then
        assertEquals(2, result.size)
    }

    @Test
    @DisplayName("남은 수량이 올바르게 계산되어야 한다")
    fun `남은 수량이 올바르게 계산되어야 한다`() {
        // Given
        val now = LocalDateTime.now()
        val coupon = FakeCouponRepository.Coupon(
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
            updatedAt = now
        )

        couponRepository.addCoupon(coupon)
        // AVAILABLE 상태 47개
        (1..47).forEach { couponRepository.addTicket(couponId = 1L, status = "AVAILABLE") }
        // ISSUED 상태 53개
        (48..100).forEach { couponRepository.addTicket(couponId = 1L, status = "ISSUED") }

        // When
        val result = couponRepository.findAllCoupons(
            page = 1,
            size = 10,
            sortBy = "created_at",
            sortDirection = "DESC"
        )

        // Then
        val foundCoupon = result.find { it.id == 1L }
        assertNotNull(foundCoupon)
        assertEquals(47, foundCoupon!!.remainingQuantity)
    }

    @Test
    @DisplayName("정렬 필드 및 순서에 맞게 정렬되어야 한다")
    fun `정렬 필드 및 순서에 맞게 정렬되어야 한다`() {
        // Given
        val now = LocalDateTime.now()
        val oldCoupon = FakeCouponRepository.Coupon(
            id = 1L,
            name = "오래된 쿠폰",
            discountType = "PERCENTAGE",
            discountValue = 10,
            minOrderAmount = 10000,
            maxDiscountAmount = 5000,
            totalQuantity = 100,
            validFrom = now.minusDays(10),
            validUntil = now.plusDays(30),
            createdAt = now.minusDays(10),
            updatedAt = now.minusDays(10)
        )

        val newCoupon = FakeCouponRepository.Coupon(
            id = 2L,
            name = "최신 쿠폰",
            discountType = "PERCENTAGE",
            discountValue = 15,
            minOrderAmount = 20000,
            maxDiscountAmount = 10000,
            totalQuantity = 50,
            validFrom = now.minusDays(1),
            validUntil = now.plusDays(60),
            createdAt = now.minusDays(1), // 더 최근
            updatedAt = now.minusDays(1)
        )

        couponRepository.addCoupon(oldCoupon)
        couponRepository.addCoupon(newCoupon)

        // When
        val result = couponRepository.findAllCoupons(
            page = 1,
            size = 10,
            sortBy = "created_at",
            sortDirection = "DESC"
        )

        // Then
        assertEquals(2, result.size)
        assertEquals(2L, result[0].id) // 최신 쿠폰이 먼저
        assertEquals(1L, result[1].id) // 오래된 쿠폰이 나중
    }

    @Test
    @DisplayName("페이징이 올바르게 적용되어야 한다")
    fun `페이징이 올바르게 적용되어야 한다`() {
        // Given
        val now = LocalDateTime.now()
        (1..15).forEach { i ->
            val coupon = FakeCouponRepository.Coupon(
                id = i.toLong(),
                name = "쿠폰 $i",
                discountType = "PERCENTAGE",
                discountValue = 10,
                minOrderAmount = 10000,
                maxDiscountAmount = 5000,
                totalQuantity = 100,
                validFrom = now.minusDays(1),
                validUntil = now.plusDays(30),
                createdAt = now.minusDays(i.toLong()),
                updatedAt = now.minusDays(i.toLong())
            )
            couponRepository.addCoupon(coupon)
        }

        // When - 첫 번째 페이지 (size=10)
        val page1 = couponRepository.findAllCoupons(
            page = 1,
            size = 10,
            sortBy = "created_at",
            sortDirection = "DESC"
        )

        // Then
        assertEquals(10, page1.size)

        // When - 두 번째 페이지 (size=10)
        val page2 = couponRepository.findAllCoupons(
            page = 2,
            size = 10,
            sortBy = "created_at",
            sortDirection = "DESC"
        )

        // Then
        assertEquals(5, page2.size)
    }

    @Test
    @DisplayName("전체 쿠폰 개수를 올바르게 조회해야 한다")
    fun `전체 쿠폰 개수를 올바르게 조회해야 한다`() {
        // Given
        val now = LocalDateTime.now()
        (1..15).forEach { i ->
            val coupon = FakeCouponRepository.Coupon(
                id = i.toLong(),
                name = "쿠폰 $i",
                discountType = "PERCENTAGE",
                discountValue = 10,
                minOrderAmount = 10000,
                maxDiscountAmount = 5000,
                totalQuantity = 100,
                validFrom = now.minusDays(1),
                validUntil = now.plusDays(30),
                createdAt = now.minusDays(i.toLong()),
                updatedAt = now.minusDays(i.toLong())
            )
            couponRepository.addCoupon(coupon)
        }

        // When
        val totalCount = couponRepository.countAllCoupons()

        // Then
        assertEquals(15L, totalCount)
    }

    @Test
    @DisplayName("쿠폰이 없을 경우 빈 리스트를 반환해야 한다")
    fun `쿠폰이 없을 경우 빈 리스트를 반환해야 한다`() {
        // When
        val result = couponRepository.findAllCoupons(
            page = 1,
            size = 10,
            sortBy = "created_at",
            sortDirection = "DESC"
        )

        // Then
        assertTrue(result.isEmpty())
    }
}

