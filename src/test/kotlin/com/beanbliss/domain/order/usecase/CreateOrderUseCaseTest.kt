package com.beanbliss.domain.order.usecase

import com.beanbliss.domain.cart.dto.CartItemResponse
import com.beanbliss.domain.cart.repository.CartItemRepository
import com.beanbliss.domain.coupon.entity.CouponEntity
import com.beanbliss.domain.coupon.entity.UserCouponEntity
import com.beanbliss.domain.coupon.enums.UserCouponStatus
import com.beanbliss.domain.coupon.repository.CouponRepository
import com.beanbliss.domain.coupon.repository.UserCouponRepository
import com.beanbliss.domain.inventory.entity.InventoryEntity
import com.beanbliss.domain.inventory.entity.InventoryReservationEntity
import com.beanbliss.domain.inventory.entity.InventoryReservationStatus
import com.beanbliss.domain.inventory.repository.InventoryRepository
import com.beanbliss.domain.inventory.repository.InventoryReservationRepository
import com.beanbliss.domain.order.entity.OrderEntity
import com.beanbliss.domain.order.entity.OrderItemEntity
import com.beanbliss.domain.order.entity.OrderStatus
import com.beanbliss.domain.order.exception.*
import com.beanbliss.domain.order.repository.OrderItemRepository
import com.beanbliss.domain.order.repository.OrderRepository
import com.beanbliss.domain.product.entity.ProductEntity
import com.beanbliss.domain.product.entity.ProductOptionEntity
import com.beanbliss.domain.product.repository.ProductOptionDetail
import com.beanbliss.domain.product.repository.ProductOptionRepository
import com.beanbliss.domain.user.entity.BalanceEntity
import com.beanbliss.domain.user.repository.BalanceRepository
import io.mockk.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.LocalDateTime

/**
 * [책임]: CreateOrderUseCase의 비즈니스 로직 검증
 * - 쿠폰 검증 로직
 * - 재고 예약 검증 로직
 * - 재고 차감 로직
 * - 잔액 차감 로직
 * - 주문 생성 로직
 */
@DisplayName("주문 생성 UseCase 테스트")
class CreateOrderUseCaseTest {

    private lateinit var createOrderUseCase: CreateOrderUseCase
    private lateinit var cartItemRepository: CartItemRepository
    private lateinit var productOptionRepository: ProductOptionRepository
    private lateinit var inventoryReservationRepository: InventoryReservationRepository
    private lateinit var inventoryRepository: InventoryRepository
    private lateinit var userCouponRepository: UserCouponRepository
    private lateinit var couponRepository: CouponRepository
    private lateinit var balanceRepository: BalanceRepository
    private lateinit var orderRepository: OrderRepository
    private lateinit var orderItemRepository: OrderItemRepository

