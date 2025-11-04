package com.beanbliss.domain.coupon.controller

import com.beanbliss.common.exception.ResourceNotFoundException
import com.beanbliss.domain.coupon.dto.IssueCouponResponse
import com.beanbliss.domain.coupon.enums.UserCouponStatus
import com.beanbliss.domain.coupon.exception.*
import com.beanbliss.domain.coupon.service.CouponIssueUseCase
import com.beanbliss.domain.coupon.service.CouponService
import com.fasterxml.jackson.databind.ObjectMapper
import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import java.time.LocalDateTime

@WebMvcTest(CouponController::class)
@DisplayName("쿠폰 발급 Controller 테스트")
class CouponIssueControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockkBean
    private lateinit var couponIssueUseCase: CouponIssueUseCase

    @MockkBean
    private lateinit var couponService: CouponService

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @Test
    fun `POST 쿠폰 발급 성공 시_200 OK 응답과 발급된 쿠폰 정보를 반환해야 한다`() {
        // Given
        val couponId = 1L
        val userId = 100L
        val requestBody = """{"userId": $userId}"""

        val mockResponse = IssueCouponResponse(
            userCouponId = 1L,
            couponId = couponId,
            userId = userId,
            couponName = "신규 가입 쿠폰",
            discountType = "PERCENTAGE",
            discountValue = 10,
            minOrderAmount = 10000,
            maxDiscountAmount = 5000,
            status = UserCouponStatus.ISSUED,
            validFrom = LocalDateTime.now(),
            validUntil = LocalDateTime.now().plusDays(7),
            issuedAt = LocalDateTime.now()
        )

        every { couponIssueUseCase.issueCoupon(couponId, userId) } returns mockResponse

        // When & Then
        mockMvc.perform(
            post("/api/coupons/$couponId/issue")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody)
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.userCouponId").value(1))
            .andExpect(jsonPath("$.couponId").value(couponId))
            .andExpect(jsonPath("$.userId").value(userId))
            .andExpect(jsonPath("$.couponName").value("신규 가입 쿠폰"))
            .andExpect(jsonPath("$.status").value("ISSUED"))
    }

    @Test
    fun `존재하지 않는 쿠폰 발급 요청 시_404 Not Found를 반환해야 한다`() {
        // Given
        val couponId = 999L
        val userId = 100L
        val requestBody = """{"userId": $userId}"""

        every { couponIssueUseCase.issueCoupon(couponId, userId) } throws ResourceNotFoundException("쿠폰을 찾을 수 없습니다.")

        // When & Then
        mockMvc.perform(
            post("/api/coupons/$couponId/issue")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody)
        )
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"))
    }

    @Test
    fun `userId가 null일 때_400 Bad Request를 반환해야 한다`() {
        // Given
        val couponId = 1L
        val requestBody = """{}"""

        // When & Then
        mockMvc.perform(
            post("/api/coupons/$couponId/issue")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody)
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.code").value("INVALID_INPUT"))
    }

    @Test
    fun `userId가 음수일 때_400 Bad Request를 반환해야 한다`() {
        // Given
        val couponId = 1L
        val requestBody = """{"userId": -1}"""

        // When & Then
        mockMvc.perform(
            post("/api/coupons/$couponId/issue")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody)
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.code").value("INVALID_INPUT"))
    }

    @Test
    fun `유효기간이 지난 쿠폰 발급 요청 시_400 Bad Request를 반환해야 한다`() {
        // Given
        val couponId = 1L
        val userId = 100L
        val requestBody = """{"userId": $userId}"""

        every { couponIssueUseCase.issueCoupon(couponId, userId) } throws CouponExpiredException("유효기간이 만료된 쿠폰입니다.")

        // When & Then
        mockMvc.perform(
            post("/api/coupons/$couponId/issue")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody)
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.code").exists())
    }

    @Test
    fun `이미 발급받은 쿠폰 재발급 요청 시_409 Conflict를 반환해야 한다`() {
        // Given
        val couponId = 1L
        val userId = 100L
        val requestBody = """{"userId": $userId}"""

        every { couponIssueUseCase.issueCoupon(couponId, userId) } throws CouponAlreadyIssuedException("이미 발급받은 쿠폰입니다.")

        // When & Then
        mockMvc.perform(
            post("/api/coupons/$couponId/issue")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody)
        )
            .andExpect(status().isConflict)
            .andExpect(jsonPath("$.code").value("ALREADY_ISSUED"))
    }

    @Test
    fun `발급 가능한 재고가 없을 때_400 Bad Request를 반환해야 한다`() {
        // Given
        val couponId = 1L
        val userId = 100L
        val requestBody = """{"userId": $userId}"""

        every { couponIssueUseCase.issueCoupon(couponId, userId) } throws CouponOutOfStockException("쿠폰 재고가 부족합니다.")

        // When & Then
        mockMvc.perform(
            post("/api/coupons/$couponId/issue")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody)
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.code").value("OUT_OF_STOCK"))
    }
}
