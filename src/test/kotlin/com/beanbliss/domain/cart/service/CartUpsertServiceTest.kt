package com.beanbliss.domain.cart.service

import com.beanbliss.domain.cart.dto.CartItemResponse
import com.beanbliss.domain.cart.repository.CartItemRepository
import com.beanbliss.domain.product.repository.ProductOptionDetail
import com.beanbliss.common.exception.InvalidParameterException
import io.mockk.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.DisplayName
import java.time.LocalDateTime

/**
 * CartService의 장바구니 Upsert 비즈니스 로직을 검증하는 테스트
 *
 * [검증 목표]:
 * 1. 신규 장바구니 아이템 추가 시 CartItemRepository.save가 호출되는가?
 * 2. 기존 장바구니 아이템 수량 증가 시 CartItemRepository.updateQuantity가 호출되는가?
 * 3. 최대 수량(999개) 초과 시 InvalidParameterException이 발생하는가?
 * 4. 신규/기존 아이템 여부가 올바르게 반환되는가?
 *
 * [관련 API]:
 * - POST /api/cart/items
 */
@DisplayName("장바구니 Upsert Service 테스트")
class CartUpsertServiceTest {

    // Mock 객체 (Repository Interface에 의존)
    private val cartItemRepository: CartItemRepository = mockk()

    // 테스트 대상 (Service 인터페이스로 선언)
    private lateinit var cartService: CartService

    @BeforeEach
    fun setUp() {
        cartService = CartServiceImpl(cartItemRepository)
    }

    @Test
    @DisplayName("신규 장바구니 아이템 추가 시 Repository의 save가 호출되어야 한다")
    fun `신규 장바구니 아이템 추가 시 Repository의 save가 호출되어야 한다`() {
        // Given
        val userId = 1L
        val productOption = createMockProductOption(1L, "ETH-HD-200")
        val quantity = 2

        val savedCartItem = createMockCartItem(100L, 1L, 2)

        every { cartItemRepository.findByUserIdAndProductOptionId(userId, 1L) } returns null
        every { cartItemRepository.save(any(), userId) } returns savedCartItem

        // When
        val result = cartService.upsertCartItem(userId, productOption, quantity)

        // Then
        // [비즈니스 로직 검증]: 신규 아이템이 생성되었는가?
        assertNotNull(result)
        assertTrue(result.isNewItem, "신규 아이템이므로 isNewItem은 true여야 함")
        assertEquals(100L, result.cartItem.cartItemId)
        assertEquals(2, result.cartItem.quantity)

        // [Repository 호출 검증]: save가 정확히 한 번 호출되어야 함
        verify(exactly = 1) { cartItemRepository.findByUserIdAndProductOptionId(userId, 1L) }
        verify(exactly = 1) { cartItemRepository.save(any(), userId) }
        verify(exactly = 0) { cartItemRepository.updateQuantity(any(), any()) }
    }

    @Test
    @DisplayName("기존 장바구니 아이템 존재 시 수량이 증가되어야 한다")
    fun `기존 장바구니 아이템 존재 시 수량이 증가되어야 한다`() {
        // Given
        val userId = 1L
        val productOption = createMockProductOption(1L, "ETH-HD-200")
        val quantity = 2

        val existingCartItem = createMockCartItem(100L, 1L, 3) // 기존 수량 3
        val updatedCartItem = createMockCartItem(100L, 1L, 5) // 업데이트된 수량 5

        every { cartItemRepository.findByUserIdAndProductOptionId(userId, 1L) } returns existingCartItem
        every { cartItemRepository.updateQuantity(100L, 5) } returns updatedCartItem

        // When
        val result = cartService.upsertCartItem(userId, productOption, quantity)

        // Then
        // [비즈니스 로직 검증]: 기존 수량에 새 수량이 합산되었는가?
        assertFalse(result.isNewItem, "기존 아이템 수량 증가이므로 isNewItem은 false여야 함")
        assertEquals(5, result.cartItem.quantity, "기존 수량 3 + 추가 수량 2 = 5")

        // [Repository 호출 검증]: updateQuantity가 호출되고 save는 호출되지 않아야 함
        verify(exactly = 1) { cartItemRepository.findByUserIdAndProductOptionId(userId, 1L) }
        verify(exactly = 1) { cartItemRepository.updateQuantity(100L, 5) }
        verify(exactly = 0) { cartItemRepository.save(any(), any()) }
    }

