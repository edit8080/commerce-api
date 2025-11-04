package com.beanbliss.domain.user.controller

import com.beanbliss.domain.user.dto.BalanceResponse
import com.beanbliss.domain.user.exception.BalanceNotFoundException
import com.beanbliss.domain.user.service.BalanceService
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
 * [책임]: UserController의 잔액 조회 API 검증
 * - HTTP 요청/응답 검증
 * - Service 호출 검증
 */
@WebMvcTest(UserController::class)
@DisplayName("사용자 잔액 조회 Controller 테스트")
class GetBalanceControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockkBean
    private lateinit var balanceService: BalanceService

    @Test
    @DisplayName("GET /api/users/{userId}/balance 요청 시 200 OK와 잔액 정보를 반환해야 한다")
    fun `GET 요청 시_200 OK와 잔액 정보를 반환해야 한다`() {
        // Given
        val userId = 123L
        val now = LocalDateTime.now()

        val mockResponse = BalanceResponse(
            userId = userId,
            amount = 50000,
            lastUpdatedAt = now
        )

        every { balanceService.getBalance(userId) } returns mockResponse

        // When & Then
        mockMvc.perform(get("/api/users/{userId}/balance", userId))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.userId").value(userId))
            .andExpect(jsonPath("$.data.amount").value(50000))
            .andExpect(jsonPath("$.data.lastUpdatedAt").exists())
    }

    @Test
    @DisplayName("존재하지 않는 사용자의 잔액 조회 시 404 Not Found를 반환해야 한다")
    fun `존재하지 않는 사용자의 잔액 조회 시_404 Not Found를 반환해야 한다`() {
        // Given
        val userId = 999L

        every { balanceService.getBalance(userId) } throws BalanceNotFoundException("잔액 정보를 찾을 수 없습니다.")

        // When & Then
        mockMvc.perform(get("/api/users/{userId}/balance", userId))
            .andExpect(status().isNotFound)
    }

    @Test
    @DisplayName("잔액이 0원인 경우도 정상적으로 반환해야 한다")
    fun `잔액이 0원인 경우도 정상적으로 반환해야 한다`() {
        // Given
        val userId = 123L
        val now = LocalDateTime.now()

        val mockResponse = BalanceResponse(
            userId = userId,
            amount = 0,
            lastUpdatedAt = now
        )

        every { balanceService.getBalance(userId) } returns mockResponse

        // When & Then
        mockMvc.perform(get("/api/users/{userId}/balance", userId))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.userId").value(userId))
            .andExpect(jsonPath("$.data.amount").value(0))
    }
}
