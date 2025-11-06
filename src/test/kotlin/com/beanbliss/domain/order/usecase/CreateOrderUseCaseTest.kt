package com.beanbliss.domain.order.usecase

import com.beanbliss.domain.cart.dto.CartItemResponse
import com.beanbliss.domain.cart.service.CartService
import com.beanbliss.domain.coupon.dto.CouponValidationResult
import com.beanbliss.domain.coupon.service.CouponService
import com.beanbliss.domain.inventory.service.InventoryReservationService
import com.beanbliss.domain.inventory.service.InventoryService
import com.beanbliss.domain.order.entity.OrderEntity
import com.beanbliss.domain.order.entity.OrderItemEntity
import com.beanbliss.domain.order.entity.OrderStatus
import com.beanbliss.domain.order.exception.*
import com.beanbliss.domain.order.service.OrderService
import com.beanbliss.domain.user.exception.InsufficientBalanceException
import com.beanbliss.domain.user.service.BalanceService
import com.beanbliss.domain.user.service.UserService
import io.mockk.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.LocalDateTime

/**
 * [책임]: CreateOrderUseCase의 Service 조율 로직 검증
 * - 각 Service 메서드 호출 순서 검증
 * - Service 간 데이터 전달 검증
 * - 예외 처리 흐름 검증
 */
@DisplayName("주문 생성 UseCase 테스트")
class CreateOrderUseCaseTest {

    private lateinit var createOrderUseCase: CreateOrderUseCase
    private lateinit var userService: UserService
    private lateinit var cartService: CartService
    private lateinit var couponService: CouponService
    private lateinit var inventoryReservationService: InventoryReservationService
    private lateinit var inventoryService: InventoryService
    private lateinit var orderService: OrderService
    private lateinit var balanceService: BalanceService

    @BeforeEach
    fun setUp() {
        // Mock 객체 생성
        userService = mockk(relaxed = true)
        cartService = mockk(relaxed = true)
        couponService = mockk(relaxed = true)
        inventoryReservationService = mockk(relaxed = true)
        inventoryService = mockk(relaxed = true)
        orderService = mockk(relaxed = true)
        balanceService = mockk(relaxed = true)

        createOrderUseCase = CreateOrderUseCase(
            userService = userService,
            cartService = cartService,
            couponService = couponService,
            inventoryReservationService = inventoryReservationService,
            inventoryService = inventoryService,
            orderService = orderService,
            balanceService = balanceService
        )
    }

    // === Helper Methods for Test Data ===

    private fun createCartItem(
        productOptionId: Long = 1L,
        quantity: Int = 2,
        price: Int = 15000
    ): CartItemResponse {
        val now = LocalDateTime.now()
        return CartItemResponse(
            cartItemId = 1L,
            productOptionId = productOptionId,
            productName = "에티오피아 예가체프",
            optionCode = "ETH-YRG-WH-200g",
            origin = "Ethiopia",
            grindType = "WHOLE_BEAN",
            weightGrams = 200,
            price = price,
            quantity = quantity,
            totalPrice = price * quantity,
            createdAt = now,
            updatedAt = now
        )
    }

    private fun createCoupon(
        couponId: Long = 789L,
        discountType: String = "PERCENTAGE",
        discountValue: Int = 10,
        minOrderAmount: Int = 10000,
        maxDiscountAmount: Int = 5000,
        totalQuantity: Int = 100,
        validFrom: LocalDateTime = LocalDateTime.now().minusDays(7),
        validUntil: LocalDateTime = LocalDateTime.now().plusDays(7)
    ): CouponService.CouponInfo {
        val now = LocalDateTime.now()
        return CouponService.CouponInfo(
            id = couponId,
            name = "10% 할인 쿠폰",
            discountType = discountType,
            discountValue = discountValue,
            minOrderAmount = minOrderAmount,
            maxDiscountAmount = maxDiscountAmount,
            totalQuantity = totalQuantity,
            validFrom = validFrom,
            validUntil = validUntil,
            createdAt = now.minusDays(7)
        )
    }

