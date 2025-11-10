package com.beanbliss.domain.cart.controller

import com.beanbliss.common.exception.CommonExceptionHandler
import com.beanbliss.domain.cart.usecase.AddToCartUseCase
import com.beanbliss.domain.cart.usecase.AddToCartUseCaseResult
import com.beanbliss.domain.cart.dto.AddToCartRequest
import com.beanbliss.domain.cart.repository.CartItemDetail
import com.beanbliss.common.exception.ResourceNotFoundException
import com.beanbliss.common.exception.InvalidParameterException
import com.fasterxml.jackson.databind.ObjectMapper
import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import io.mockk.verify
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.DisplayName
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import java.time.LocalDateTime

/**
 * 장바구니 추가 Controller의 HTTP 요청/응답을 검증하는 테스트
 *
 * [검증 목표]:
 * 1. POST /api/cart/items 엔드포인트가 올바르게 동작하는가?
 * 2. Request Body의 JSON이 올바르게 파싱되는가?
 * 3. Response Status Code가 올바르게 반환되는가?
 * 4. Response Body의 JSON 구조가 API 명세와 일치하는가?
 * 5. 예외 발생 시 적절한 HTTP Status와 Error Response가 반환되는가?
 *
 * [관련 API]:
 * - POST /api/cart/items
 */
