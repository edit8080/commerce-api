package com.beanbliss.domain.coupon.usecase

import com.beanbliss.domain.coupon.domain.DiscountType
import com.beanbliss.domain.coupon.dto.CreateCouponRequest
import com.beanbliss.domain.coupon.entity.CouponEntity
import com.beanbliss.domain.coupon.entity.CouponTicketEntity
import com.beanbliss.domain.coupon.exception.InvalidCouponException
import com.beanbliss.domain.coupon.repository.CouponRepository
import com.beanbliss.domain.coupon.repository.CouponTicketRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.LocalDateTime

@DisplayName("쿠폰 생성 UseCase 테스트")
class CreateCouponUseCaseTest {

    private val couponRepository: CouponRepository = mockk()
    private val couponTicketRepository: CouponTicketRepository = mockk()
    private val createCouponUseCase = CreateCouponUseCase(couponRepository, couponTicketRepository)

    private val now = LocalDateTime.now()

    @Test
    fun `정률 할인 쿠폰 생성 성공 시_쿠폰과 티켓이 생성되어야 한다`() {
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

        val ticketsSlot = slot<List<CouponTicketEntity>>()
        every { couponTicketRepository.saveAll(capture(ticketsSlot)) } returns emptyList()

        // When
        val response = createCouponUseCase.execute(request)

        // Then
        // [검증 1]: CouponRepository.save()가 호출되었는가?
        verify(exactly = 1) { couponRepository.save(any()) }

        // [검증 2]: 저장된 쿠폰 정보가 올바른가?
        assertEquals(request.name, couponSlot.captured.name)
        assertEquals("PERCENTAGE", couponSlot.captured.discountType)
        assertEquals(request.discountValue, couponSlot.captured.discountValue)

        // [검증 3]: CouponTicketRepository.saveAll()이 호출되었는가?
        verify(exactly = 1) { couponTicketRepository.saveAll(any()) }

        // [검증 4]: 생성된 티켓 수량이 totalQuantity와 일치하는가?
        assertEquals(request.totalQuantity, ticketsSlot.captured.size)

        // [검증 5]: 모든 티켓의 상태가 AVAILABLE이고 userId가 null인가?
        ticketsSlot.captured.forEach { ticket ->
            assertEquals("AVAILABLE", ticket.status)
            assertNull(ticket.userId)
            assertEquals(savedCoupon.id, ticket.couponId)
        }

        // [검증 6]: 응답 DTO가 올바르게 반환되는가?
        assertEquals(savedCoupon.id, response.couponId)
        assertEquals(request.name, response.name)
        assertEquals(request.totalQuantity, response.totalQuantity)
        assertEquals(DiscountType.PERCENTAGE, response.discountType)
    }

    @Test
    fun `정액 할인 쿠폰 생성 성공 시_쿠폰과 티켓이 생성되어야 한다`() {
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
        every { couponTicketRepository.saveAll(any()) } returns emptyList()

        // When
        val response = createCouponUseCase.execute(request)

        // Then
        assertEquals(savedCoupon.id, response.couponId)
        assertEquals(DiscountType.FIXED_AMOUNT, response.discountType)
        assertEquals(5000, response.discountValue)
    }

    @Test
    fun `정률 할인값이 1 미만인 경우_예외가 발생해야 한다`() {
        // Given
        val request = CreateCouponRequest(
            name = "잘못된 할인값 쿠폰",
            discountType = DiscountType.PERCENTAGE,
            discountValue = 0,
            minOrderAmount = 0,
            maxDiscountAmount = null,
            totalQuantity = 100,
            validFrom = now,
            validUntil = now.plusDays(30)
        )

        // When & Then
        val exception = assertThrows<InvalidCouponException> {
            createCouponUseCase.execute(request)
        }

        assertEquals("정률 할인의 할인값은 1 이상 100 이하여야 합니다. 현재값: 0", exception.message)
        verify(exactly = 0) { couponRepository.save(any()) }
    }