    @Test
    @DisplayName("주문 생성 성공 시_주문 정보를 반환해야 한다")
    fun `주문 생성 성공 시_주문 정보를 반환해야 한다`() {
        // Given
        val userId = 123L
        val userCouponId = 456L
        val shippingAddress = "서울시 강남구 테헤란로 123"

        val cartItem = createCartItem(quantity = 2, price = 15000) // totalPrice = 30000
        val coupon = createCoupon(discountValue = 10, minOrderAmount = 10000) // 10% 할인
        val now = LocalDateTime.now()

        // Mock 설정: Service 호출
        every { userService.validateUserExists(userId) } just Runs
        every { cartService.getCartItemsWithProducts(userId) } returns listOf(cartItem)
        every { cartService.validateCartItems(any()) } just Runs
        every { couponService.validateAndGetCoupon(userId, userCouponId) } returns CouponValidationResult(coupon)
        every { couponService.calculateDiscount(coupon, 30000) } returns 3000 // 10% 할인 = 3000원
        every { inventoryReservationService.validateReservations(userId, any()) } just Runs
        every { inventoryService.reduceStockForOrder(any()) } just Runs
        every { orderService.createOrderWithItems(any()) } returns OrderService.OrderCreationResult(
            orderEntity = OrderEntity(
                id = 1L,
                userId = userId,
                totalAmount = 30000,
                discountAmount = 3000,
                finalAmount = 27000,
                shippingAddress = shippingAddress,
                orderStatus = OrderStatus.PAYMENT_COMPLETED,
                orderedAt = now,
                updatedAt = now
            ),
            orderItemEntities = listOf(
                OrderItemEntity(
                    id = 1L,
                    orderId = 1L,
                    productOptionId = 1L,
                    quantity = 2,
                    unitPrice = 15000,
                    totalPrice = 30000
                )
            )
        )
        every { balanceService.deductBalance(userId, 27000) } just Runs
        every { inventoryReservationService.confirmReservations(userId, any()) } just Runs
        every { couponService.markCouponAsUsed(userCouponId, 1L) } just Runs
        every { cartService.clearCart(userId) } just Runs

        // When
        val response = createOrderUseCase.createOrder(userId, userCouponId, shippingAddress)

        // Then
        assertNotNull(response)
        assertEquals(1L, response.orderEntity.id)
        assertEquals(OrderStatus.PAYMENT_COMPLETED, response.orderEntity.orderStatus)
        assertEquals(1, response.orderItemEntities.size)
        assertEquals(30000, response.originalAmount)
        assertEquals(3000, response.discountAmount)
        assertEquals(27000, response.finalAmount)
        assertNotNull(response.couponInfo)
        assertEquals("10% 할인 쿠폰", response.couponInfo?.name)

        // Verify: 각 Service 메서드가 호출되었는지 검증
        verify(exactly = 1) { userService.validateUserExists(userId) }
        verify(exactly = 1) { cartService.getCartItemsWithProducts(userId) }
        verify(exactly = 1) { cartService.validateCartItems(any()) }
        verify(exactly = 1) { couponService.validateAndGetCoupon(userId, userCouponId) }
        verify(exactly = 1) { couponService.calculateDiscount(coupon, 30000) }
        verify(exactly = 1) { inventoryReservationService.validateReservations(userId, any()) }
        verify(exactly = 1) { inventoryService.reduceStockForOrder(any()) }
        verify(exactly = 1) { orderService.createOrderWithItems(any()) }
        verify(exactly = 1) { balanceService.deductBalance(userId, 27000) }
        verify(exactly = 1) { inventoryReservationService.confirmReservations(userId, any()) }
        verify(exactly = 1) { couponService.markCouponAsUsed(userCouponId, 1L) }
        verify(exactly = 1) { cartService.clearCart(userId) }
    }

