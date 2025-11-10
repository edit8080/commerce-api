package com.beanbliss.domain.order.controller

import com.beanbliss.domain.cart.repository.CartItemDetail
import com.beanbliss.domain.coupon.service.CouponService
import com.beanbliss.domain.order.entity.OrderEntity
import com.beanbliss.domain.order.entity.OrderItemEntity
import com.beanbliss.domain.order.entity.OrderStatus
import com.beanbliss.domain.order.exception.*
import com.beanbliss.domain.order.usecase.CreateOrderUseCase
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
import java.math.BigDecimal
import java.time.LocalDateTime

/**
 * [책임]: OrderController의 주문 생성 API 검증
 * - HTTP 요청/응답 검증
 * - UseCase 호출 검증
 * - 예외 처리 검증
 */
@WebMvcTest(OrderController::class)
@DisplayName("주문 생성 Controller 테스트")
class CreateOrderControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockkBean
    private lateinit var createOrderUseCase: CreateOrderUseCase

    @MockkBean
    private lateinit var reserveOrderUseCase: com.beanbliss.domain.order.usecase.ReserveOrderUseCase

    @Test
    @DisplayName("POST /api/order/create 요청 시 200 OK와 주문 결과를 반환해야 한다")
    fun `POST 요청 시_200 OK와 주문 결과를 반환해야 한다`() {
        // Given
        val userId = 123L
        val userCouponId = 456L
        val shippingAddress = "서울시 강남구 테헤란로 123"
        val now = LocalDateTime.now()

        val mockOrderEntity = OrderEntity(
            id = 1001L,
            userId = userId,
            originalAmount = 30000.toBigDecimal(),
            discountAmount = 3000.toBigDecimal(),
            finalAmount = 27000.toBigDecimal(),
            shippingAddress = shippingAddress,
            status = OrderStatus.PAYMENT_COMPLETED,
            createdAt = now,
            updatedAt = now
        )

        val mockOrderItemEntities = listOf(
            OrderItemEntity(
                id = 1L,
                orderId = 1001L,
                productOptionId = 1L,
                quantity = 2,
                unitPrice = 15000.toBigDecimal(),
                totalPrice = 30000.toBigDecimal()
            )
        )

        val mockCartItems = listOf(
            CartItemDetail(
                cartItemId = 1L,
                productOptionId = 1L,
                productName = "에티오피아 예가체프 G1",
                optionCode = "ETH-HD-200",
                origin = "Ethiopia",
                grindType = "WHOLE_BEANS",
                weightGrams = 200,
                price = 15000,
                quantity = 2,
                totalPrice = 30000,
                createdAt = now,
                updatedAt = now
            )
        )

        val mockCouponInfo = CouponService.CouponInfo(
            id = 1L,
            name = "신규 가입 쿠폰",
            discountType = "FIXED",
            discountValue = 3000,
            minOrderAmount = 10000,
            maxDiscountAmount = 5000,
            totalQuantity = 100,
            validFrom = now.minusDays(7),
            validUntil = now.plusDays(30),
            createdAt = now.minusDays(10)
        )

        val mockUseCaseResult = CreateOrderUseCase.OrderCreationResult(
            orderEntity = mockOrderEntity,
            orderItemEntities = mockOrderItemEntities,
            cartItems = mockCartItems,
            couponInfo = mockCouponInfo,
            userCouponId = userCouponId,
            originalAmount = 30000,
            discountAmount = 3000,
            finalAmount = 27000,
            shippingAddress = shippingAddress
        )

        every { createOrderUseCase.createOrder(userId, userCouponId, shippingAddress) } returns mockUseCaseResult

        val requestBody = """
            {
                "userId": $userId,
                "userCouponId": $userCouponId,
                "shippingAddress": "$shippingAddress"
            }
        """.trimIndent()

        // When & Then
        mockMvc.perform(
            post("/api/order/create")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody)
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.orderId").value(1001))
            .andExpect(jsonPath("$.data.orderStatus").value("PAYMENT_COMPLETED"))
            .andExpect(jsonPath("$.data.orderItems").isArray)
            .andExpect(jsonPath("$.data.orderItems[0].productOptionId").value(1))
            .andExpect(jsonPath("$.data.orderItems[0].quantity").value(2))
            .andExpect(jsonPath("$.data.orderItems[0].unitPrice").value(15000))
            .andExpect(jsonPath("$.data.orderItems[0].totalPrice").value(30000))
            .andExpect(jsonPath("$.data.appliedCoupon.userCouponId").value(456))
            .andExpect(jsonPath("$.data.appliedCoupon.couponName").value("신규 가입 쿠폰"))
            .andExpect(jsonPath("$.data.appliedCoupon.discountType").value("FIXED"))
            .andExpect(jsonPath("$.data.appliedCoupon.discountValue").value(3000))
            .andExpect(jsonPath("$.data.priceInfo.totalProductAmount").value(30000))
            .andExpect(jsonPath("$.data.priceInfo.discountAmount").value(3000))
            .andExpect(jsonPath("$.data.priceInfo.finalAmount").value(27000))
            .andExpect(jsonPath("$.data.shippingAddress").value(shippingAddress))
    }

    @Test
    @DisplayName("쿠폰을 사용하지 않고 주문 생성 시 200 OK를 반환해야 한다")
    fun `쿠폰을 사용하지 않고 주문 생성 시_200 OK를 반환해야 한다`() {
        // Given
        val userId = 123L
        val shippingAddress = "서울시 강남구 테헤란로 123"
        val now = LocalDateTime.now()

        val mockOrderEntity = OrderEntity(
            id = 1001L,
            userId = userId,
            originalAmount = 30000.toBigDecimal(),
            discountAmount = 0.toBigDecimal(),
            finalAmount = 30000.toBigDecimal(),
            shippingAddress = shippingAddress,
            status = OrderStatus.PAYMENT_COMPLETED,
            createdAt = now,
            updatedAt = now
        )

        val mockOrderItemEntities = listOf(
            OrderItemEntity(
                id = 1L,
                orderId = 1001L,
                productOptionId = 1L,
                quantity = 2,
                unitPrice = 15000.toBigDecimal(),
                totalPrice = 30000.toBigDecimal()
            )
        )

        val mockCartItems = listOf(
            CartItemDetail(
                cartItemId = 1L,
                productOptionId = 1L,
                productName = "에티오피아 예가체프 G1",
                optionCode = "ETH-HD-200",
                origin = "Ethiopia",
                grindType = "WHOLE_BEANS",
                weightGrams = 200,
                price = 15000,
                quantity = 2,
                totalPrice = 30000,
                createdAt = now,
                updatedAt = now
            )
        )

        val mockUseCaseResult = CreateOrderUseCase.OrderCreationResult(
            orderEntity = mockOrderEntity,
            orderItemEntities = mockOrderItemEntities,
            cartItems = mockCartItems,
            couponInfo = null,  // 쿠폰 미사용
            userCouponId = null,
            originalAmount = 30000,
            discountAmount = 0,
            finalAmount = 30000,
            shippingAddress = shippingAddress
        )

        every { createOrderUseCase.createOrder(userId, null, shippingAddress) } returns mockUseCaseResult

        val requestBody = """
            {
                "userId": $userId,
                "userCouponId": null,
                "shippingAddress": "$shippingAddress"
            }
        """.trimIndent()

        // When & Then
        mockMvc.perform(
            post("/api/order/create")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody)
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.orderId").value(1001))
            .andExpect(jsonPath("$.data.appliedCoupon").doesNotExist())
            .andExpect(jsonPath("$.data.priceInfo.discountAmount").value(0))
            .andExpect(jsonPath("$.data.priceInfo.finalAmount").value(30000))
    }

    @Test
    @DisplayName("사용자 ID가 양수가 아닐 경우 400 Bad Request를 반환해야 한다")
    fun `사용자 ID가 양수가 아닐 경우_400 Bad Request를 반환해야 한다`() {
        // Given
        val invalidUserId = -1L
        val shippingAddress = "서울시 강남구 테헤란로 123"

        val requestBody = """
            {
                "userId": $invalidUserId,
                "userCouponId": null,
                "shippingAddress": "$shippingAddress"
            }
        """.trimIndent()

        // When & Then
        mockMvc.perform(
            post("/api/order/create")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody)
        )
            .andExpect(status().isBadRequest)
    }

    @Test
    @DisplayName("배송지 주소가 10자 미만일 경우 400 Bad Request를 반환해야 한다")
    fun `배송지 주소가 10자 미만일 경우_400 Bad Request를 반환해야 한다`() {
        // Given
        val userId = 123L
        val tooShortAddress = "서울시"

        val requestBody = """
            {
                "userId": $userId,
                "userCouponId": null,
                "shippingAddress": "$tooShortAddress"
            }
        """.trimIndent()

        // When & Then
        mockMvc.perform(
            post("/api/order/create")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody)
        )
            .andExpect(status().isBadRequest)
    }

    @Test
    @DisplayName("사용자 쿠폰을 찾을 수 없을 경우 404 Not Found를 반환해야 한다")
    fun `사용자 쿠폰을 찾을 수 없을 경우_404 Not Found를 반환해야 한다`() {
        // Given
        val userId = 123L
        val userCouponId = 999L
        val shippingAddress = "서울시 강남구 테헤란로 123"

        every {
            createOrderUseCase.createOrder(userId, userCouponId, shippingAddress)
        } throws UserCouponNotFoundException("사용자 쿠폰을 찾을 수 없습니다.")

        val requestBody = """
            {
                "userId": $userId,
                "userCouponId": $userCouponId,
                "shippingAddress": "$shippingAddress"
            }
        """.trimIndent()

        // When & Then
        mockMvc.perform(
            post("/api/order/create")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody)
        )
            .andExpect(status().isNotFound)
    }

    @Test
    @DisplayName("쿠폰이 만료된 경우 400 Bad Request를 반환해야 한다")
    fun `쿠폰이 만료된 경우_400 Bad Request를 반환해야 한다`() {
        // Given
        val userId = 123L
        val userCouponId = 456L
        val shippingAddress = "서울시 강남구 테헤란로 123"

        every {
            createOrderUseCase.createOrder(userId, userCouponId, shippingAddress)
        } throws UserCouponExpiredException("쿠폰이 만료되었습니다.")

        val requestBody = """
            {
                "userId": $userId,
                "userCouponId": $userCouponId,
                "shippingAddress": "$shippingAddress"
            }
        """.trimIndent()

        // When & Then
        mockMvc.perform(
            post("/api/order/create")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody)
        )
            .andExpect(status().isBadRequest)
    }

    @Test
    @DisplayName("쿠폰이 이미 사용된 경우 400 Bad Request를 반환해야 한다")
    fun `쿠폰이 이미 사용된 경우_400 Bad Request를 반환해야 한다`() {
        // Given
        val userId = 123L
        val userCouponId = 456L
        val shippingAddress = "서울시 강남구 테헤란로 123"

        every {
            createOrderUseCase.createOrder(userId, userCouponId, shippingAddress)
        } throws UserCouponAlreadyUsedException("이미 사용된 쿠폰입니다.")

        val requestBody = """
            {
                "userId": $userId,
                "userCouponId": $userCouponId,
                "shippingAddress": "$shippingAddress"
            }
        """.trimIndent()

        // When & Then
        mockMvc.perform(
            post("/api/order/create")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody)
        )
            .andExpect(status().isBadRequest)
    }

    @Test
    @DisplayName("최소 주문 금액 미달로 쿠폰을 사용할 수 없을 경우 400 Bad Request를 반환해야 한다")
    fun `최소 주문 금액 미달로 쿠폰을 사용할 수 없을 경우_400 Bad Request를 반환해야 한다`() {
        // Given
        val userId = 123L
        val userCouponId = 456L
        val shippingAddress = "서울시 강남구 테헤란로 123"

        every {
            createOrderUseCase.createOrder(userId, userCouponId, shippingAddress)
        } throws InvalidCouponOrderAmountException("최소 주문 금액 미달로 쿠폰을 사용할 수 없습니다.")

        val requestBody = """
            {
                "userId": $userId,
                "userCouponId": $userCouponId,
                "shippingAddress": "$shippingAddress"
            }
        """.trimIndent()

        // When & Then
        mockMvc.perform(
            post("/api/order/create")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody)
        )
            .andExpect(status().isBadRequest)
    }

    @Test
    @DisplayName("재고 예약을 찾을 수 없을 경우 404 Not Found를 반환해야 한다")
    fun `재고 예약을 찾을 수 없을 경우_404 Not Found를 반환해야 한다`() {
        // Given
        val userId = 123L
        val shippingAddress = "서울시 강남구 테헤란로 123"

        every {
            createOrderUseCase.createOrder(userId, null, shippingAddress)
        } throws InventoryReservationNotFoundException("재고 예약을 찾을 수 없습니다.")

        val requestBody = """
            {
                "userId": $userId,
                "userCouponId": null,
                "shippingAddress": "$shippingAddress"
            }
        """.trimIndent()

        // When & Then
        mockMvc.perform(
            post("/api/order/create")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody)
        )
            .andExpect(status().isNotFound)
    }

    @Test
    @DisplayName("재고 예약이 만료된 경우 400 Bad Request를 반환해야 한다")
    fun `재고 예약이 만료된 경우_400 Bad Request를 반환해야 한다`() {
        // Given
        val userId = 123L
        val shippingAddress = "서울시 강남구 테헤란로 123"

        every {
            createOrderUseCase.createOrder(userId, null, shippingAddress)
        } throws InventoryReservationExpiredException("재고 예약이 만료되었습니다.")

        val requestBody = """
            {
                "userId": $userId,
                "userCouponId": null,
                "shippingAddress": "$shippingAddress"
            }
        """.trimIndent()

        // When & Then
        mockMvc.perform(
            post("/api/order/create")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody)
        )
            .andExpect(status().isBadRequest)
    }

    @Test
    @DisplayName("사용자 잔액이 부족한 경우 409 Conflict를 반환해야 한다")
    fun `사용자 잔액이 부족한 경우_409 Conflict를 반환해야 한다`() {
        // Given
        val userId = 123L
        val shippingAddress = "서울시 강남구 테헤란로 123"

        every {
            createOrderUseCase.createOrder(userId, null, shippingAddress)
        } throws InsufficientBalanceException("사용자 잔액이 부족합니다.")

        val requestBody = """
            {
                "userId": $userId,
                "userCouponId": null,
                "shippingAddress": "$shippingAddress"
            }
        """.trimIndent()

        // When & Then
        mockMvc.perform(
            post("/api/order/create")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody)
        )
            .andExpect(status().isConflict)
    }

    @Test
    @DisplayName("비활성화된 상품 옵션이 포함된 경우 400 Bad Request를 반환해야 한다")
    fun `비활성화된 상품 옵션이 포함된 경우_400 Bad Request를 반환해야 한다`() {
        // Given
        val userId = 123L
        val shippingAddress = "서울시 강남구 테헤란로 123"

        every {
            createOrderUseCase.createOrder(userId, null, shippingAddress)
        } throws ProductOptionInactiveException("비활성화된 상품 옵션이 포함되어 있습니다.")

        val requestBody = """
            {
                "userId": $userId,
                "userCouponId": null,
                "shippingAddress": "$shippingAddress"
            }
        """.trimIndent()

        // When & Then
        mockMvc.perform(
            post("/api/order/create")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody)
        )
            .andExpect(status().isBadRequest)
    }
}
