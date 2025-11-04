package com.beanbliss.domain.coupon.repository

import com.beanbliss.domain.coupon.entity.UserCouponEntity
import com.beanbliss.domain.coupon.enums.UserCouponStatus
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

@DisplayName("사용자 쿠폰 Repository 테스트")
class UserCouponRepositoryTest {

    private lateinit var repository: FakeUserCouponRepository

    @BeforeEach
    fun setUp() {
        repository = FakeUserCouponRepository()
    }

    @Test
    fun `사용자가 쿠폰을 발급받지 않았을 경우_false를 반환해야 한다`() {
        // Given
        val userId = 100L
        val couponId = 1L

        // When
        val exists = repository.existsByUserIdAndCouponId(userId, couponId)

        // Then
        assertFalse(exists)
    }

    @Test
    fun `사용자가 쿠폰을 이미 발급받았을 경우_true를 반환해야 한다`() {
        // Given
        val userId = 100L
        val couponId = 1L
        val now = LocalDateTime.now()

        val userCoupon = UserCouponEntity(
            id = 1L,
            userId = userId,
            couponId = couponId,
            status = UserCouponStatus.ISSUED,
            usedOrderId = null,
            usedAt = null,
            createdAt = now,
            updatedAt = now
        )

        repository.addUserCoupon(userCoupon)

        // When
        val exists = repository.existsByUserIdAndCouponId(userId, couponId)

        // Then
        assertTrue(exists)
    }

    @Test
    fun `다른 사용자의 쿠폰은 조회되지 않아야 한다`() {
        // Given
        val userId1 = 100L
        val userId2 = 200L
        val couponId = 1L
        val now = LocalDateTime.now()

        val userCoupon = UserCouponEntity(
            id = 1L,
            userId = userId1,
            couponId = couponId,
            status = UserCouponStatus.ISSUED,
            usedOrderId = null,
            usedAt = null,
            createdAt = now,
            updatedAt = now
        )

        repository.addUserCoupon(userCoupon)

        // When
        val exists = repository.existsByUserIdAndCouponId(userId2, couponId)

        // Then
        assertFalse(exists) // userId2는 발급받지 않았으므로 false
    }

    @Test
    fun `사용자 쿠폰 생성 시_ID가 자동으로 생성되고 상태가 ISSUED여야 한다`() {
        // Given
        val userId = 100L
        val couponId = 1L

        // When
        val savedUserCoupon = repository.save(userId, couponId)

        // Then
        assertNotNull(savedUserCoupon)
        assertTrue(savedUserCoupon.id > 0) // ID가 생성되어야 함
        assertEquals(userId, savedUserCoupon.userId)
        assertEquals(couponId, savedUserCoupon.couponId)
        assertEquals(UserCouponStatus.ISSUED, savedUserCoupon.status)
        assertNull(savedUserCoupon.usedOrderId)
        assertNull(savedUserCoupon.usedAt)
        assertNotNull(savedUserCoupon.createdAt)
        assertNotNull(savedUserCoupon.updatedAt)
    }

    @Test
    fun `여러 사용자 쿠폰 생성 시_ID가 순차적으로 증가해야 한다`() {
        // Given
        val userId = 100L
        val couponId1 = 1L
        val couponId2 = 2L

        // When
        val userCoupon1 = repository.save(userId, couponId1)
        val userCoupon2 = repository.save(userId, couponId2)

        // Then
        assertTrue(userCoupon2.id > userCoupon1.id) // ID가 증가해야 함
    }

    @Test
    fun `사용자 쿠폰 ID로 조회 시_올바른 쿠폰을 반환해야 한다`() {
        // Given
        val userId = 100L
        val couponId = 1L
        val now = LocalDateTime.now()

        val userCoupon = UserCouponEntity(
            id = 1L,
            userId = userId,
            couponId = couponId,
            status = UserCouponStatus.ISSUED,
            usedOrderId = null,
            usedAt = null,
            createdAt = now,
            updatedAt = now
        )

        repository.addUserCoupon(userCoupon)

        // When
        val result = repository.findById(1L)

        // Then
        assertNotNull(result)
        assertEquals(1L, result?.id)
        assertEquals(userId, result?.userId)
        assertEquals(couponId, result?.couponId)
    }

