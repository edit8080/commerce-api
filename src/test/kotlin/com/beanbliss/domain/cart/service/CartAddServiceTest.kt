package com.beanbliss.domain.cart.service

import com.beanbliss.domain.cart.repository.CartItemRepository
import com.beanbliss.domain.cart.dto.AddToCartRequest
import com.beanbliss.domain.cart.dto.CartItemResponse
import com.beanbliss.domain.product.repository.ProductOptionRepository
import com.beanbliss.domain.user.repository.UserRepository
import com.beanbliss.common.exception.ResourceNotFoundException
import com.beanbliss.common.exception.InvalidParameterException
import io.mockk.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.DisplayName
import java.time.LocalDateTime

/**
 * 장바구니 추가 Service의 비즈니스 로직을 검증하는 테스트
 *
 * [검증 목표]:
 * 1. 신규 장바구니 아이템 추가 시 Repository가 올바르게 호출되는가?
 * 2. 기존 장바구니 아이템의 수량 증가 시 Repository가 올바르게 호출되는가?
 * 3. 존재하지 않는 사용자 ID 요청 시 적절한 예외가 발생하는가?
 * 4. 존재하지 않는 상품 옵션 ID 요청 시 적절한 예외가 발생하는가?
 * 5. 비활성 상태의 상품 옵션 추가 시 적절한 예외가 발생하는가?
 * 6. 최대 수량(999개)을 초과하는 요청 시 적절한 예외가 발생하는가?
 *
 * [관련 API]:
 * - POST /api/cart/items
 */
@DisplayName("장바구니 추가 Service 테스트")
class CartAddServiceTest {

    // Mock 객체 (Repository Interface에 의존)
    private val cartItemRepository: CartItemRepository = mockk()
    private val productOptionRepository: ProductOptionRepository = mockk()
    private val userRepository: UserRepository = mockk()

    // 테스트 대상 (Service 인터페이스로 선언)
    private lateinit var cartService: CartService

    @BeforeEach
    fun setUp() {
        cartService = CartServiceImpl(cartItemRepository, productOptionRepository, userRepository)
    }

    @Test
    @DisplayName("신규 장바구니 아이템 추가 시 Repository가 올바르게 호출되어야 한다")
    fun `신규 장바구니 아이템 추가 시 Repository가 올바르게 호출되어야 한다`() {
        // Given
        val request = AddToCartRequest(
            userId = 1L,
            productOptionId = 1L,
            quantity = 2
        )

        val mockProductOption = createMockProductOption(1L, "ETH-HD-200", true)
        val mockCartItem = createMockCartItem(100L, 1L, 1L, 2)

        every { userRepository.existsById(1L) } returns true
        every { productOptionRepository.findActiveOptionWithProduct(1L) } returns mockProductOption
        every { cartItemRepository.findByUserIdAndProductOptionId(1L, 1L) } returns null
        every { cartItemRepository.save(any(), 1L) } returns mockCartItem

        // When
        val result = cartService.addToCart(request)

        // Then
        // [비즈니스 로직 검증]: 신규 장바구니 아이템이 생성되었는가?
        assertNotNull(result)
        assertTrue(result.isNewItem, "신규 아이템이므로 isNewItem은 true여야 함")
        assertEquals(100L, result.cartItem.cartItemId)
        assertEquals(1L, result.cartItem.productOptionId)
        assertEquals(2, result.cartItem.quantity)

        // [Repository 호출 검증]: 각 Repository가 정확히 한 번씩 호출되어야 함
        verify(exactly = 1) { userRepository.existsById(1L) }
        verify(exactly = 1) { productOptionRepository.findActiveOptionWithProduct(1L) }
        verify(exactly = 1) { cartItemRepository.findByUserIdAndProductOptionId(1L, 1L) }
        verify(exactly = 1) { cartItemRepository.save(any(), 1L) }
    }

