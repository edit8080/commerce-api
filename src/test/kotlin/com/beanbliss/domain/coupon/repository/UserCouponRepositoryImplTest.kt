package com.beanbliss.domain.coupon.repository

import com.beanbliss.common.test.RepositoryTestBase
import com.beanbliss.domain.coupon.domain.DiscountType
import com.beanbliss.domain.coupon.entity.CouponEntity
import com.beanbliss.domain.coupon.entity.UserCouponEntity
import com.beanbliss.domain.coupon.enums.UserCouponStatus
import com.beanbliss.domain.user.entity.UserEntity
import jakarta.persistence.EntityManager
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.math.BigDecimal
import java.time.LocalDateTime

@DisplayName("UserCoupon Repository 통합 테스트")
class UserCouponRepositoryImplTest : RepositoryTestBase() {

    @Autowired
    private lateinit var userCouponRepository: UserCouponRepository

    @Autowired
    private lateinit var entityManager: EntityManager

    private lateinit var testUser1: UserEntity
    private lateinit var testUser2: UserEntity
    private lateinit var testCoupon1: CouponEntity
    private lateinit var testCoupon2: CouponEntity

    @BeforeEach
    fun setUpTestData() {
        // 테스트 사용자 생성
        testUser1 = UserEntity(
            email = "user1@example.com",
            password = "password123",
            name = "사용자1"
        )
        testUser2 = UserEntity(
            email = "user2@example.com",
            password = "password123",
            name = "사용자2"
        )
        entityManager.persist(testUser1)
        entityManager.persist(testUser2)

        // 테스트 쿠폰 생성 (유효한 쿠폰)
        testCoupon1 = CouponEntity(
            name = "신규 가입 10% 할인",
            discountType = DiscountType.PERCENTAGE,
            discountValue = BigDecimal("10"),
            minOrderAmount = BigDecimal("20000"),
            maxDiscountAmount = BigDecimal("5000"),
            totalQuantity = 100,
            validFrom = LocalDateTime.now().minusDays(1),
            validUntil = LocalDateTime.now().plusDays(30)
        )
        entityManager.persist(testCoupon1)

        // 테스트 쿠폰 생성 (만료된 쿠폰)
        testCoupon2 = CouponEntity(
            name = "만료된 5000원 할인",
            discountType = DiscountType.FIXED_AMOUNT,
            discountValue = BigDecimal("5000"),
            minOrderAmount = BigDecimal("30000"),
            maxDiscountAmount = BigDecimal("5000"),
            totalQuantity = 50,
            validFrom = LocalDateTime.now().minusDays(10),
            validUntil = LocalDateTime.now().minusDays(1)  // 만료됨
        )
        entityManager.persist(testCoupon2)

        // 사용자1의 쿠폰 발급 (ISSUED 상태, 유효한 쿠폰)
        val userCoupon1 = UserCouponEntity(
            userId = testUser1.id,
            couponId = testCoupon1.id,
            status = UserCouponStatus.ISSUED
        )
        entityManager.persist(userCoupon1)

        // 사용자1의 쿠폰 발급 (ISSUED 상태, 만료된 쿠폰)
        val userCoupon2 = UserCouponEntity(
            userId = testUser1.id,
            couponId = testCoupon2.id,
            status = UserCouponStatus.ISSUED
        )
        entityManager.persist(userCoupon2)

        // 사용자1의 쿠폰 발급 (USED 상태)
        val userCoupon3 = UserCouponEntity(
            userId = testUser1.id,
            couponId = testCoupon1.id,
            status = UserCouponStatus.USED,
            usedOrderId = 1L,
            usedAt = LocalDateTime.now().minusDays(2)
        )
        entityManager.persist(userCoupon3)

        entityManager.flush()
        entityManager.clear()
    }

    @Test
    @DisplayName("사용자가 특정 쿠폰을 이미 발급받았는지 확인 - 발급받은 경우")
    fun `existsByUserIdAndCouponId should return true when user has coupon`() {
        // When
        val exists = userCouponRepository.existsByUserIdAndCouponId(testUser1.id, testCoupon1.id)

        // Then
        assertTrue(exists)
    }

    @Test
    @DisplayName("사용자가 특정 쿠폰을 이미 발급받았는지 확인 - 발급받지 않은 경우")
    fun `existsByUserIdAndCouponId should return false when user does not have coupon`() {
        // When
        val exists = userCouponRepository.existsByUserIdAndCouponId(testUser2.id, testCoupon1.id)

        // Then
        assertFalse(exists)
    }

