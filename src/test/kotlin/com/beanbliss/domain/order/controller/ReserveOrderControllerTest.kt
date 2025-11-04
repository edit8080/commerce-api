package com.beanbliss.domain.order.controller

import com.beanbliss.domain.inventory.entity.InventoryReservationStatus
import com.beanbliss.domain.order.dto.InventoryReservationItemResponse
import com.beanbliss.domain.order.dto.ReserveOrderResponse
import com.beanbliss.domain.order.exception.CartEmptyException
import com.beanbliss.domain.order.exception.DuplicateReservationException
import com.beanbliss.domain.order.exception.InsufficientAvailableStockException
import com.beanbliss.domain.order.usecase.ReserveOrderUseCase
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
 * [책임]: OrderController의 주문 예약 API 검증
 * - HTTP 요청/응답 검증
 * - UseCase 호출 검증
 */
@WebMvcTest(OrderController::class)
@DisplayName("주문 예약 Controller 테스트")
class ReserveOrderControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockkBean
    private lateinit var reserveOrderUseCase: ReserveOrderUseCase

    @Test
    @DisplayName("POST /api/order/reserve 요청 시 200 OK와 예약 결과를 반환해야 한다")
    fun `POST 요청 시_200 OK와 예약 결과를 반환해야 한다`() {
        // Given
        val userId = 123L
        val now = LocalDateTime.now()

        val mockResponse = ReserveOrderResponse(
            reservations = listOf(
                InventoryReservationItemResponse(
                    reservationId = 1001L,
                    productOptionId = 1L,
                    productName = "에티오피아 예가체프 G1",
                    optionCode = "ETH-HD-200",
                    quantity = 2,
                    status = InventoryReservationStatus.RESERVED,
                    availableStock = 8,
                    reservedAt = now,
                    expiresAt = now.plusMinutes(30)
                )
            )
        )

        every { reserveOrderUseCase.reserveOrder(userId) } returns mockResponse

        val requestBody = """
            {
                "userId": $userId
            }
        """.trimIndent()

        // When & Then
        mockMvc.perform(
            post("/api/order/reserve")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody)
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.reservations").isArray)
            .andExpect(jsonPath("$.data.reservations[0].reservationId").value(1001))
            .andExpect(jsonPath("$.data.reservations[0].productOptionId").value(1))
            .andExpect(jsonPath("$.data.reservations[0].quantity").value(2))
            .andExpect(jsonPath("$.data.reservations[0].status").value("RESERVED"))
            .andExpect(jsonPath("$.data.reservations[0].availableStock").value(8))
    }

    @Test
    @DisplayName("장바구니가 비어있을 경우 400 Bad Request를 반환해야 한다")
    fun `장바구니가 비어있을 경우_400 Bad Request를 반환해야 한다`() {
        // Given
        val userId = 123L

        every { reserveOrderUseCase.reserveOrder(userId) } throws CartEmptyException("장바구니가 비어 있습니다.")

        val requestBody = """
            {
                "userId": $userId
            }
        """.trimIndent()

        // When & Then
        mockMvc.perform(
            post("/api/order/reserve")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody)
        )
            .andExpect(status().isBadRequest)
    }

    @Test
    @DisplayName("이미 진행 중인 예약이 있을 경우 409 Conflict를 반환해야 한다")
    fun `이미 진행 중인 예약이 있을 경우_409 Conflict를 반환해야 한다`() {
        // Given
        val userId = 123L

        every { reserveOrderUseCase.reserveOrder(userId) } throws DuplicateReservationException("이미 진행 중인 주문 예약이 있습니다.")

        val requestBody = """
            {
                "userId": $userId
            }
        """.trimIndent()

        // When & Then
        mockMvc.perform(
            post("/api/order/reserve")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody)
        )
            .andExpect(status().isConflict)
    }

    @Test
    @DisplayName("가용 재고가 부족할 경우 409 Conflict를 반환해야 한다")
    fun `가용 재고가 부족할 경우_409 Conflict를 반환해야 한다`() {
        // Given
        val userId = 123L

        every { reserveOrderUseCase.reserveOrder(userId) } throws InsufficientAvailableStockException("가용 재고가 부족합니다.")

        val requestBody = """
            {
                "userId": $userId
            }
        """.trimIndent()

        // When & Then
        mockMvc.perform(
            post("/api/order/reserve")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody)
        )
            .andExpect(status().isConflict)
    }

    @Test
    @DisplayName("사용자 ID가 양수가 아닐 경우 400 Bad Request를 반환해야 한다")
    fun `사용자 ID가 양수가 아닐 경우_400 Bad Request를 반환해야 한다`() {
        // Given
        val invalidUserId = -1L

        val requestBody = """
            {
                "userId": $invalidUserId
            }
        """.trimIndent()

        // When & Then
        mockMvc.perform(
            post("/api/order/reserve")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody)
        )
            .andExpect(status().isBadRequest)
    }
}
