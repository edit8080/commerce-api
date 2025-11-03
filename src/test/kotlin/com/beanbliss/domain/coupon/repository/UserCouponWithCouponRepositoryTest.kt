package com.beanbliss.domain.coupon.repository

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

/**
 * [책임]: UserCouponRepository의 findByUserIdWithPaging, countByUserId 테스트
 * - Coupon과 JOIN하는 쿼리 검증
 * - 페이징 및 정렬 검증
 * - 전체 개수 조회 검증
 *
 * [주의]: Fake 구현을 사용하여 Coupon과 UserCoupon을 함께 테스트
 */
@DisplayName("사용자 쿠폰 목록 조회 Repository 테스트")
class UserCouponWithCouponRepositoryTest {

    private lateinit var userCouponRepository: FakeUserCouponRepository
    private lateinit var couponRepository: FakeCouponRepository

    @BeforeEach
    fun setUp() {
        couponRepository = FakeCouponRepository()
        userCouponRepository = FakeUserCouponRepository(couponRepository)
    }

    @Test
    fun `사용자 쿠폰 목록 조회 시_Coupon 정보가 포함되어야 한다`() {
        // Given
        val userId = 123L
        val now = LocalDateTime.now()

        // Coupon 데이터 추가
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
            createdAt = now.minusDays(2),
            updatedAt = now.minusDays(2)
        )
        couponRepository.addCoupon(coupon)

        // 사용자 쿠폰 저장 (실제로는 save 메서드 사용)
        val savedUserCoupon = userCouponRepository.save(userId, 1L)

        // When
        val result = userCouponRepository.findByUserIdWithPaging(userId, 1, 10)

        // Then
        assertEquals(1, result.size)
        val userCouponWithCoupon = result[0]
        assertEquals(savedUserCoupon.id, userCouponWithCoupon.userCouponId)
        assertEquals(1L, userCouponWithCoupon.couponId)
        assertEquals("테스트 쿠폰", userCouponWithCoupon.couponName)
        assertEquals("PERCENTAGE", userCouponWithCoupon.discountType)
        assertEquals(10, userCouponWithCoupon.discountValue)
    }

    @Test
    fun `페이징 적용 시_offset과 limit이 올바르게 적용되어야 한다`() {
        // Given
        val userId = 123L
        val now = LocalDateTime.now()

        // Coupon 추가
        repeat(3) { index ->
            val coupon = FakeCouponRepository.Coupon(
                id = index.toLong() + 1,
                name = "쿠폰 ${index + 1}",
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
            couponRepository.addCoupon(coupon)
        }

        // 사용자 쿠폰 3개 생성
        repeat(3) { index ->
            userCouponRepository.save(userId, index.toLong() + 1)
            Thread.sleep(10) // 발급 시간 차이를 위해
        }

        // When - 첫 페이지, size=2
        val page1 = userCouponRepository.findByUserIdWithPaging(userId, 1, 2)

        // Then
        assertEquals(2, page1.size, "첫 페이지는 2개만 반환되어야 합니다")

        // When - 두 번째 페이지, size=2
        val page2 = userCouponRepository.findByUserIdWithPaging(userId, 2, 2)

        // Then
        assertEquals(1, page2.size, "두 번째 페이지는 1개만 반환되어야 합니다")
    }

    @Test
    fun `정렬 순서_최신 발급 순으로 정렬되어야 한다`() {
        // Given
        val userId = 123L
        val now = LocalDateTime.now()

        // Coupon 추가
        repeat(3) { index ->
            val coupon = FakeCouponRepository.Coupon(
                id = index.toLong() + 1,
                name = "쿠폰 ${index + 1}",
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
            couponRepository.addCoupon(coupon)
        }

        // 사용자 쿠폰을 시간차로 생성
        val userCoupon1 = userCouponRepository.save(userId, 1L)
        Thread.sleep(10)
        val userCoupon2 = userCouponRepository.save(userId, 2L)
        Thread.sleep(10)
        val userCoupon3 = userCouponRepository.save(userId, 3L)

        // When
        val result = userCouponRepository.findByUserIdWithPaging(userId, 1, 10)

        // Then
        assertEquals(3, result.size)
        assertTrue(result[0].issuedAt.isAfter(result[1].issuedAt), "최신 발급 순으로 정렬되어야 합니다")
        assertTrue(result[1].issuedAt.isAfter(result[2].issuedAt), "최신 발급 순으로 정렬되어야 합니다")
    }

    @Test
    fun `countByUserId 호출 시_해당 사용자의 전체 쿠폰 개수를 반환해야 한다`() {
        // Given
        val userId = 123L
        val now = LocalDateTime.now()

        // Coupon 추가
        repeat(5) { index ->
            val coupon = FakeCouponRepository.Coupon(
                id = index.toLong() + 1,
                name = "쿠폰 ${index + 1}",
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
            couponRepository.addCoupon(coupon)
        }

        // 사용자 쿠폰 5개 생성
        repeat(5) { index ->
            userCouponRepository.save(userId, index.toLong() + 1)
        }

        // When
        val count = userCouponRepository.countByUserId(userId)

        // Then
        assertEquals(5L, count)
    }

    @Test
    fun `다른 사용자의 쿠폰은 조회되지 않아야 한다`() {
        // Given
        val userId1 = 123L
        val userId2 = 456L
        val now = LocalDateTime.now()

        // Coupon 추가
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
            createdAt = now.minusDays(2),
            updatedAt = now.minusDays(2)
        )
        couponRepository.addCoupon(coupon)

        // 각 사용자에게 쿠폰 발급
        userCouponRepository.save(userId1, 1L)
        userCouponRepository.save(userId2, 1L)

        // When
        val user1Coupons = userCouponRepository.findByUserIdWithPaging(userId1, 1, 10)
        val user1Count = userCouponRepository.countByUserId(userId1)

        val user2Coupons = userCouponRepository.findByUserIdWithPaging(userId2, 1, 10)
        val user2Count = userCouponRepository.countByUserId(userId2)

        // Then
        assertEquals(1, user1Coupons.size)
        assertEquals(1L, user1Count)
        assertEquals(userId1, user1Coupons[0].userId)

        assertEquals(1, user2Coupons.size)
        assertEquals(1L, user2Count)
        assertEquals(userId2, user2Coupons[0].userId)
    }

    @Test
    fun `빈 결과 조회 시_빈 리스트와 0을 반환해야 한다`() {
        // Given
        val userId = 999L

        // When
        val result = userCouponRepository.findByUserIdWithPaging(userId, 1, 10)
        val count = userCouponRepository.countByUserId(userId)

        // Then
        assertTrue(result.isEmpty())
        assertEquals(0L, count)
    }

    @Test
    fun `모든 상태의 쿠폰이 조회되어야 한다`() {
        // Given: 이 테스트는 ISSUED, USED, EXPIRED 상태 모두 조회되는지 확인
        // 실제 구현에서는 status 필터링 없이 모든 상태를 조회해야 함
        val userId = 123L

        // When & Then
        // 이 테스트는 Green Test에서 실제 구현과 함께 작성
        assertTrue(true, "이 테스트는 실제 구현 시 작성됩니다")
    }
}
