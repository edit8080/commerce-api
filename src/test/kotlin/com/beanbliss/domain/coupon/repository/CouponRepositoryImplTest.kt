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

@DisplayName("Coupon Repository 통합 테스트")
class CouponRepositoryImplTest : RepositoryTestBase() {

    @Autowired
    private lateinit var couponRepository: CouponRepository

    @Autowired
    private lateinit var entityManager: EntityManager

    private lateinit var testCoupon1: CouponEntity
    private lateinit var testCoupon2: CouponEntity

    @BeforeEach
    fun setUpTestData() {
        // 테스트 쿠폰 1 생성 (정률 할인)
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

        // 쿠폰 1의 티켓 생성 (AVAILABLE 80개, ISSUED 20개)
        for (i in 1..80) {
            val ticket = CouponTicketEntity(
                couponId = testCoupon1.id,
                status = CouponTicketStatus.AVAILABLE
            )
            entityManager.persist(ticket)
        }
        for (i in 1..20) {
            val ticket = CouponTicketEntity(
                couponId = testCoupon1.id,
                userId = 1L,
                status = CouponTicketStatus.ISSUED,
                issuedAt = LocalDateTime.now()
            )
            entityManager.persist(ticket)
        }

        // 테스트 쿠폰 2 생성 (정액 할인)
        testCoupon2 = CouponEntity(
            name = "5000원 할인 쿠폰",
            discountType = DiscountType.FIXED_AMOUNT,
            discountValue = BigDecimal("5000"),
            minOrderAmount = BigDecimal("30000"),
            maxDiscountAmount = BigDecimal("5000"),
            totalQuantity = 50,
            validFrom = LocalDateTime.now().minusDays(1),
            validUntil = LocalDateTime.now().plusDays(15)
        )
        entityManager.persist(testCoupon2)

        // 쿠폰 2의 티켓 생성 (AVAILABLE 50개)
        for (i in 1..50) {
            val ticket = CouponTicketEntity(
                couponId = testCoupon2.id,
                status = CouponTicketStatus.AVAILABLE
            )
            entityManager.persist(ticket)
        }

        entityManager.flush()
        entityManager.clear()
    }

    @Test
    @DisplayName("쿠폰 목록 조회 (남은 수량 포함) - 페이징 및 정렬")
    fun `findAllCoupons should return coupons with remaining quantity`() {
        // When
        val coupons = couponRepository.findAllCoupons(
            page = 1,
            size = 10,
            sortBy = "createdAt",
            sortDirection = "DESC"
        )

        // Then
        assertEquals(2, coupons.size)

        // 첫 번째 쿠폰 (최근 생성된 것)
        val coupon1 = coupons[0]
        assertEquals(testCoupon2.name, coupon1.name)
        assertEquals(50, coupon1.remainingQuantity)  // 모두 AVAILABLE

        // 두 번째 쿠폰
        val coupon2 = coupons[1]
        assertEquals(testCoupon1.name, coupon2.name)
        assertEquals(80, coupon2.remainingQuantity)  // 80개 AVAILABLE (20개는 ISSUED)
    }

    @Test
    @DisplayName("쿠폰 목록 조회 - 정렬 옵션 테스트 (name ASC)")
    fun `findAllCoupons should sort by name ascending`() {
        // When
        val coupons = couponRepository.findAllCoupons(
            page = 1,
            size = 10,
            sortBy = "name",
            sortDirection = "ASC"
        )

        // Then
        assertEquals(2, coupons.size)
        assertEquals("5000원 할인 쿠폰", coupons[0].name)
        assertEquals("신규 가입 10% 할인", coupons[1].name)
    }

    @Test
    @DisplayName("전체 쿠폰 개수 조회")
    fun `countAllCoupons should return total coupon count`() {
        // When
        val count = couponRepository.countAllCoupons()

        // Then
        assertEquals(2, count)
    }

    @Test
    @DisplayName("쿠폰 ID로 조회 - 성공")
    fun `findById should return coupon when exists`() {
        // When
        val coupon = couponRepository.findById(testCoupon1.id)

        // Then
        assertNotNull(coupon)
        assertEquals(testCoupon1.name, coupon!!.name)
        assertEquals(DiscountType.PERCENTAGE, coupon.discountType)
        assertTrue(BigDecimal("10").compareTo(coupon.discountValue) == 0)
        assertEquals(100, coupon.totalQuantity)
    }

    @Test
    @DisplayName("쿠폰 ID로 조회 - 존재하지 않는 경우")
    fun `findById should return null when not exists`() {
        // When
        val coupon = couponRepository.findById(999L)

        // Then
        assertNull(coupon)
    }

    @Test
    @DisplayName("쿠폰 저장 - 신규 생성")
    fun `save should create new coupon`() {
        // Given
        val newCoupon = CouponEntity(
            name = "신규 쿠폰 20% 할인",
            discountType = DiscountType.PERCENTAGE,
            discountValue = BigDecimal("20"),
            minOrderAmount = BigDecimal("50000"),
            maxDiscountAmount = BigDecimal("10000"),
            totalQuantity = 200,
            validFrom = LocalDateTime.now(),
            validUntil = LocalDateTime.now().plusDays(60)
        )

        // When
        val savedCoupon = couponRepository.save(newCoupon)

        // Then
        assertTrue(savedCoupon.id > 0)
        assertEquals("신규 쿠폰 20% 할인", savedCoupon.name)
        assertEquals(DiscountType.PERCENTAGE, savedCoupon.discountType)
        assertEquals(200, savedCoupon.totalQuantity)
    }

    @Test
    @DisplayName("쿠폰 저장 - 기존 쿠폰 업데이트")
    fun `save should update existing coupon`() {
        // Given: 기존 쿠폰 조회 및 수정
        val existingCoupon = couponRepository.findById(testCoupon1.id)!!
        val updatedCoupon = CouponEntity(
            id = existingCoupon.id,
            name = "신규 가입 15% 할인 (수정)",
            discountType = existingCoupon.discountType,
            discountValue = BigDecimal("15"),
            minOrderAmount = existingCoupon.minOrderAmount,
            maxDiscountAmount = existingCoupon.maxDiscountAmount,
            totalQuantity = existingCoupon.totalQuantity,
            validFrom = existingCoupon.validFrom,
            validUntil = existingCoupon.validUntil,
            createdAt = existingCoupon.createdAt
        )

        // When
        val savedCoupon = couponRepository.save(updatedCoupon)

        // Then
        assertEquals(existingCoupon.id, savedCoupon.id)
        assertEquals("신규 가입 15% 할인 (수정)", savedCoupon.name)
        assertTrue(BigDecimal("15").compareTo(savedCoupon.discountValue) == 0)
    }

    @Test
    @DisplayName("쿠폰 목록 조회 - 페이징 테스트")
    fun `findAllCoupons should respect page and size parameters`() {
        // When: 첫 번째 페이지, 크기 1
        val coupons = couponRepository.findAllCoupons(
            page = 1,
            size = 1,
            sortBy = "createdAt",
            sortDirection = "DESC"
        )

        // Then: 1개만 조회되어야 함
        assertEquals(1, coupons.size)
    }
}