    @Test
    @DisplayName("쿠폰을 사용하지 않고 주문 생성 성공 시_주문 정보를 반환해야 한다")
    fun `쿠폰을 사용하지 않고 주문 생성 성공 시_주문 정보를 반환해야 한다`() {
        // Given
        val userId = 123L
        val userCouponId: Long? = null
        val shippingAddress = "서울시 강남구 테헤란로 123"

        val cartItem = createCartItem(quantity = 2, price = 15000) // totalPrice = 30000
        val now = LocalDateTime.now()

        // Mock 설정: Service 호출
        every { userService.validateUserExists(userId) } just Runs
        every { cartService.getCartItemsWithProducts(userId) } returns listOf(cartItem)
        every { cartService.validateCartItems(any()) } just Runs
        every { inventoryReservationService.validateReservations(userId, any()) } just Runs
        every { inventoryService.reduceStockForOrder(any()) } just Runs
        every { orderService.createOrderWithItems(any()) } returns OrderService.OrderCreationResult(
            orderEntity = OrderEntity(
                id = 1L,
                userId = userId,
                totalAmount = 30000,
                discountAmount = 0,
                finalAmount = 30000,
                shippingAddress = shippingAddress,
                orderStatus = OrderStatus.PAYMENT_COMPLETED,
                orderedAt = now,
                updatedAt = now
            ),
            orderItemEntities = listOf(
                OrderItemEntity(
                    id = 1L,
                    orderId = 1L,
                    productOptionId = 1L,
                    quantity = 2,
                    unitPrice = 15000,
                    totalPrice = 30000
                )
            )
        )
        every { balanceService.deductBalance(userId, 30000) } just Runs
        every { inventoryReservationService.confirmReservations(userId, any()) } just Runs
        every { cartService.clearCart(userId) } just Runs

        // When
        val response = createOrderUseCase.createOrder(userId, userCouponId, shippingAddress)

        // Then
        assertNotNull(response)
        assertEquals(1L, response.orderEntity.id)
        assertEquals(OrderStatus.PAYMENT_COMPLETED, response.orderEntity.orderStatus)
        assertEquals(1, response.orderItemEntities.size)
        assertEquals(30000, response.originalAmount)
        assertEquals(0, response.discountAmount)
        assertEquals(30000, response.finalAmount)
        assertNull(response.couponInfo) // 쿠폰을 사용하지 않음

        // Verify: 쿠폰 관련 Service는 호출되지 않아야 함
        verify(exactly = 1) { userService.validateUserExists(userId) }
        verify(exactly = 1) { cartService.getCartItemsWithProducts(userId) }
        verify(exactly = 1) { cartService.validateCartItems(any()) }
        verify(exactly = 0) { couponService.validateAndGetCoupon(any(), any()) }
        verify(exactly = 0) { couponService.calculateDiscount(any(), any()) }
        verify(exactly = 1) { inventoryReservationService.validateReservations(userId, any()) }
        verify(exactly = 1) { inventoryService.reduceStockForOrder(any()) }
        verify(exactly = 1) { orderService.createOrderWithItems(any()) }
        verify(exactly = 1) { balanceService.deductBalance(userId, 30000) }
        verify(exactly = 1) { inventoryReservationService.confirmReservations(userId, any()) }
        verify(exactly = 0) { couponService.markCouponAsUsed(any(), any()) }
        verify(exactly = 1) { cartService.clearCart(userId) }
    }

    @Test
    @DisplayName("사용자 쿠폰을 찾을 수 없을 경우_UserCouponNotFoundException이 발생해야 한다")
    fun `사용자 쿠폰을 찾을 수 없을 경우_UserCouponNotFoundException이 발생해야 한다`() {
        // Given
        val userId = 123L
        val userCouponId = 999L
        val shippingAddress = "서울시 강남구 테헤란로 123"

        val cartItem = createCartItem()

        // Mock 설정: CouponService에서 예외 발생
        every { userService.validateUserExists(userId) } just Runs
        every { cartService.getCartItemsWithProducts(userId) } returns listOf(cartItem)
        every { cartService.validateCartItems(any()) } just Runs
        every { couponService.validateAndGetCoupon(userId, userCouponId) } throws UserCouponNotFoundException("사용자 쿠폰을 찾을 수 없습니다.")

        // When & Then
        val exception = assertThrows<UserCouponNotFoundException> {
            createOrderUseCase.createOrder(userId, userCouponId, shippingAddress)
        }

        assertTrue(exception.message!!.contains("사용자 쿠폰을 찾을 수 없습니다"))
    }