    @Test
    @DisplayName("신규 추가 시 수량이 999를 초과하면 InvalidParameterException이 발생해야 한다")
    fun `신규 추가 시 수량이 999를 초과하면 InvalidParameterException이 발생해야 한다`() {
        // Given
        val userId = 1L
        val productOption = createMockProductOption(1L, "ETH-HD-200")
        val quantity = 1000 // 999 초과

        every { cartItemRepository.findByUserIdAndProductOptionId(userId, 1L) } returns null

        // When & Then
        // [비즈니스 로직 검증]: 최대 수량 초과 시 적절한 예외가 발생하는가?
        val exception = assertThrows(InvalidParameterException::class.java) {
            cartService.upsertCartItem(userId, productOption, quantity)
        }

        assertEquals("장바구니 내 동일 옵션의 최대 수량은 999개입니다.", exception.message)

        // [Repository 호출 검증]: 예외 발생 시 save는 호출되지 않아야 함
        verify(exactly = 1) { cartItemRepository.findByUserIdAndProductOptionId(userId, 1L) }
        verify(exactly = 0) { cartItemRepository.save(any(), any()) }
    }

    @Test
    @DisplayName("기존 아이템 수량 증가 시 합계가 999를 초과하면 InvalidParameterException이 발생해야 한다")
    fun `기존 아이템 수량 증가 시 합계가 999를 초과하면 InvalidParameterException이 발생해야 한다`() {
        // Given
        val userId = 1L
        val productOption = createMockProductOption(1L, "ETH-HD-200")
        val quantity = 100 // 기존 950 + 100 = 1050 > 999

        val existingCartItem = createMockCartItem(100L, 1L, 950) // 기존 수량 950

        every { cartItemRepository.findByUserIdAndProductOptionId(userId, 1L) } returns existingCartItem

        // When & Then
        // [비즈니스 로직 검증]: 최대 수량 초과 시 적절한 예외가 발생하는가?
        val exception = assertThrows(InvalidParameterException::class.java) {
            cartService.upsertCartItem(userId, productOption, quantity)
        }

        assertEquals("장바구니 내 동일 옵션의 최대 수량은 999개입니다.", exception.message)

        // [Repository 호출 검증]: 예외 발생 시 updateQuantity는 호출되지 않아야 함
        verify(exactly = 1) { cartItemRepository.findByUserIdAndProductOptionId(userId, 1L) }
        verify(exactly = 0) { cartItemRepository.updateQuantity(any(), any()) }
    }

    @Test
    @DisplayName("수량 999 정확히 일치 시 정상 처리되어야 한다")
    fun `수량 999 정확히 일치 시 정상 처리되어야 한다`() {
        // Given
        val userId = 1L
        val productOption = createMockProductOption(1L, "ETH-HD-200")
        val quantity = 999 // 정확히 999

        val savedCartItem = createMockCartItem(100L, 1L, 999)

        every { cartItemRepository.findByUserIdAndProductOptionId(userId, 1L) } returns null
        every { cartItemRepository.save(any(), userId) } returns savedCartItem

        // When
        val result = cartService.upsertCartItem(userId, productOption, quantity)

        // Then
        // [경계값 검증]: 999는 유효한 수량이므로 정상 처리되어야 함
        assertTrue(result.isNewItem)
        assertEquals(999, result.cartItem.quantity)

        verify(exactly = 1) { cartItemRepository.save(any(), userId) }
    }