    @Test
    fun `존재하지 않는 사용자 쿠폰 ID로 조회 시_null을 반환해야 한다`() {
        // Given
        val nonExistentId = 999L

        // When
        val result = repository.findById(nonExistentId)

        // Then
        assertNull(result)
    }

    @Test
    fun `특정 사용자의 모든 쿠폰을 조회할 수 있어야 한다`() {
        // Given
        val userId = 100L
        val now = LocalDateTime.now()

        val userCoupon1 = UserCouponEntity(
            id = 1L,
            userId = userId,
            couponId = 1L,
            status = UserCouponStatus.ISSUED,
            usedOrderId = null,
            usedAt = null,
            createdAt = now.minusDays(2),
            updatedAt = now.minusDays(2)
        )

        val userCoupon2 = UserCouponEntity(
            id = 2L,
            userId = userId,
            couponId = 2L,
            status = UserCouponStatus.ISSUED,
            usedOrderId = null,
            usedAt = null,
            createdAt = now.minusDays(1),
            updatedAt = now.minusDays(1)
        )

        val userCoupon3 = UserCouponEntity(
            id = 3L,
            userId = 200L, // 다른 사용자
            couponId = 3L,
            status = UserCouponStatus.ISSUED,
            usedOrderId = null,
            usedAt = null,
            createdAt = now,
            updatedAt = now
        )

        repository.addUserCoupon(userCoupon1)
        repository.addUserCoupon(userCoupon2)
        repository.addUserCoupon(userCoupon3)

        // When
        val result = repository.findAllByUserId(userId)

        // Then
        assertEquals(2, result.size) // userId가 100인 쿠폰만 2개
        assertTrue(result.all { it.userId == userId })
    }

    @Test
    fun `사용자 쿠폰 목록 조회 시_최신 순으로 정렬되어야 한다`() {
        // Given
        val userId = 100L
        val now = LocalDateTime.now()

        val userCoupon1 = UserCouponEntity(
            id = 1L,
            userId = userId,
            couponId = 1L,
            status = UserCouponStatus.ISSUED,
            usedOrderId = null,
            usedAt = null,
            createdAt = now.minusDays(3), // 3일 전
            updatedAt = now.minusDays(3)
        )

        val userCoupon2 = UserCouponEntity(
            id = 2L,
            userId = userId,
            couponId = 2L,
            status = UserCouponStatus.ISSUED,
            usedOrderId = null,
            usedAt = null,
            createdAt = now.minusDays(1), // 1일 전 (최신)
            updatedAt = now.minusDays(1)
        )

        val userCoupon3 = UserCouponEntity(
            id = 3L,
            userId = userId,
            couponId = 3L,
            status = UserCouponStatus.ISSUED,
            usedOrderId = null,
            usedAt = null,
            createdAt = now.minusDays(2), // 2일 전
            updatedAt = now.minusDays(2)
        )

        repository.addUserCoupon(userCoupon1)
        repository.addUserCoupon(userCoupon2)
        repository.addUserCoupon(userCoupon3)

        // When
        val result = repository.findAllByUserId(userId)

        // Then
        assertEquals(3, result.size)
        assertEquals(2L, result[0].id) // 최신 (1일 전)
        assertEquals(3L, result[1].id) // 2일 전
        assertEquals(1L, result[2].id) // 3일 전
    }

    @Test
    fun `사용자 쿠폰이 없을 경우_빈 리스트를 반환해야 한다`() {
        // Given
        val userId = 100L

        // When
        val result = repository.findAllByUserId(userId)

        // Then
        assertTrue(result.isEmpty())
    }
}
