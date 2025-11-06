package com.beanbliss.domain.coupon.service

import com.beanbliss.domain.coupon.domain.DiscountType
import com.beanbliss.domain.coupon.dto.CreateCouponRequest
import com.beanbliss.domain.coupon.entity.CouponEntity
import com.beanbliss.domain.coupon.exception.InvalidCouponException
import com.beanbliss.domain.coupon.repository.CouponRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.LocalDateTime

/**
 * 쿠폰 생성 Service 테스트
 *
 * [테스트 목적]:
 * - Service 계층의 비즈니스 규칙 검증이 올바르게 동작하는지 검증
 * - 정액 할인에 최대 할인 금액 설정 불가 규칙 검증
 * - Entity → Service DTO 변환 검증
 */
@DisplayName("쿠폰 생성 Service 테스트")
class CreateCouponServiceTest {

    private val couponRepository: CouponRepository = mockk()
    private val userCouponRepository = mockk<com.beanbliss.domain.coupon.repository.UserCouponRepository>()
    private val couponService: CouponService = CouponServiceImpl(couponRepository, userCouponRepository)

    private val now = LocalDateTime.now()

    @Test
    fun `정률 할인 쿠폰 생성 성공 시_Repository save가 호출되고 Service DTO를 반환해야 한다`() {
        // Given
        val request = CreateCouponRequest(
            name = "오픈 기념 10% 할인 쿠폰",
            discountType = DiscountType.PERCENTAGE,
            discountValue = 10,
            minOrderAmount = 10000,
            maxDiscountAmount = 5000,
            totalQuantity = 100,
            validFrom = now,
            validUntil = now.plusDays(30)
        )

        val savedCoupon = CouponEntity(
            id = 1L,
            name = request.name,
            discountType = "PERCENTAGE",
            discountValue = request.discountValue,
            minOrderAmount = request.minOrderAmount,
            maxDiscountAmount = request.maxDiscountAmount ?: 0,
            totalQuantity = request.totalQuantity,
            validFrom = request.validFrom,
            validUntil = request.validUntil,
            createdAt = now,
            updatedAt = now
        )

        // Mocking
        val couponSlot = slot<CouponEntity>()
        every { couponRepository.save(capture(couponSlot)) } returns savedCoupon

        // When
        val couponInfo = couponService.createCoupon(request)

        // Then
        // [검증 1]: Repository.save()가 호출되었는가?
        verify(exactly = 1) { couponRepository.save(any()) }

        // [검증 2]: 저장된 쿠폰 Entity 정보가 올바른가?
        assertEquals(request.name, couponSlot.captured.name)
        assertEquals("PERCENTAGE", couponSlot.captured.discountType)
        assertEquals(request.discountValue, couponSlot.captured.discountValue)
        assertEquals(5000, couponSlot.captured.maxDiscountAmount)

        // [검증 3]: 반환된 Service DTO가 올바른가?
        assertEquals(1L, couponInfo.id)
        assertEquals("오픈 기념 10% 할인 쿠폰", couponInfo.name)
        assertEquals("PERCENTAGE", couponInfo.discountType)
        assertEquals(10, couponInfo.discountValue)
        assertEquals(5000, couponInfo.maxDiscountAmount)
    }

    @Test
    fun `정액 할인 쿠폰 생성 성공 시_Repository save가 호출되고 Service DTO를 반환해야 한다`() {
        // Given
        val request = CreateCouponRequest(
            name = "신규 회원 5000원 할인 쿠폰",
            discountType = DiscountType.FIXED_AMOUNT,
            discountValue = 5000,
            minOrderAmount = 30000,
            maxDiscountAmount = null,
            totalQuantity = 500,
            validFrom = now,
            validUntil = now.plusDays(60)
        )

        val savedCoupon = CouponEntity(
            id = 2L,
            name = request.name,
            discountType = "FIXED_AMOUNT",
            discountValue = request.discountValue,
            minOrderAmount = request.minOrderAmount,
            maxDiscountAmount = 0,
            totalQuantity = request.totalQuantity,
            validFrom = request.validFrom,
            validUntil = request.validUntil,
            createdAt = now,
            updatedAt = now
        )

        // Mocking
        every { couponRepository.save(any()) } returns savedCoupon

        // When
        val couponInfo = couponService.createCoupon(request)

        // Then
        verify(exactly = 1) { couponRepository.save(any()) }
        assertEquals(2L, couponInfo.id)
        assertEquals("신규 회원 5000원 할인 쿠폰", couponInfo.name)
        assertEquals("FIXED_AMOUNT", couponInfo.discountType)
        assertEquals(5000, couponInfo.discountValue)
    }

    @Test
    fun `정액 할인에 maxDiscountAmount가 설정된 경우_InvalidCouponException이 발생해야 한다`() {
        // Given
        val request = CreateCouponRequest(
            name = "잘못된 최대 할인 금액 쿠폰",
            discountType = DiscountType.FIXED_AMOUNT,
            discountValue = 5000,
            minOrderAmount = 0,
            maxDiscountAmount = 10000, // 정액 할인에는 maxDiscountAmount 설정 불가
            totalQuantity = 100,
            validFrom = now,
            validUntil = now.plusDays(30)
        )

        // When & Then
        val exception = assertThrows<InvalidCouponException> {
            couponService.createCoupon(request)
        }

        assertEquals("정액 할인에는 최대 할인 금액을 설정할 수 없습니다.", exception.message)

        // [검증]: 예외 발생 시 Repository.save()는 호출되지 않아야 한다
        verify(exactly = 0) { couponRepository.save(any()) }
    }

    @Test
    fun `정률 할인에 maxDiscountAmount가 null인 경우_정상 처리되고 Service DTO를 반환해야 한다`() {
        // Given
        val request = CreateCouponRequest(
            name = "최대 할인 금액 없는 정률 할인 쿠폰",
            discountType = DiscountType.PERCENTAGE,
            discountValue = 10,
            minOrderAmount = 0,
            maxDiscountAmount = null, // 정률 할인에서 maxDiscountAmount는 선택 사항
            totalQuantity = 100,
            validFrom = now,
            validUntil = now.plusDays(30)
        )

        val savedCoupon = CouponEntity(
            id = 3L,
            name = request.name,
            discountType = "PERCENTAGE",
            discountValue = request.discountValue,
            minOrderAmount = request.minOrderAmount,
            maxDiscountAmount = 0,
            totalQuantity = request.totalQuantity,
            validFrom = request.validFrom,
            validUntil = request.validUntil,
            createdAt = now,
            updatedAt = now
        )

        // Mocking
        every { couponRepository.save(any()) } returns savedCoupon

        // When
        val couponInfo = couponService.createCoupon(request)

        // Then
        verify(exactly = 1) { couponRepository.save(any()) }
        assertEquals(3L, couponInfo.id)
        assertEquals("최대 할인 금액 없는 정률 할인 쿠폰", couponInfo.name)
        assertEquals(0, couponInfo.maxDiscountAmount) // null은 0으로 저장
    }
}