    @Test
    @DisplayName("사용자 쿠폰 저장 - 신규 발급")
    fun `save should create new user coupon`() {
        // When
        val savedUserCoupon = userCouponRepository.save(testUser2.id, testCoupon1.id)

        // Then
        assertTrue(savedUserCoupon.id > 0)
        assertEquals(testUser2.id, savedUserCoupon.userId)
        assertEquals(testCoupon1.id, savedUserCoupon.couponId)
        assertEquals(UserCouponStatus.ISSUED, savedUserCoupon.status)
        assertNull(savedUserCoupon.usedOrderId)
        assertNull(savedUserCoupon.usedAt)
    }

    @Test
    @DisplayName("사용자 쿠폰 ID로 조회 - 성공")
    fun `findById should return user coupon when exists`() {
        // Given: 저장된 쿠폰 조회
        val allCoupons = userCouponRepository.findAllByUserId(testUser1.id)
        val firstCoupon = allCoupons.first()

        // When
        val userCoupon = userCouponRepository.findById(firstCoupon.id)

        // Then
        assertNotNull(userCoupon)
        assertEquals(firstCoupon.id, userCoupon!!.id)
    }

    @Test
    @DisplayName("사용자 쿠폰 ID로 조회 - 존재하지 않는 경우")
    fun `findById should return null when not exists`() {
        // When
        val userCoupon = userCouponRepository.findById(999L)

        // Then
        assertNull(userCoupon)
    }

    @Test
    @DisplayName("사용자 ID로 모든 쿠폰 조회 - createdAt DESC 정렬")
    fun `findAllByUserId should return all user coupons sorted by createdAt desc`() {
        // When
        val userCoupons = userCouponRepository.findAllByUserId(testUser1.id)

        // Then
        assertEquals(3, userCoupons.size)
        // 최근 생성된 것이 먼저 (USED 상태가 가장 오래됨)
        assertTrue(userCoupons[0].createdAt >= userCoupons[1].createdAt)
        assertTrue(userCoupons[1].createdAt >= userCoupons[2].createdAt)
    }

    @Test
    @DisplayName("사용자 ID로 쿠폰 조회 (페이징, 쿠폰 정보 포함, isAvailable 계산)")
    fun `findByUserIdWithPaging should return user coupons with coupon info and availability`() {
        // When
        val now = LocalDateTime.now()
        val userCoupons = userCouponRepository.findByUserIdWithPaging(
            userId = testUser1.id,
            page = 1,
            size = 10,
            now = now
        )

        // Then
        assertEquals(3, userCoupons.size)

        // 첫 번째 쿠폰 검증 (ISSUED 상태, 유효한 쿠폰)
        val availableCoupon = userCoupons.find {
            it.status == UserCouponStatus.ISSUED && it.couponName.contains("신규 가입")
        }
        assertNotNull(availableCoupon)
        assertTrue(availableCoupon!!.isAvailable)
        assertEquals(testCoupon1.name, availableCoupon.couponName)

        // 두 번째 쿠폰 검증 (ISSUED 상태, 만료된 쿠폰)
        val expiredCoupon = userCoupons.find {
            it.status == UserCouponStatus.ISSUED && it.couponName.contains("만료된")
        }
        assertNotNull(expiredCoupon)
        assertFalse(expiredCoupon!!.isAvailable)  // validUntil이 과거이므로 사용 불가

        // 세 번째 쿠폰 검증 (USED 상태)
        val usedCoupon = userCoupons.find { it.status == UserCouponStatus.USED }
        assertNotNull(usedCoupon)
        assertFalse(usedCoupon!!.isAvailable)  // USED 상태는 사용 불가
        assertEquals(1L, usedCoupon.usedOrderId)
    }

    @Test
    @DisplayName("사용자 ID로 쿠폰 개수 조회")
    fun `countByUserId should return count of user coupons`() {
        // When
        val count = userCouponRepository.countByUserId(testUser1.id)

        // Then
        assertEquals(3, count)
    }

    @Test
    @DisplayName("사용자 ID로 쿠폰 개수 조회 - 0개인 경우")
    fun `countByUserId should return zero when user has no coupons`() {
        // When
        val count = userCouponRepository.countByUserId(testUser2.id)

        // Then
        assertEquals(0, count)
    }

    @Test
    @DisplayName("사용자 쿠폰 조회 - 정렬 순서 검증 (isAvailable DESC, createdAt DESC)")
    fun `findByUserIdWithPaging should sort by availability first then by createdAt`() {
        // When
        val now = LocalDateTime.now()
        val userCoupons = userCouponRepository.findByUserIdWithPaging(
            userId = testUser1.id,
            page = 1,
            size = 10,
            now = now
        )

        // Then: 사용 가능한 쿠폰이 먼저 오고, 그 다음 최신순
        assertEquals(3, userCoupons.size)

        // 첫 번째는 사용 가능한 쿠폰이어야 함
        val firstCoupon = userCoupons.find { it.isAvailable }
        assertNotNull(firstCoupon)
        assertEquals(userCoupons[0].userCouponId, firstCoupon!!.userCouponId)
    }
}
