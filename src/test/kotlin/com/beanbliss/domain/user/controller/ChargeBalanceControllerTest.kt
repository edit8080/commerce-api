package com.beanbliss.domain.user.controller

import com.beanbliss.domain.user.dto.ChargeBalanceResponse
import com.beanbliss.domain.user.exception.BalanceNotFoundException
import com.beanbliss.domain.user.service.BalanceService
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
 * - Service 호출 검증
 * - 유효성 검증 (Bean Validation)
 */
@WebMvcTest(UserController::class)
@DisplayName("사용자 잔액 충전 Controller 테스트")
class ChargeBalanceControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockkBean
    private lateinit var balanceService: BalanceService

    @Test
    @DisplayName("POST /api/users/{userId}/balance/charge 요청 시 200 OK와 충전 결과를 반환해야 한다")
    fun `POST 요청 시_200 OK와 충전 결과를 반환해야 한다`() {
        // Given
        val userId = 123L
        val chargeAmount = 50000
        val now = LocalDateTime.now()

        val mockResponse = ChargeBalanceResponse(
            userId = userId,
            previousBalance = 30000,
            chargeAmount = chargeAmount,
            currentBalance = 80000,
            chargedAt = now
        )

        every { balanceService.chargeBalance(userId, chargeAmount) } returns mockResponse

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
            .andExpect(jsonPath("$.data.previousBalance").value(30000))
            .andExpect(jsonPath("$.data.chargeAmount").value(chargeAmount))
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
    @DisplayName("존재하지 않는 사용자의 잔액 충전 시 404 Not Found를 반환해야 한다")
    fun `존재하지 않는 사용자의 잔액 충전 시_404 Not Found를 반환해야 한다`() {
        // Given
        val userId = 999L
        val chargeAmount = 50000

        every { balanceService.chargeBalance(userId, chargeAmount) } throws BalanceNotFoundException("잔액 정보를 찾을 수 없습니다.")

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
            .andExpect(status().isNotFound)
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
            previousBalance = 0,
            chargeAmount = minChargeAmount,
            currentBalance = minChargeAmount,
            chargedAt = now
        )

        every { balanceService.chargeBalance(userId, minChargeAmount) } returns mockResponse

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
            .andExpect(jsonPath("$.data.chargeAmount").value(minChargeAmount))
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
            previousBalance = 500000,
            chargeAmount = maxChargeAmount,
            currentBalance = 1500000,
            chargedAt = now
        )

        every { balanceService.chargeBalance(userId, maxChargeAmount) } returns mockResponse

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
            .andExpect(jsonPath("$.data.chargeAmount").value(maxChargeAmount))
            .andExpect(jsonPath("$.data.currentBalance").value(1500000))
    }
}