    @Test
    fun `정률 할인값이 100 초과인 경우_예외가 발생해야 한다`() {
        // Given
        val request = CreateCouponRequest(
            name = "잘못된 할인값 쿠폰",
            discountType = DiscountType.PERCENTAGE,
            discountValue = 101,
            minOrderAmount = 0,
            maxDiscountAmount = null,
            totalQuantity = 100,
            validFrom = now,
            validUntil = now.plusDays(30)
        )

        // When & Then
        val exception = assertThrows<InvalidCouponException> {
            createCouponUseCase.execute(request)
        }

        assertEquals("정률 할인의 할인값은 1 이상 100 이하여야 합니다. 현재값: 101", exception.message)
    }

    @Test
    fun `정액 할인값이 1 미만인 경우_예외가 발생해야 한다`() {
        // Given
        val request = CreateCouponRequest(
            name = "잘못된 할인값 쿠폰",
            discountType = DiscountType.FIXED_AMOUNT,
            discountValue = 0,
            minOrderAmount = 0,
            maxDiscountAmount = null,
            totalQuantity = 100,
            validFrom = now,
            validUntil = now.plusDays(30)
        )

        // When & Then
        val exception = assertThrows<InvalidCouponException> {
            createCouponUseCase.execute(request)
        }

        assertEquals("정액 할인의 할인값은 1 이상이어야 합니다. 현재값: 0", exception.message)
    }

    @Test
    fun `validFrom이 validUntil보다 이후인 경우_예외가 발생해야 한다`() {
        // Given
        val validFrom = now.plusDays(30)
        val validUntil = now
        val request = CreateCouponRequest(
            name = "잘못된 유효기간 쿠폰",
            discountType = DiscountType.PERCENTAGE,
            discountValue = 10,
            minOrderAmount = 0,
            maxDiscountAmount = null,
            totalQuantity = 100,
            validFrom = validFrom,
            validUntil = validUntil
        )

        // When & Then
        val exception = assertThrows<InvalidCouponException> {
            createCouponUseCase.execute(request)
        }

        assertEquals("유효 시작 일시는 유효 종료 일시보다 이전이어야 합니다. validFrom: $validFrom, validUntil: $validUntil", exception.message)
        verify(exactly = 0) { couponRepository.save(any()) }
    }

    @Test
    fun `정액 할인에 maxDiscountAmount가 설정된 경우_예외가 발생해야 한다`() {
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
            createCouponUseCase.execute(request)
        }

        assertEquals("정액 할인에는 최대 할인 금액을 설정할 수 없습니다.", exception.message)
        verify(exactly = 0) { couponRepository.save(any()) }
    }

    @Test
    fun `totalQuantity가 1 미만인 경우_예외가 발생해야 한다`() {
        // Given
        val request = CreateCouponRequest(
            name = "잘못된 수량 쿠폰",
            discountType = DiscountType.PERCENTAGE,
            discountValue = 10,
            minOrderAmount = 0,
            maxDiscountAmount = null,
            totalQuantity = 0,
            validFrom = now,
            validUntil = now.plusDays(30)
        )

        // When & Then
        val exception = assertThrows<InvalidCouponException> {
            createCouponUseCase.execute(request)
        }

        assertEquals("발급 수량은 1 이상 10,000 이하여야 합니다. 현재값: 0", exception.message)
        verify(exactly = 0) { couponRepository.save(any()) }
    }

    @Test
    fun `totalQuantity가 10000 초과인 경우_예외가 발생해야 한다`() {
        // Given
        val request = CreateCouponRequest(
            name = "잘못된 수량 쿠폰",
            discountType = DiscountType.PERCENTAGE,
            discountValue = 10,
            minOrderAmount = 0,
            maxDiscountAmount = null,
            totalQuantity = 10001,
            validFrom = now,
            validUntil = now.plusDays(30)
        )

        // When & Then
        val exception = assertThrows<InvalidCouponException> {
            createCouponUseCase.execute(request)
        }

        assertEquals("발급 수량은 1 이상 10,000 이하여야 합니다. 현재값: 10001", exception.message)
        verify(exactly = 0) { couponRepository.save(any()) }
    }
}