    @BeforeEach
    fun setUp() {
        // Mock 객체 생성
        cartItemRepository = mockk(relaxed = true)
        productOptionRepository = mockk(relaxed = true)
        inventoryReservationRepository = mockk(relaxed = true)
        inventoryRepository = mockk(relaxed = true)
        userCouponRepository = mockk(relaxed = true)
        couponRepository = mockk(relaxed = true)
        balanceRepository = mockk(relaxed = true)
        orderRepository = mockk(relaxed = true)
        orderItemRepository = mockk(relaxed = true)

        createOrderUseCase = CreateOrderUseCaseImpl(
            cartItemRepository = cartItemRepository,
            productOptionRepository = productOptionRepository,
            inventoryReservationRepository = inventoryReservationRepository,
            inventoryRepository = inventoryRepository,
            userCouponRepository = userCouponRepository,
            couponRepository = couponRepository,
            balanceRepository = balanceRepository,
            orderRepository = orderRepository,
            orderItemRepository = orderItemRepository
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

    private fun createProductOptionWithProduct(
        productOptionId: Long = 1L,
        isActive: Boolean = true
    ): ProductOptionDetail {
        return ProductOptionDetail(
            optionId = productOptionId,
            productId = 1L,
            productName = "에티오피아 예가체프",
            optionCode = "ETH-YRG-WH-200g",
            origin = "Ethiopia",
            grindType = "WHOLE_BEAN",
            weightGrams = 200,
            price = 15000,
            isActive = isActive
        )
    }

    private fun createUserCoupon(
        userCouponId: Long = 456L,
        userId: Long = 123L,
        couponId: Long = 789L,
        status: UserCouponStatus = UserCouponStatus.ISSUED
    ): UserCouponEntity {
        val now = LocalDateTime.now()
        return UserCouponEntity(
            id = userCouponId,
            userId = userId,
            couponId = couponId,
            status = status,
            usedAt = null,
            usedOrderId = null,
            createdAt = now.minusDays(1),
            updatedAt = now.minusDays(1)
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
    ): CouponEntity {
        val now = LocalDateTime.now()
        return CouponEntity(
            id = couponId,
            name = "10% 할인 쿠폰",
            discountType = discountType,
            discountValue = discountValue,
            minOrderAmount = minOrderAmount,
            maxDiscountAmount = maxDiscountAmount,
            totalQuantity = totalQuantity,
            validFrom = validFrom,
            validUntil = validUntil,
            createdAt = now.minusDays(7),
            updatedAt = now.minusDays(7)
        )
    }

    private fun createBalance(
        userId: Long = 123L,
        amount: Int = 100000
    ): BalanceEntity {
        val now = LocalDateTime.now()
        return BalanceEntity(
            id = 1L,
            userId = userId,
            amount = amount,
            createdAt = now.minusDays(30),
            updatedAt = now
        )
    }

    private fun createInventoryReservation(
        userId: Long = 123L,
        productOptionId: Long = 1L,
        quantity: Int = 2,
        status: InventoryReservationStatus = InventoryReservationStatus.RESERVED,
        expiresAt: LocalDateTime = LocalDateTime.now().plusMinutes(10)
    ): InventoryReservationEntity {
        val now = LocalDateTime.now()
        return InventoryReservationEntity(
            id = 1L,
            productOptionId = productOptionId,
            userId = userId,
            quantity = quantity,
            status = status,
            reservedAt = now,
            expiresAt = expiresAt,
            updatedAt = now
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
        val productOption = createProductOptionWithProduct()
        val userCoupon = createUserCoupon(userCouponId = userCouponId, userId = userId)
        val coupon = createCoupon(discountValue = 10, minOrderAmount = 10000) // 10% 할인
        val balance = createBalance(amount = 100000)
        val reservation = createInventoryReservation(userId = userId)

        // Mock 설정
        every { cartItemRepository.findByUserId(userId) } returns listOf(cartItem)
        every { productOptionRepository.findActiveOptionWithProduct(1L) } returns productOption
        every { userCouponRepository.findById(userCouponId) } returns userCoupon
        every { couponRepository.findById(coupon.id!!) } returns coupon
        every { balanceRepository.findByUserId(userId) } returns balance
        every { inventoryReservationRepository.findActiveReservationsByUserId(userId) } returns listOf(reservation)
        every { inventoryRepository.calculateAvailableStock(1L) } returns 100

        val savedOrder = OrderEntity(
            id = 1L,
            userId = userId,
            totalAmount = 30000,
            discountAmount = 3000, // 10% of 30000
            finalAmount = 27000,
            shippingAddress = shippingAddress,
            orderStatus = OrderStatus.PAYMENT_COMPLETED,
            orderedAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now()
        )
        every { orderRepository.save(any()) } returns savedOrder

        val savedOrderItems = listOf(
            OrderItemEntity(
                id = 1L,
                orderId = 1L,
                productOptionId = 1L,
                quantity = 2,
                unitPrice = 15000,
                totalPrice = 30000
            )
        )
        every { orderItemRepository.saveAll(any()) } returns savedOrderItems
        every { inventoryReservationRepository.save(any()) } returns reservation
        every { userCouponRepository.save(any(), any()) } returns userCoupon
        every { cartItemRepository.deleteByUserId(userId) } just Runs

        // When
        val response = createOrderUseCase.createOrder(userId, userCouponId, shippingAddress)

        // Then
        assertNotNull(response)
        assertEquals(1L, response.orderId)
        assertEquals(OrderStatus.PAYMENT_COMPLETED, response.orderStatus)
        assertEquals(1, response.orderItems.size)
        assertEquals(30000, response.priceInfo.totalProductAmount)
        assertEquals(3000, response.priceInfo.discountAmount)
        assertEquals(27000, response.priceInfo.finalAmount)
        assertNotNull(response.appliedCoupon)
        assertEquals("10% 할인 쿠폰", response.appliedCoupon?.couponName)

        // Verify
        verify(exactly = 1) { cartItemRepository.findByUserId(userId) }
        verify(exactly = 1) { orderRepository.save(any()) }
        verify(exactly = 1) { orderItemRepository.saveAll(any()) }
        verify(exactly = 1) { cartItemRepository.deleteByUserId(userId) }
    }

    @Test
    @DisplayName("쿠폰을 사용하지 않고 주문 생성 성공 시_주문 정보를 반환해야 한다")
    fun `쿠폰을 사용하지 않고 주문 생성 성공 시_주문 정보를 반환해야 한다`() {
        // Given
        val userId = 123L
        val userCouponId: Long? = null
        val shippingAddress = "서울시 강남구 테헤란로 123"

        val cartItem = createCartItem(quantity = 2, price = 15000) // totalPrice = 30000
        val productOption = createProductOptionWithProduct()
        val balance = createBalance(amount = 100000)
        val reservation = createInventoryReservation(userId = userId)

        // Mock 설정
        every { cartItemRepository.findByUserId(userId) } returns listOf(cartItem)
        every { productOptionRepository.findActiveOptionWithProduct(1L) } returns productOption
        every { balanceRepository.findByUserId(userId) } returns balance
        every { inventoryReservationRepository.findActiveReservationsByUserId(userId) } returns listOf(reservation)
        every { inventoryRepository.calculateAvailableStock(1L) } returns 100

        val savedOrder = OrderEntity(
            id = 1L,
            userId = userId,
            totalAmount = 30000,
            discountAmount = 0,
            finalAmount = 30000,
            shippingAddress = shippingAddress,
            orderStatus = OrderStatus.PAYMENT_COMPLETED,
            orderedAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now()
        )
        every { orderRepository.save(any()) } returns savedOrder

        val savedOrderItems = listOf(
            OrderItemEntity(
                id = 1L,
                orderId = 1L,
                productOptionId = 1L,
                quantity = 2,
                unitPrice = 15000,
                totalPrice = 30000
            )
        )
        every { orderItemRepository.saveAll(any()) } returns savedOrderItems
        every { inventoryReservationRepository.save(any()) } returns reservation
        every { cartItemRepository.deleteByUserId(userId) } just Runs

        // When
        val response = createOrderUseCase.createOrder(userId, userCouponId, shippingAddress)

        // Then
        assertNotNull(response)
        assertEquals(1L, response.orderId)
        assertEquals(OrderStatus.PAYMENT_COMPLETED, response.orderStatus)
        assertEquals(1, response.orderItems.size)
        assertEquals(30000, response.priceInfo.totalProductAmount)
        assertEquals(0, response.priceInfo.discountAmount)
        assertEquals(30000, response.priceInfo.finalAmount)
        assertNull(response.appliedCoupon) // 쿠폰을 사용하지 않음

        // Verify
        verify(exactly = 1) { cartItemRepository.findByUserId(userId) }
        verify(exactly = 1) { orderRepository.save(any()) }
        verify(exactly = 1) { orderItemRepository.saveAll(any()) }
        verify(exactly = 1) { cartItemRepository.deleteByUserId(userId) }
        verify(exactly = 0) { userCouponRepository.findById(any()) } // 쿠폰 조회하지 않음
    }

    @Test
    @DisplayName("사용자 쿠폰을 찾을 수 없을 경우_UserCouponNotFoundException이 발생해야 한다")
    fun `사용자 쿠폰을 찾을 수 없을 경우_UserCouponNotFoundException이 발생해야 한다`() {
        // Given
        val userId = 123L
        val userCouponId = 999L
        val shippingAddress = "서울시 강남구 테헤란로 123"

        val cartItem = createCartItem()
        val productOption = createProductOptionWithProduct()

        // Mock 설정: 쿠폰을 찾을 수 없음
        every { cartItemRepository.findByUserId(userId) } returns listOf(cartItem)
        every { productOptionRepository.findActiveOptionWithProduct(1L) } returns productOption
        every { userCouponRepository.findById(userCouponId) } returns null

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
        val productOption = createProductOptionWithProduct()
        val userCoupon = createUserCoupon(userCouponId = userCouponId, userId = userId)
        val expiredCoupon = createCoupon(
            validFrom = LocalDateTime.now().minusDays(30),
            validUntil = LocalDateTime.now().minusDays(1) // 만료됨
        )

        // Mock 설정
        every { cartItemRepository.findByUserId(userId) } returns listOf(cartItem)
        every { productOptionRepository.findActiveOptionWithProduct(1L) } returns productOption
        every { userCouponRepository.findById(userCouponId) } returns userCoupon
        every { couponRepository.findById(userCoupon.couponId) } returns expiredCoupon

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
        val productOption = createProductOptionWithProduct()
        val usedCoupon = createUserCoupon(
            userCouponId = userCouponId,
            userId = userId,
            status = UserCouponStatus.USED // 이미 사용됨
        )

        // Mock 설정
        every { cartItemRepository.findByUserId(userId) } returns listOf(cartItem)
        every { productOptionRepository.findActiveOptionWithProduct(1L) } returns productOption
        every { userCouponRepository.findById(userCouponId) } returns usedCoupon

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
        val productOption = createProductOptionWithProduct()
        val userCoupon = createUserCoupon(userCouponId = userCouponId, userId = userId)
        val coupon = createCoupon(minOrderAmount = 10000) // 최소 주문 금액 10000원

        // Mock 설정
        every { cartItemRepository.findByUserId(userId) } returns listOf(cartItem)
        every { productOptionRepository.findActiveOptionWithProduct(1L) } returns productOption
        every { userCouponRepository.findById(userCouponId) } returns userCoupon
        every { couponRepository.findById(userCoupon.couponId) } returns coupon

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
        val productOption = createProductOptionWithProduct()
        val balance = createBalance(amount = 100000)

        // Mock 설정: 재고 예약을 찾을 수 없음
        every { cartItemRepository.findByUserId(userId) } returns listOf(cartItem)
        every { productOptionRepository.findActiveOptionWithProduct(1L) } returns productOption
        every { balanceRepository.findByUserId(userId) } returns balance
        every { inventoryReservationRepository.findActiveReservationsByUserId(userId) } returns emptyList() // 예약 없음

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
        val productOption = createProductOptionWithProduct()
        val balance = createBalance(amount = 100000)
        val expiredReservation = createInventoryReservation(
            userId = userId,
            expiresAt = LocalDateTime.now().minusMinutes(1) // 만료됨
        )

        // Mock 설정
        every { cartItemRepository.findByUserId(userId) } returns listOf(cartItem)
        every { productOptionRepository.findActiveOptionWithProduct(1L) } returns productOption
        every { balanceRepository.findByUserId(userId) } returns balance
        every { inventoryReservationRepository.findActiveReservationsByUserId(userId) } returns listOf(expiredReservation)

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
        val productOption = createProductOptionWithProduct()
        val balance = createBalance(amount = 20000) // 부족한 잔액

        // Mock 설정
        every { cartItemRepository.findByUserId(userId) } returns listOf(cartItem)
        every { productOptionRepository.findActiveOptionWithProduct(1L) } returns productOption
        every { balanceRepository.findByUserId(userId) } returns balance

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
        val inactiveProductOption = createProductOptionWithProduct(isActive = false) // 비활성화됨

        // Mock 설정
        every { cartItemRepository.findByUserId(userId) } returns listOf(cartItem)
        every { productOptionRepository.findActiveOptionWithProduct(1L) } returns inactiveProductOption

        // When & Then
        val exception = assertThrows<ProductOptionInactiveException> {
            createOrderUseCase.createOrder(userId, null, shippingAddress)
        }

        assertTrue(exception.message!!.contains("비활성화된 상품 옵션이 포함되어 있습니다"))
    }
}