    @Test
    @DisplayName("쿠폰이 만료된 경우_UserCouponExpiredException이 발생해야 한다")
    fun `쿠폰이 만료된 경우_UserCouponExpiredException이 발생해야 한다`() {
        // Given
        val userId = 123L
        val userCouponId = 456L
        val shippingAddress = "서울시 강남구 테헤란로 123"

        val cartItem = createCartItem()

        // Mock 설정: CouponService에서 예외 발생
        every { userService.validateUserExists(userId) } just Runs
        every { cartService.getCartItemsWithProducts(userId) } returns listOf(cartItem)
        every { cartService.validateCartItems(any()) } just Runs
        every { couponService.validateAndGetCoupon(userId, userCouponId) } throws UserCouponExpiredException("쿠폰이 만료되었습니다.")

        // When & Then
        val exception = assertThrows<UserCouponExpiredException> {
            createOrderUseCase.createOrder(userId, userCouponId, shippingAddress)
        }

        assertTrue(exception.message!!.contains("쿠폰이 만료되었습니다"))
    }

    @Test
    @DisplayName("쿠폰이 이미 사용된 경우_UserCouponAlreadyUsedException이 발생해야 한다")
    fun `쿠폰이 이미 사용된 경우_UserCouponAlreadyUsedException이 발생해야 한다`() {
        // Given
        val userId = 123L
        val userCouponId = 456L
        val shippingAddress = "서울시 강남구 테헤란로 123"

        val cartItem = createCartItem()

        // Mock 설정: CouponService에서 예외 발생
        every { userService.validateUserExists(userId) } just Runs
        every { cartService.getCartItemsWithProducts(userId) } returns listOf(cartItem)
        every { cartService.validateCartItems(any()) } just Runs
        every { couponService.validateAndGetCoupon(userId, userCouponId) } throws UserCouponAlreadyUsedException("이미 사용된 쿠폰입니다.")

        // When & Then
        val exception = assertThrows<UserCouponAlreadyUsedException> {
            createOrderUseCase.createOrder(userId, userCouponId, shippingAddress)
        }

        assertTrue(exception.message!!.contains("이미 사용된 쿠폰입니다"))
    }

    @Test
    @DisplayName("최소 주문 금액 미달로 쿠폰을 사용할 수 없을 경우_InvalidCouponOrderAmountException이 발생해야 한다")
    fun `최소 주문 금액 미달로 쿠폰을 사용할 수 없을 경우_InvalidCouponOrderAmountException이 발생해야 한다`() {
        // Given
        val userId = 123L
        val userCouponId = 456L
        val shippingAddress = "서울시 강남구 테헤란로 123"

        val cartItem = createCartItem(quantity = 1, price = 5000) // totalPrice = 5000 (최소 주문 금액 미달)

        // Mock 설정: CouponService에서 예외 발생
        every { userService.validateUserExists(userId) } just Runs
        every { cartService.getCartItemsWithProducts(userId) } returns listOf(cartItem)
        every { cartService.validateCartItems(any()) } just Runs
        every { couponService.validateAndGetCoupon(userId, userCouponId) } throws InvalidCouponOrderAmountException("최소 주문 금액 10000원을 충족하지 못했습니다.")

        // When & Then
        val exception = assertThrows<InvalidCouponOrderAmountException> {
            createOrderUseCase.createOrder(userId, userCouponId, shippingAddress)
        }

        assertTrue(exception.message!!.contains("최소 주문 금액"))
    }

    @Test
    @DisplayName("재고 예약을 찾을 수 없을 경우_InventoryReservationNotFoundException이 발생해야 한다")
    fun `재고 예약을 찾을 수 없을 경우_InventoryReservationNotFoundException이 발생해야 한다`() {
        // Given
        val userId = 123L
        val shippingAddress = "서울시 강남구 테헤란로 123"

        val cartItem = createCartItem()

        // Mock 설정: InventoryReservationService에서 예외 발생
        every { userService.validateUserExists(userId) } just Runs
        every { cartService.getCartItemsWithProducts(userId) } returns listOf(cartItem)
        every { cartService.validateCartItems(any()) } just Runs
        every { inventoryReservationService.validateReservations(userId, any()) } throws InventoryReservationNotFoundException("재고 예약을 찾을 수 없습니다.")

        // When & Then
        val exception = assertThrows<InventoryReservationNotFoundException> {
            createOrderUseCase.createOrder(userId, null, shippingAddress)
        }

        assertTrue(exception.message!!.contains("재고 예약을 찾을 수 없습니다"))
    }

