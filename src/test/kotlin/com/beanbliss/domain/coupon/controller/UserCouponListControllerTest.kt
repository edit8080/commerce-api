package com.beanbliss.domain.coupon.controller

import com.beanbliss.common.dto.PageableResponse
import com.beanbliss.domain.coupon.dto.UserCouponListData
import com.beanbliss.domain.coupon.dto.UserCouponListResponse
import com.beanbliss.domain.coupon.dto.UserCouponResponse
import com.beanbliss.domain.coupon.enums.UserCouponStatus
import com.beanbliss.domain.coupon.service.UserCouponService
import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import java.time.LocalDateTime

/**
 * [책임]: UserCouponController의 쿠폰 목록 조회 API 검증
 * - HTTP 요청/응답 검증
 * - 페이징 파라미터 검증
 * - 공통 응답 구조 검증 (data, pageable)
 */
@WebMvcTest(UserCouponController::class)
@DisplayName("사용자 쿠폰 목록 조회 Controller 테스트")
class UserCouponListControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockkBean
    private lateinit var userCouponService: UserCouponService

    @Test
    @DisplayName("GET /api/users/{userId}/coupons 요청 시 200 OK와 쿠폰 목록을 반환해야 한다")
    fun `GET 요청 시_200 OK와 쿠폰 목록을 반환해야 한다`() {
        // Given
        val userId = 123L
        val now = LocalDateTime.now()

        val mockResponse = UserCouponListResponse(
            data = UserCouponListData(
                content = listOf(
                    UserCouponResponse(
                        userCouponId = 456L,
                        couponId = 1L,
                        couponName = "오픈 기념! 선착순 100명 10% 할인 쿠폰",
                        discountType = "PERCENTAGE",
                        discountValue = 10,
                        minOrderAmount = 10000,
                        maxDiscountAmount = 5000,
                        status = UserCouponStatus.ISSUED,
                        validFrom = now.minusDays(1),
                        validUntil = now.plusDays(30),
                        issuedAt = now.minusHours(2),
                        usedAt = null,
                        usedOrderId = null,
                        isAvailable = true
                    ),
                    UserCouponResponse(
                        userCouponId = 457L,
                        couponId = 2L,
                        couponName = "신규 회원 5000원 할인 쿠폰",
                        discountType = "FIXED_AMOUNT",
                        discountValue = 5000,
                        minOrderAmount = 30000,
                        maxDiscountAmount = 5000,
                        status = UserCouponStatus.USED,
                        validFrom = now.minusDays(10),
                        validUntil = now.plusDays(20),
                        issuedAt = now.minusDays(5),
                        usedAt = now.minusDays(3),
                        usedOrderId = 789L,
                        isAvailable = false
                    )
                ),
                pageable = PageableResponse(
                    pageNumber = 1,
                    pageSize = 10,
                    totalElements = 2,
                    totalPages = 1
                )
            )
        )

        every { userCouponService.getUserCoupons(userId, 1, 10) } returns mockResponse

        // When & Then
        mockMvc.perform(
            get("/api/users/$userId/coupons")
                .param("page", "1")
                .param("size", "10")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data").exists())
            .andExpect(jsonPath("$.data.content").isArray)
            .andExpect(jsonPath("$.data.content.length()").value(2))
            .andExpect(jsonPath("$.data.content[0].userCouponId").value(456))
            .andExpect(jsonPath("$.data.content[0].couponName").value("오픈 기념! 선착순 100명 10% 할인 쿠폰"))
            .andExpect(jsonPath("$.data.content[0].status").value("ISSUED"))
            .andExpect(jsonPath("$.data.content[0].isAvailable").value(true))
            .andExpect(jsonPath("$.data.content[1].userCouponId").value(457))
            .andExpect(jsonPath("$.data.content[1].status").value("USED"))
            .andExpect(jsonPath("$.data.content[1].isAvailable").value(false))
            .andExpect(jsonPath("$.data.pageable.pageNumber").value(1))
            .andExpect(jsonPath("$.data.pageable.pageSize").value(10))
            .andExpect(jsonPath("$.data.pageable.totalElements").value(2))
            .andExpect(jsonPath("$.data.pageable.totalPages").value(1))
    }

    @Test
    @DisplayName("페이징 파라미터가 없으면 기본값(page=1, size=10)을 사용해야 한다")
    fun `페이징 파라미터가 없으면_기본값을 사용해야 한다`() {
        // Given
        val userId = 123L
        val mockResponse = UserCouponListResponse(
            data = UserCouponListData(
                content = emptyList(),
                pageable = PageableResponse(
                    pageNumber = 1,
                    pageSize = 10,
                    totalElements = 0,
                    totalPages = 0
                )
            )
        )

        every { userCouponService.getUserCoupons(userId, 1, 10) } returns mockResponse

        // When & Then
        mockMvc.perform(get("/api/users/$userId/coupons"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.pageable.pageNumber").value(1))
            .andExpect(jsonPath("$.data.pageable.pageSize").value(10))
    }

    @Test
    @DisplayName("쿠폰이 없을 경우 빈 배열과 페이징 정보를 반환해야 한다")
    fun `쿠폰이 없을 경우_빈 배열과 페이징 정보를 반환해야 한다`() {
        // Given
        val userId = 123L
        val mockResponse = UserCouponListResponse(
            data = UserCouponListData(
                content = emptyList(),
                pageable = PageableResponse(
                    pageNumber = 1,
                    pageSize = 10,
                    totalElements = 0,
                    totalPages = 0
                )
            )
        )

        every { userCouponService.getUserCoupons(userId, 1, 10) } returns mockResponse

        // When & Then
        mockMvc.perform(
            get("/api/users/$userId/coupons")
                .param("page", "1")
                .param("size", "10")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.content").isArray)
            .andExpect(jsonPath("$.data.content.length()").value(0))
            .andExpect(jsonPath("$.data.pageable.totalElements").value(0))
            .andExpect(jsonPath("$.data.pageable.totalPages").value(0))
    }

    @Test
    @DisplayName("page가 0 이하이면 400 Bad Request를 반환해야 한다")
    fun `page가 0 이하이면_400 Bad Request를 반환해야 한다`() {
        // Given
        val userId = 123L

        // When & Then
        mockMvc.perform(
            get("/api/users/$userId/coupons")
                .param("page", "0")
                .param("size", "10")
        )
            .andExpect(status().isBadRequest)
    }

    @Test
    @DisplayName("size가 0 이하이면 400 Bad Request를 반환해야 한다")
    fun `size가 0 이하이면_400 Bad Request를 반환해야 한다`() {
        // Given
        val userId = 123L

        // When & Then
        mockMvc.perform(
            get("/api/users/$userId/coupons")
                .param("page", "1")
                .param("size", "0")
        )
            .andExpect(status().isBadRequest)
    }

    @Test
    @DisplayName("size가 100을 초과하면 400 Bad Request를 반환해야 한다")
    fun `size가 100을 초과하면_400 Bad Request를 반환해야 한다`() {
        // Given
        val userId = 123L

        // When & Then
        mockMvc.perform(
            get("/api/users/$userId/coupons")
                .param("page", "1")
                .param("size", "101")
        )
            .andExpect(status().isBadRequest)
    }
}
