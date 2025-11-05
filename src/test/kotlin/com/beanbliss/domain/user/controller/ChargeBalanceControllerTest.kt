package com.beanbliss.domain.user.controller

import com.beanbliss.domain.user.dto.ChargeBalanceResponse
import com.beanbliss.domain.user.usecase.ChargeBalanceUseCase
import com.beanbliss.domain.user.usecase.GetBalanceUseCase
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

/**
 * [책임]: UserController의 잔액 충전 API 검증
 * - HTTP 요청/응답 검증
 * - UseCase 호출 검증
 * - 유효성 검증 (Bean Validation)
 */
@WebMvcTest(UserController::class)
@DisplayName("사용자 잔액 충전 Controller 테스트")
class ChargeBalanceControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockkBean
    private lateinit var getBalanceUseCase: GetBalanceUseCase

    @MockkBean
    private lateinit var chargeBalanceUseCase: ChargeBalanceUseCase

    @Test
    @DisplayName("POST /api/users/{userId}/balance/charge 요청 시 200 OK와 충전 결과를 반환해야 한다")
    fun `POST 요청 시_200 OK와 충전 결과를 반환해야 한다`() {
        // Given
        val userId = 123L
        val chargeAmount = 50000
        val now = LocalDateTime.now()

        val mockResponse = ChargeBalanceResponse(
            userId = userId,
            currentBalance = 80000,
            chargedAt = now
        )

        every { chargeBalanceUseCase.chargeBalance(userId, chargeAmount) } returns mockResponse

        val requestBody = """
            {
                "chargeAmount": $chargeAmount
            }
        """.trimIndent()

        // When & Then
        mockMvc.perform(
            post("/api/users/{userId}/balance/charge", userId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody)
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.userId").value(userId))
            .andExpect(jsonPath("$.data.currentBalance").value(80000))
            .andExpect(jsonPath("$.data.chargedAt").exists())
    }

    @Test
    @DisplayName("충전 금액이 최소 금액(1,000원) 미만일 경우 400 Bad Request를 반환해야 한다")
    fun `충전 금액이 최소 금액 미만일 경우_400 Bad Request를 반환해야 한다`() {
        // Given
        val userId = 123L
        val invalidChargeAmount = 500 // 1,000원 미만

        val requestBody = """
            {
                "chargeAmount": $invalidChargeAmount
            }
        """.trimIndent()

        // When & Then
        mockMvc.perform(
            post("/api/users/{userId}/balance/charge", userId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody)
        )
            .andExpect(status().isBadRequest)
    }

    @Test
    @DisplayName("충전 금액이 최대 금액(1,000,000원)을 초과할 경우 400 Bad Request를 반환해야 한다")
    fun `충전 금액이 최대 금액을 초과할 경우_400 Bad Request를 반환해야 한다`() {
        // Given
        val userId = 123L
        val invalidChargeAmount = 1500000 // 1,000,000원 초과

        val requestBody = """
            {
                "chargeAmount": $invalidChargeAmount
            }
        """.trimIndent()

        // When & Then
        mockMvc.perform(
            post("/api/users/{userId}/balance/charge", userId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody)
        )
            .andExpect(status().isBadRequest)
    }

    @Test
    @DisplayName("최소 금액(1,000원) 충전 시 정상적으로 처리되어야 한다")
    fun `최소 금액 충전 시_정상적으로 처리되어야 한다`() {
        // Given
        val userId = 123L
        val minChargeAmount = 1000
        val now = LocalDateTime.now()

        val mockResponse = ChargeBalanceResponse(
            userId = userId,
            currentBalance = minChargeAmount,
            chargedAt = now
        )

        every { chargeBalanceUseCase.chargeBalance(userId, minChargeAmount) } returns mockResponse

        val requestBody = """
            {
                "chargeAmount": $minChargeAmount
            }
        """.trimIndent()

        // When & Then
        mockMvc.perform(
            post("/api/users/{userId}/balance/charge", userId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody)
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.currentBalance").value(minChargeAmount))
    }

    @Test
    @DisplayName("최대 금액(1,000,000원) 충전 시 정상적으로 처리되어야 한다")
    fun `최대 금액 충전 시_정상적으로 처리되어야 한다`() {
        // Given
        val userId = 123L
        val maxChargeAmount = 1000000
        val now = LocalDateTime.now()

        val mockResponse = ChargeBalanceResponse(
            userId = userId,
            currentBalance = 1500000,
            chargedAt = now
        )

        every { chargeBalanceUseCase.chargeBalance(userId, maxChargeAmount) } returns mockResponse

        val requestBody = """
            {
                "chargeAmount": $maxChargeAmount
            }
        """.trimIndent()

        // When & Then
        mockMvc.perform(
            post("/api/users/{userId}/balance/charge", userId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody)
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.currentBalance").value(1500000))
    }
}