    @Test
    @DisplayName("기존 장바구니 아이템 존재 시 수량이 증가되어야 한다")
    fun `기존 장바구니 아이템 존재 시 수량이 증가되어야 한다`() {
        // Given
        val request = AddToCartRequest(
            userId = 1L,
            productOptionId = 1L,
            quantity = 2
        )

        val mockProductOption = createMockProductOption(1L, "ETH-HD-200", true)
        val existingCartItem = createMockCartItem(100L, 1L, 1L, 3) // 기존 수량 3
        val updatedCartItem = createMockCartItem(100L, 1L, 1L, 5) // 업데이트된 수량 5

        every { userRepository.existsById(1L) } returns true
        every { productOptionRepository.findActiveOptionWithProduct(1L) } returns mockProductOption
        every { cartItemRepository.findByUserIdAndProductOptionId(1L, 1L) } returns existingCartItem
        every { cartItemRepository.updateQuantity(100L, 5) } returns updatedCartItem

        // When
        val result = cartService.addToCart(request)

        // Then
        // [비즈니스 로직 검증]: 기존 수량에 새 수량이 합산되었는가?
        assertFalse(result.isNewItem, "기존 아이템 수량 증가이므로 isNewItem은 false여야 함")
        assertEquals(5, result.cartItem.quantity, "기존 수량 3 + 추가 수량 2 = 5")

        // [Repository 호출 검증]: save가 아닌 updateQuantity가 호출되어야 함
        verify(exactly = 1) { cartItemRepository.updateQuantity(100L, 5) }
        verify(exactly = 0) { cartItemRepository.save(any(), any()) }
    }

    @Test
    @DisplayName("존재하지 않는 사용자 ID 요청 시 ResourceNotFoundException이 발생해야 한다")
    fun `존재하지 않는 사용자 ID 요청 시 ResourceNotFoundException이 발생해야 한다`() {
        // Given
        val request = AddToCartRequest(
            userId = 999L,
            productOptionId = 1L,
            quantity = 2
        )

        every { userRepository.existsById(999L) } returns false

        // When & Then
        // [예외 처리 검증]: 존재하지 않는 사용자 ID 요청 시 적절한 예외가 발생하는가?
        val exception = assertThrows(ResourceNotFoundException::class.java) {
            cartService.addToCart(request)
        }

        assertEquals("사용자 ID: 999를 찾을 수 없습니다.", exception.message)

        // [Repository 호출 검증]: 사용자 검증 실패 시, 다른 Repository는 호출되지 않아야 함
        verify(exactly = 1) { userRepository.existsById(999L) }
        verify(exactly = 0) { productOptionRepository.findActiveOptionWithProduct(any()) }
        verify(exactly = 0) { cartItemRepository.save(any(), any()) }
    }

    @Test
    @DisplayName("존재하지 않는 상품 옵션 ID 요청 시 ResourceNotFoundException이 발생해야 한다")
    fun `존재하지 않는 상품 옵션 ID 요청 시 ResourceNotFoundException이 발생해야 한다`() {
        // Given
        val request = AddToCartRequest(
            userId = 1L,
            productOptionId = 999L,
            quantity = 2
        )

        every { userRepository.existsById(1L) } returns true
        every { productOptionRepository.findActiveOptionWithProduct(999L) } returns null

        // When & Then
        // [예외 처리 검증]: 존재하지 않는 상품 옵션 ID 요청 시 적절한 예외가 발생하는가?
        val exception = assertThrows(ResourceNotFoundException::class.java) {
            cartService.addToCart(request)
        }

        assertEquals("상품 옵션 ID: 999를 찾을 수 없습니다.", exception.message)

        // [Repository 호출 검증]: 상품 옵션 검증 실패 시, 장바구니 Repository는 호출되지 않아야 함
        verify(exactly = 1) { productOptionRepository.findActiveOptionWithProduct(999L) }
        verify(exactly = 0) { cartItemRepository.save(any(), any()) }
    }