@WebMvcTest(controllers = [CartController::class])
@Import(CommonExceptionHandler::class)
@DisplayName("장바구니 추가 Controller 테스트")
class CartAddControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @MockkBean
    private lateinit var addToCartUseCase: AddToCartUseCase

    @Test
    @DisplayName("신규 장바구니 추가 요청 시 201 Created와 함께 장바구니 아이템 정보를 반환해야 한다")
    fun `신규 장바구니 추가 요청 시 201 Created와 함께 장바구니 아이템 정보를 반환해야 한다`() {
        // Given
        val request = AddToCartRequest(
            userId = 1L,
            productOptionId = 1L,
            quantity = 2
        )

        val mockCartItem = CartItemDetail(
            cartItemId = 100L,
            productOptionId = 1L,
            productName = "에티오피아 예가체프 G1",
            optionCode = "ETH-HD-200",
            origin = "Ethiopia",
            grindType = "HAND_DRIP",
            weightGrams = 200,
            price = 21000,
            quantity = 2,
            totalPrice = 42000,
            createdAt = LocalDateTime.of(2025, 11, 3, 14, 30),
            updatedAt = LocalDateTime.of(2025, 11, 3, 14, 30)
        )

        val mockResponse = AddToCartUseCaseResult(
            cartItem = mockCartItem,
            isNewItem = true
        )

        every { addToCartUseCase.addToCart(any()) } returns mockResponse

        // When & Then
        mockMvc.perform(
            post("/api/cart/items")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isCreated) // 201 Created
            .andExpect(jsonPath("$.data.cartItemId").value(100))
            .andExpect(jsonPath("$.data.productOptionId").value(1))
            .andExpect(jsonPath("$.data.productName").value("에티오피아 예가체프 G1"))
            .andExpect(jsonPath("$.data.optionCode").value("ETH-HD-200"))
            .andExpect(jsonPath("$.data.origin").value("Ethiopia"))
            .andExpect(jsonPath("$.data.grindType").value("HAND_DRIP"))
            .andExpect(jsonPath("$.data.weightGrams").value(200))
            .andExpect(jsonPath("$.data.price").value(21000))
            .andExpect(jsonPath("$.data.quantity").value(2))
            .andExpect(jsonPath("$.data.totalPrice").value(42000))

        // [Controller 책임 검증]: Service가 정확히 한 번 호출되었는가?
        verify(exactly = 1) { addToCartUseCase.addToCart(any()) }
    }

    @Test
    @DisplayName("기존 장바구니 아이템 수량 증가 시 200 OK와 함께 업데이트된 정보를 반환해야 한다")
    fun `기존 장바구니 아이템 수량 증가 시 200 OK와 함께 업데이트된 정보를 반환해야 한다`() {
        // Given
        val request = AddToCartRequest(
            userId = 1L,
            productOptionId = 1L,
            quantity = 2
        )

        val mockCartItem = CartItemDetail(
            cartItemId = 100L,
            productOptionId = 1L,
            productName = "에티오피아 예가체프 G1",
            optionCode = "ETH-HD-200",
            origin = "Ethiopia",
            grindType = "HAND_DRIP",
            weightGrams = 200,
            price = 21000,
            quantity = 5, // 기존 3 + 추가 2 = 5
            totalPrice = 105000,
            createdAt = LocalDateTime.of(2025, 11, 3, 14, 20),
            updatedAt = LocalDateTime.of(2025, 11, 3, 14, 30)
        )

        val mockResponse = AddToCartUseCaseResult(
            cartItem = mockCartItem,
            isNewItem = false // 기존 아이템 수량 증가
        )

        every { addToCartUseCase.addToCart(any()) } returns mockResponse

        // When & Then
        mockMvc.perform(
            post("/api/cart/items")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isOk) // 200 OK (기존 아이템 수량 증가)
            .andExpect(jsonPath("$.data.quantity").value(5))
            .andExpect(jsonPath("$.data.totalPrice").value(105000))

        verify(exactly = 1) { addToCartUseCase.addToCart(any()) }
    }

    @Test
    @DisplayName("userId가 0 이하인 요청 시 400 Bad Request를 반환해야 한다")
    fun `userId가 0 이하인 요청 시 400 Bad Request를 반환해야 한다`() {
        // Given
        val invalidRequest = """
            {
                "userId": 0,
                "productOptionId": 1,
                "quantity": 2
            }
        """.trimIndent()

        // When & Then
        mockMvc.perform(
            post("/api/cart/items")
                .contentType(MediaType.APPLICATION_JSON)
                .content(invalidRequest)
        )
            .andExpect(status().isBadRequest) // 400 Bad Request
            .andExpect(jsonPath("$.code").value("INVALID_INPUT"))

        // [Controller 책임 검증]: 유효성 검사 실패 시 Service는 호출되지 않아야 함
            verify(exactly = 0) { addToCartUseCase.addToCart(any()) }
    }

    @Test
    @DisplayName("quantity가 0 이하인 요청 시 400 Bad Request를 반환해야 한다")
    fun `quantity가 0 이하인 요청 시 400 Bad Request를 반환해야 한다`() {
        // Given
        val invalidRequest = """
            {
                "userId": 1,
                "productOptionId": 1,
                "quantity": 0
            }
        """.trimIndent()

        // When & Then
        mockMvc.perform(
            post("/api/cart/items")
                .contentType(MediaType.APPLICATION_JSON)
                .content(invalidRequest)
        )
            .andExpect(status().isBadRequest) // 400 Bad Request
            .andExpect(jsonPath("$.code").value("INVALID_INPUT"))

        verify(exactly = 0) { addToCartUseCase.addToCart(any()) }
    }

    @Test
    @DisplayName("quantity가 999를 초과하는 요청 시 400 Bad Request를 반환해야 한다")
    fun `quantity가 999를 초과하는 요청 시 400 Bad Request를 반환해야 한다`() {
        // Given
        val invalidRequest = """
            {
                "userId": 1,
                "productOptionId": 1,
                "quantity": 1000
            }
        """.trimIndent()

        // When & Then
        mockMvc.perform(
            post("/api/cart/items")
                .contentType(MediaType.APPLICATION_JSON)
                .content(invalidRequest)
        )
            .andExpect(status().isBadRequest) // 400 Bad Request
            .andExpect(jsonPath("$.code").value("INVALID_INPUT"))

        verify(exactly = 0) { addToCartUseCase.addToCart(any()) }
    }

    @Test
    @DisplayName("존재하지 않는 사용자 ID 요청 시 404 Not Found를 반환해야 한다")
    fun `존재하지 않는 사용자 ID 요청 시 404 Not Found를 반환해야 한다`() {
        // Given
        val request = AddToCartRequest(
            userId = 999L,
            productOptionId = 1L,
            quantity = 2
        )

        every { addToCartUseCase.addToCart(any()) } throws ResourceNotFoundException("사용자 ID: 999를 찾을 수 없습니다.")

        // When & Then
        mockMvc.perform(
            post("/api/cart/items")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isNotFound) // 404 Not Found
            .andExpect(jsonPath("$.status").value(404))
            .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"))
            .andExpect(jsonPath("$.message").value("사용자 ID: 999를 찾을 수 없습니다."))

        verify(exactly = 1) { addToCartUseCase.addToCart(any()) }
    }

    @Test
    @DisplayName("존재하지 않는 상품 옵션 ID 요청 시 404 Not Found를 반환해야 한다")
    fun `존재하지 않는 상품 옵션 ID 요청 시 404 Not Found를 반환해야 한다`() {
        // Given
        val request = AddToCartRequest(
            userId = 1L,
            productOptionId = 999L,
            quantity = 2
        )

        every { addToCartUseCase.addToCart(any()) } throws ResourceNotFoundException("상품 옵션 ID: 999를 찾을 수 없습니다.")

        // When & Then
        mockMvc.perform(
            post("/api/cart/items")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isNotFound) // 404 Not Found
            .andExpect(jsonPath("$.status").value(404))
            .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"))
            .andExpect(jsonPath("$.message").value("상품 옵션 ID: 999를 찾을 수 없습니다."))

        verify(exactly = 1) { addToCartUseCase.addToCart(any()) }
    }

    @Test
    @DisplayName("비활성 상태의 상품 옵션 추가 시 404 Not Found를 반환해야 한다")
    fun `비활성 상태의 상품 옵션 추가 시 404 Not Found를 반환해야 한다`() {
        // Given
        val request = AddToCartRequest(
            userId = 1L,
            productOptionId = 1L,
            quantity = 2
        )

        every { addToCartUseCase.addToCart(any()) } throws ResourceNotFoundException("해당 상품 옵션은 현재 판매하지 않습니다.")

        // When & Then
        mockMvc.perform(
            post("/api/cart/items")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isNotFound) // 404 Not Found
            .andExpect(jsonPath("$.status").value(404))
            .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"))
            .andExpect(jsonPath("$.message").value("해당 상품 옵션은 현재 판매하지 않습니다."))

        verify(exactly = 1) { addToCartUseCase.addToCart(any()) }
    }

    @Test
    @DisplayName("최대 수량 초과 시 400 Bad Request를 반환해야 한다")
    fun `최대 수량 초과 시 400 Bad Request를 반환해야 한다`() {
        // Given
        val request = AddToCartRequest(
            userId = 1L,
            productOptionId = 1L,
            quantity = 100
        )

        every { addToCartUseCase.addToCart(any()) } throws InvalidParameterException("장바구니 내 동일 옵션의 최대 수량은 999개입니다.")

        // When & Then
        mockMvc.perform(
            post("/api/cart/items")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isBadRequest) // 400 Bad Request
            .andExpect(jsonPath("$.status").value(400))
            .andExpect(jsonPath("$.code").value("INVALID_PARAMETER"))
            .andExpect(jsonPath("$.message").value("장바구니 내 동일 옵션의 최대 수량은 999개입니다."))

        verify(exactly = 1) { addToCartUseCase.addToCart(any()) }
    }

    @Test
    @DisplayName("필수 필드 누락 시 400 Bad Request를 반환해야 한다")
    fun `필수 필드 누락 시 400 Bad Request를 반환해야 한다`() {
        // Given
        val invalidRequest = """
            {
                "productOptionId": 1,
                "quantity": 2
            }
        """.trimIndent()

        // When & Then
        mockMvc.perform(
            post("/api/cart/items")
                .contentType(MediaType.APPLICATION_JSON)
                .content(invalidRequest)
        )
            .andExpect(status().isBadRequest) // 400 Bad Request
            .andExpect(jsonPath("$.code").value("INVALID_INPUT"))

        verify(exactly = 0) { addToCartUseCase.addToCart(any()) }
    }
}