    @Test
    @DisplayName("재고 예약이 만료된 경우_InventoryReservationExpiredException이 발생해야 한다")
    fun `재고 예약이 만료된 경우_InventoryReservationExpiredException이 발생해야 한다`() {
        // Given
        val userId = 123L
        val shippingAddress = "서울시 강남구 테헤란로 123"

        val cartItem = createCartItem()

        // Mock 설정: InventoryReservationService에서 예외 발생
        every { userService.validateUserExists(userId) } just Runs
        every { cartService.getCartItemsWithProducts(userId) } returns listOf(cartItem)
        every { cartService.validateCartItems(any()) } just Runs
        every { inventoryReservationService.validateReservations(userId, any()) } throws InventoryReservationExpiredException("재고 예약이 만료되었습니다.")

        // When & Then
        val exception = assertThrows<InventoryReservationExpiredException> {
            createOrderUseCase.createOrder(userId, null, shippingAddress)
        }

        assertTrue(exception.message!!.contains("재고 예약이 만료되었습니다"))
    }

    @Test
    @DisplayName("사용자 잔액이 부족한 경우_InsufficientBalanceException이 발생해야 한다")
    fun `사용자 잔액이 부족한 경우_InsufficientBalanceException이 발생해야 한다`() {
        // Given
        val userId = 123L
        val shippingAddress = "서울시 강남구 테헤란로 123"

        val cartItem = createCartItem(quantity = 2, price = 15000) // totalPrice = 30000
        val now = LocalDateTime.now()

        // Mock 설정: BalanceService에서 예외 발생
        every { userService.validateUserExists(userId) } just Runs
        every { cartService.getCartItemsWithProducts(userId) } returns listOf(cartItem)
        every { cartService.validateCartItems(any()) } just Runs
        every { inventoryReservationService.validateReservations(userId, any()) } just Runs
        every { inventoryService.reduceStockForOrder(any()) } just Runs
        every { orderService.createOrderWithItems(any()) } returns OrderService.OrderCreationResult(
            orderEntity = OrderEntity(
                id = 1L,
                userId = userId,
                totalAmount = 30000,
                discountAmount = 0,
                finalAmount = 30000,
                shippingAddress = shippingAddress,
                orderStatus = OrderStatus.PAYMENT_COMPLETED,
                orderedAt = now,
                updatedAt = now
            ),
            orderItemEntities = listOf(
                OrderItemEntity(
                    id = 1L,
                    orderId = 1L,
                    productOptionId = 1L,
                    quantity = 2,
                    unitPrice = 15000,
                    totalPrice = 30000
                )
            )
        )
        every { balanceService.deductBalance(userId, 30000) } throws InsufficientBalanceException("사용자 잔액이 부족합니다.")

        // When & Then
        val exception = assertThrows<InsufficientBalanceException> {
            createOrderUseCase.createOrder(userId, null, shippingAddress)
        }

        assertTrue(exception.message!!.contains("사용자 잔액이 부족합니다"))
    }

    @Test
    @DisplayName("비활성화된 상품 옵션이 포함된 경우_ProductOptionInactiveException이 발생해야 한다")
    fun `비활성화된 상품 옵션이 포함된 경우_ProductOptionInactiveException이 발생해야 한다`() {
        // Given
        val userId = 123L
        val shippingAddress = "서울시 강남구 테헤란로 123"

        val cartItem = createCartItem()

        // Mock 설정: CartService에서 예외 발생
        every { userService.validateUserExists(userId) } just Runs
        every { cartService.getCartItemsWithProducts(userId) } returns listOf(cartItem)
        every { cartService.validateCartItems(any()) } throws ProductOptionInactiveException("비활성화된 상품 옵션이 포함되어 있습니다.")

        // When & Then
        val exception = assertThrows<ProductOptionInactiveException> {
            createOrderUseCase.createOrder(userId, null, shippingAddress)
        }

        assertTrue(exception.message!!.contains("비활성화된 상품 옵션이 포함되어 있습니다"))
    }
}