    @Test
    @DisplayName("비활성 상태의 상품 옵션 추가 시 ResourceNotFoundException이 발생해야 한다")
    fun `비활성 상태의 상품 옵션 추가 시 ResourceNotFoundException이 발생해야 한다`() {
        // Given
        val request = AddToCartRequest(
            userId = 1L,
            productOptionId = 1L,
            quantity = 2
        )

        every { userRepository.existsById(1L) } returns true
        // Repository에서 비활성 옵션은 null 반환 (findActiveOptionWithProduct)
        every { productOptionRepository.findActiveOptionWithProduct(1L) } returns null

        // When & Then
        // [예외 처리 검증]: 비활성 상태의 옵션 추가 시 적절한 예외가 발생하는가?
        val exception = assertThrows(ResourceNotFoundException::class.java) {
            cartService.addToCart(request)
        }

        assertEquals("상품 옵션 ID: 1를 찾을 수 없습니다.", exception.message)

        // [Repository 호출 검증]: 비활성 옵션 검증 실패 시, 장바구니 Repository는 호출되지 않아야 함
        verify(exactly = 0) { cartItemRepository.save(any(), any()) }
    }

    @Test
    @DisplayName("최대 수량(999개) 초과 시 InvalidParameterException이 발생해야 한다")
    fun `최대 수량 999개 초과 시 InvalidParameterException이 발생해야 한다`() {
        // Given
        val request = AddToCartRequest(
            userId = 1L,
            productOptionId = 1L,
            quantity = 100 // 기존 950 + 100 = 1050 > 999
        )

        val mockProductOption = createMockProductOption(1L, "ETH-HD-200", true)
        val existingCartItem = createMockCartItem(100L, 1L, 1L, 950) // 기존 수량 950

        every { userRepository.existsById(1L) } returns true
        every { productOptionRepository.findActiveOptionWithProduct(1L) } returns mockProductOption
        every { cartItemRepository.findByUserIdAndProductOptionId(1L, 1L) } returns existingCartItem

        // When & Then
        // [비즈니스 로직 검증]: 최대 수량 초과 시 적절한 예외가 발생하는가?
        val exception = assertThrows(InvalidParameterException::class.java) {
            cartService.addToCart(request)
        }

        assertEquals("장바구니 내 동일 옵션의 최대 수량은 999개입니다.", exception.message)

        // [Repository 호출 검증]: 수량 검증 실패 시, updateQuantity는 호출되지 않아야 함
        verify(exactly = 0) { cartItemRepository.updateQuantity(any(), any()) }
        verify(exactly = 0) { cartItemRepository.save(any(), any()) }
    }

    @Test
    @DisplayName("신규 추가 시 최대 수량(999개) 초과하는 요청은 InvalidParameterException이 발생해야 한다")
    fun `신규 추가 시 최대 수량 999개 초과하는 요청은 InvalidParameterException이 발생해야 한다`() {
        // Given
        val request = AddToCartRequest(
            userId = 1L,
            productOptionId = 1L,
            quantity = 1000 // 1000 > 999
        )

        val mockProductOption = createMockProductOption(1L, "ETH-HD-200", true)

        every { userRepository.existsById(1L) } returns true
        every { productOptionRepository.findActiveOptionWithProduct(1L) } returns mockProductOption
        every { cartItemRepository.findByUserIdAndProductOptionId(1L, 1L) } returns null

        // When & Then
        // [비즈니스 로직 검증]: 단일 요청 수량이 999 초과 시 예외가 발생하는가?
        val exception = assertThrows(InvalidParameterException::class.java) {
            cartService.addToCart(request)
        }

        assertEquals("장바구니 내 동일 옵션의 최대 수량은 999개입니다.", exception.message)

        // [Repository 호출 검증]: 수량 검증 실패 시, save는 호출되지 않아야 함
        verify(exactly = 0) { cartItemRepository.save(any(), any()) }
    }

    // === Helper Methods ===

    /**
     * 테스트용 Mock ProductOption 생성
     */
    private fun createMockProductOption(
        optionId: Long,
        optionCode: String,
        isActive: Boolean
    ): com.beanbliss.domain.product.repository.ProductOptionDetail {
        return com.beanbliss.domain.product.repository.ProductOptionDetail(
            optionId = optionId,
            productId = 1L,
            productName = "에티오피아 예가체프 G1",
            optionCode = optionCode,
            origin = "Ethiopia",
            grindType = "HAND_DRIP",
            weightGrams = 200,
            price = 21000,
            isActive = isActive
        )
    }

    /**
     * 테스트용 Mock CartItem 생성
     */
    private fun createMockCartItem(
        cartItemId: Long,
        userId: Long,
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
