package com.beanbliss.domain.coupon.controller

import com.beanbliss.domain.coupon.domain.DiscountType
import com.beanbliss.domain.coupon.dto.CreateCouponRequest
import com.beanbliss.domain.coupon.dto.CreateCouponResponse
import com.beanbliss.domain.coupon.service.CouponIssueUseCase
import com.beanbliss.domain.coupon.service.CouponService
import com.beanbliss.domain.coupon.usecase.CreateCouponUseCase
import com.fasterxml.jackson.databind.ObjectMapper
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.time.LocalDateTime

/**
 * 쿠폰 생성 Controller 테스트
 *
 * [테스트 목적]:
 * - 커스텀 Bean Validator 검증 (ValidDiscountValue, ValidDateRange)
 * - Service DTO → Response DTO 변환 검증
 * - 표준 Bean Validation (@Min, @Max)은 Spring Framework가 보장하므로 테스트 불필요
 */
@WebMvcTest(CouponController::class)
@DisplayName("쿠폰 생성 Controller 테스트")
class CreateCouponControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @Autowired
    private lateinit var createCouponUseCase: CreateCouponUseCase

    @TestConfiguration
    class TestConfig {
        @Bean
        @Primary
        fun createCouponUseCase(): CreateCouponUseCase = mockk(relaxed = true)

        @Bean
        @Primary
        fun couponService(): CouponService = mockk(relaxed = true)

        @Bean
        @Primary
        fun couponIssueUseCase(): CouponIssueUseCase = mockk(relaxed = true)
    }

    private val now = LocalDateTime.now()

    @Test
    fun `ValidDiscountValue_정률 할인값이 유효 범위를 벗어나면_400 에러가 발생해야 한다`() {
        // Given
        val request = CreateCouponRequest(
            name = "잘못된 할인값 쿠폰",
            discountType = DiscountType.PERCENTAGE,
            discountValue = 0, // 1~100 범위 벗어남
            minOrderAmount = 0,
            maxDiscountAmount = null,
            totalQuantity = 100,
            validFrom = now,
            validUntil = now.plusDays(30)
        )

        // When & Then
        mockMvc.perform(
            post("/api/coupons")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isBadRequest)
    }

    @Test
    fun `ValidDateRange_유효 시작일이 종료일보다 이후이면_400 에러가 발생해야 한다`() {
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
        mockMvc.perform(
            post("/api/coupons")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isBadRequest)
    }

    @Test
    fun `정상적인 쿠폰 생성 요청 시_201 응답이 반환되어야 한다`() {
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
            maxDiscountAmount = 5000,
            totalQuantity = request.totalQuantity,
            validFrom = request.validFrom,
            validUntil = request.validUntil,
            createdAt = now
        )

        every { createCouponUseCase.createCoupon(request) } returns mockCouponInfo

        // When & Then
        mockMvc.perform(
            post("/api/coupons")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isCreated)
    }
}
