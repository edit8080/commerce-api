package com.beanbliss.domain.coupon.usecase

import com.beanbliss.domain.coupon.domain.DiscountType
import com.beanbliss.domain.coupon.dto.CreateCouponRequest
import com.beanbliss.domain.coupon.entity.CouponEntity
import com.beanbliss.domain.coupon.entity.CouponTicketEntity
import com.beanbliss.domain.coupon.exception.InvalidCouponException
import com.beanbliss.domain.coupon.service.CouponService
import com.beanbliss.domain.coupon.service.CouponTicketService
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.LocalDateTime

/**
 * 쿠폰 생성 UseCase 테스트
 *
 * [테스트 목적]:
 * - Service 조율 책임 검증 (CouponService → CouponTicketService)
 * - Service DTO → Response DTO 변환 검증
 * - 비즈니스 로직 검증은 Service 테스트에서 수행하므로 여기서는 불필요
 */
@DisplayName("쿠폰 생성 UseCase 테스트")
class CreateCouponUseCaseTest {

    private val couponService: CouponService = mockk()
    private val couponTicketService: CouponTicketService = mockk()
    private val createCouponUseCase = CreateCouponUseCase(couponService, couponTicketService)

    private val now = LocalDateTime.now()

    @Test
    fun `쿠폰 생성 시_CouponService와 CouponTicketService를 올바른 순서로 호출하고_Response DTO를 반환해야 한다`() {
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

        val mockCouponInfo = CouponService.CouponInfo(
            id = 1L,
            name = request.name,
            discountType = "PERCENTAGE",
            discountValue = request.discountValue,
            minOrderAmount = request.minOrderAmount,
            maxDiscountAmount = request.maxDiscountAmount ?: 0,
            totalQuantity = request.totalQuantity,
            validFrom = request.validFrom,
            validUntil = request.validUntil,
            createdAt = now
        )

        // Mocking
        every { couponService.createCoupon(request) } returns mockCouponInfo
        every { couponTicketService.createTickets(1L, 100) } returns emptyList()

        // When
        val couponInfo = createCouponUseCase.createCoupon(request)

        // Then
        // [검증 1]: Service 호출 순서 - CouponService.createCoupon() 먼저 호출
        verify(exactly = 1) { couponService.createCoupon(request) }

        // [검증 2]: Service 호출 순서 - CouponTicketService.createTickets() 다음 호출
        verify(exactly = 1) { couponTicketService.createTickets(mockCouponInfo.id, request.totalQuantity) }

        // [검증 3]: UseCase가 Service DTO를 그대로 반환하는지 검증
        assertEquals(mockCouponInfo.id, couponInfo.id)
        assertEquals(request.name, couponInfo.name)
        assertEquals(request.totalQuantity, couponInfo.totalQuantity)
        assertEquals("PERCENTAGE", couponInfo.discountType)
    }
}