    @Test
    @DisplayName("기존 수량 + 추가 수량이 정확히 999일 때 정상 처리되어야 한다")
    fun `기존 수량 + 추가 수량이 정확히 999일 때 정상 처리되어야 한다`() {
        // Given
        val userId = 1L
        val productOption = createMockProductOption(1L, "ETH-HD-200")
        val quantity = 99 // 기존 900 + 99 = 999

        val existingCartItem = createMockCartItem(100L, 1L, 900)
        val updatedCartItem = createMockCartItem(100L, 1L, 999)

        every { cartItemRepository.findByUserIdAndProductOptionId(userId, 1L) } returns existingCartItem
        every { cartItemRepository.updateQuantity(100L, 999) } returns updatedCartItem

        // When
        val result = cartService.upsertCartItem(userId, productOption, quantity)

        // Then
        // [경계값 검증]: 합계 999는 유효하므로 정상 처리되어야 함
        assertFalse(result.isNewItem)
        assertEquals(999, result.cartItem.quantity)

        verify(exactly = 1) { cartItemRepository.updateQuantity(100L, 999) }
    }

    @Test
    @DisplayName("ProductOption 정보가 CartItem 생성 시 올바르게 매핑되어야 한다")
    fun `ProductOption 정보가 CartItem 생성 시 올바르게 매핑되어야 한다`() {
        // Given
        val userId = 1L
        val productOption = ProductOptionDetail(
            optionId = 10L,
            productId = 5L,
            productName = "콜롬비아 수프리모",
            optionCode = "COL-ESP-500",
            origin = "Colombia",
            grindType = "ESPRESSO",
            weightGrams = 500,
            price = 35000,
            isActive = true
        )
        val quantity = 3

        val savedCartItem = CartItemResponse(
            cartItemId = 200L,
            productOptionId = 10L,
            productName = "콜롬비아 수프리모",
            optionCode = "COL-ESP-500",
            origin = "Colombia",
            grindType = "ESPRESSO",
            weightGrams = 500,
            price = 35000,
            quantity = 3,
            totalPrice = 105000,
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now()
        )

        every { cartItemRepository.findByUserIdAndProductOptionId(userId, 10L) } returns null
        every { cartItemRepository.save(any(), userId) } returns savedCartItem

        // When
        val result = cartService.upsertCartItem(userId, productOption, quantity)

        // Then
        // [데이터 매핑 검증]: ProductOption 정보가 CartItem에 올바르게 복사되었는가?
        assertEquals(10L, result.cartItem.productOptionId)
        assertEquals("콜롬비아 수프리모", result.cartItem.productName)
        assertEquals("COL-ESP-500", result.cartItem.optionCode)
        assertEquals("Colombia", result.cartItem.origin)
        assertEquals("ESPRESSO", result.cartItem.grindType)
        assertEquals(500, result.cartItem.weightGrams)
        assertEquals(35000, result.cartItem.price)
        assertEquals(105000, result.cartItem.totalPrice) // 35000 * 3
    }

    // === Helper Methods ===

    /**
     * 테스트용 Mock ProductOption 생성
     */
    private fun createMockProductOption(
        optionId: Long,
        optionCode: String
    ): ProductOptionDetail {
        return ProductOptionDetail(
            optionId = optionId,
            productId = 1L,
            productName = "에티오피아 예가체프 G1",
            optionCode = optionCode,
            origin = "Ethiopia",
            grindType = "HAND_DRIP",
            weightGrams = 200,
            price = 21000,
            isActive = true
        )
    }

    /**
     * 테스트용 Mock CartItem 생성
     */
    private fun createMockCartItem(
        cartItemId: Long,
        productOptionId: Long,
        quantity: Int
    ): CartItemResponse {
        return CartItemResponse(
            cartItemId = cartItemId,
            productOptionId = productOptionId,
            productName = "에티오피아 예가체프 G1",
            optionCode = "ETH-HD-200",
            origin = "Ethiopia",
            grindType = "HAND_DRIP",
            weightGrams = 200,
            price = 21000,
            quantity = quantity,
            totalPrice = 21000 * quantity,
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now()
        )
    }
}
