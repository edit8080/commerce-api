package com.beanbliss.domain.coupon.controller

import com.beanbliss.common.dto.PageableResponse
import com.beanbliss.domain.coupon.dto.CouponListData
import com.beanbliss.domain.coupon.dto.CouponListResponse
import com.beanbliss.domain.coupon.dto.CouponResponse
import com.beanbliss.domain.coupon.service.CouponIssueUseCase
import com.beanbliss.domain.coupon.service.CouponService
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
 * [책임]: CouponController의 쿠폰 목록 조회 API 검증
 * - HTTP 요청/응답 검증
 * - 페이징 파라미터 검증
 * - 공통 응답 구조 검증 (data, pageable)
 */
@WebMvcTest(CouponController::class)
@DisplayName("쿠폰 목록 조회 Controller 테스트")
class CouponListControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockkBean
    private lateinit var couponService: CouponService

    @MockkBean
    private lateinit var couponIssueUseCase: CouponIssueUseCase

    @Test
    @DisplayName("GET /api/coupons 요청 시 200 OK와 쿠폰 목록을 반환해야 한다")
    fun `GET api coupons 요청 시 200 OK와 쿠폰 목록을 반환해야 한다`() {
        // Given
        val now = LocalDateTime.now()
        val mockResponse = CouponListResponse(
            data = CouponListData(
                content = listOf(
                    CouponResponse(
                        couponId = 1L,
                        name = "오픈 기념 쿠폰",
                        discountType = "PERCENTAGE",
                        discountValue = 10,
                        minOrderAmount = 10000,
                        maxDiscountAmount = 5000,
                        remainingQuantity = 47,
                        totalQuantity = 100,
                        validFrom = now.minusDays(1),
                        validUntil = now.plusDays(30),
                        isIssuable = true
                    ),
                    CouponResponse(
                        couponId = 2L,
                        name = "신규 회원 쿠폰",
                        discountType = "FIXED_AMOUNT",
                        discountValue = 5000,
                        minOrderAmount = 30000,
                        maxDiscountAmount = 5000,
                        remainingQuantity = 0,
                        totalQuantity = 500,
                        validFrom = now.minusDays(1),
                        validUntil = now.plusDays(60),
                        isIssuable = false
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

        every { couponService.getCoupons(1, 10) } returns mockResponse

        // When & Then
        mockMvc.perform(get("/api/coupons")
            .param("page", "1")
            .param("size", "10"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data").exists())
            .andExpect(jsonPath("$.data.content").isArray)
            .andExpect(jsonPath("$.data.content.length()").value(2))
            .andExpect(jsonPath("$.data.content[0].couponId").value(1))
            .andExpect(jsonPath("$.data.content[0].name").value("오픈 기념 쿠폰"))
            .andExpect(jsonPath("$.data.content[0].discountType").value("PERCENTAGE"))
            .andExpect(jsonPath("$.data.content[0].discountValue").value(10))
            .andExpect(jsonPath("$.data.content[0].remainingQuantity").value(47))
            .andExpect(jsonPath("$.data.content[0].isIssuable").value(true))
            .andExpect(jsonPath("$.data.content[1].couponId").value(2))
            .andExpect(jsonPath("$.data.content[1].isIssuable").value(false))
            .andExpect(jsonPath("$.data.pageable.pageNumber").value(1))
            .andExpect(jsonPath("$.data.pageable.pageSize").value(10))
            .andExpect(jsonPath("$.data.pageable.totalElements").value(2))
            .andExpect(jsonPath("$.data.pageable.totalPages").value(1))
    }

    @Test
    @DisplayName("페이징 파라미터가 없으면 기본값(page=1, size=10)을 사용해야 한다")
    fun `페이징 파라미터가 없으면 기본값을 사용해야 한다`() {
        // Given
        val mockResponse = CouponListResponse(
            data = CouponListData(
                content = emptyList(),
                pageable = PageableResponse(
                    pageNumber = 1,
                    pageSize = 10,
                    totalElements = 0,
                    totalPages = 0
                )
            )
        )

        every { couponService.getCoupons(1, 10) } returns mockResponse

        // When & Then
        mockMvc.perform(get("/api/coupons"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.pageable.pageNumber").value(1))
            .andExpect(jsonPath("$.data.pageable.pageSize").value(10))
    }

    @Test
    @DisplayName("쿠폰이 없을 경우 빈 배열과 페이징 정보를 반환해야 한다")
    fun `쿠폰이 없을 경우_빈 배열과 페이징 정보를 반환해야 한다`() {
        // Given
        val mockResponse = CouponListResponse(
            data = CouponListData(
                content = emptyList(),
                pageable = PageableResponse(
                    pageNumber = 1,
                    pageSize = 10,
                    totalElements = 0,
                    totalPages = 0
                )
            )
        )

        every { couponService.getCoupons(1, 10) } returns mockResponse

        // When & Then
        mockMvc.perform(get("/api/coupons")
            .param("page", "1")
            .param("size", "10"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.content").isArray)
            .andExpect(jsonPath("$.data.content.length()").value(0))
            .andExpect(jsonPath("$.data.pageable.totalElements").value(0))
            .andExpect(jsonPath("$.data.pageable.totalPages").value(0))
    }

    @Test
    @DisplayName("page가 0 이하이면 400 Bad Request를 반환해야 한다")
    fun `page가 0 이하이면 400 Bad Request를 반환해야 한다`() {
        // When & Then
        mockMvc.perform(get("/api/coupons")
            .param("page", "0")
            .param("size", "10"))
            .andExpect(status().isBadRequest)
    }

    @Test
    @DisplayName("size가 0 이하이면 400 Bad Request를 반환해야 한다")
    fun `size가 0 이하이면 400 Bad Request를 반환해야 한다`() {
        // When & Then
        mockMvc.perform(get("/api/coupons")
            .param("page", "1")
            .param("size", "0"))
            .andExpect(status().isBadRequest)
    }

    @Test
    @DisplayName("size가 100을 초과하면 400 Bad Request를 반환해야 한다")
    fun `size가 100을 초과하면 400 Bad Request를 반환해야 한다`() {
        // When & Then
        mockMvc.perform(get("/api/coupons")
            .param("page", "1")
            .param("size", "101"))
            .andExpect(status().isBadRequest)
    }
}
