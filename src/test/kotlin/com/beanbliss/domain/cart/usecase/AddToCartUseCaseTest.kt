package com.beanbliss.domain.cart.usecase

import com.beanbliss.domain.cart.dto.AddToCartRequest
import com.beanbliss.domain.cart.repository.CartItemDetail
import com.beanbliss.domain.cart.service.CartService
import com.beanbliss.domain.cart.service.UpsertCartItemResult
import com.beanbliss.domain.product.repository.ProductOptionDetail
import com.beanbliss.domain.product.service.ProductOptionService
import com.beanbliss.domain.user.service.UserService
import com.beanbliss.common.exception.ResourceNotFoundException
import com.beanbliss.common.exception.InvalidParameterException
import io.mockk.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.DisplayName
import java.time.LocalDateTime

/**
 * 장바구니 추가 UseCase의 오케스트레이션 로직을 검증하는 테스트
 *
 * [검증 목표]:
 * 1. UseCase가 UserService, ProductOptionService, CartService를 올바른 순서로 호출하는가?
 * 2. 각 Service의 예외가 UseCase를 통해 올바르게 전파되는가?
 * 3. 신규 추가와 수량 증가 시 올바른 결과를 반환하는가?
 *
 * [관련 API]:
 * - POST /api/cart/items
 */
@DisplayName("장바구니 추가 UseCase 테스트")
class AddToCartUseCaseTest {

    // Mock 객체 (Service Interface에 의존)
    private val userService: UserService = mockk()
    private val productOptionService: ProductOptionService = mockk()
    private val cartService: CartService = mockk()

    // 테스트 대상
    private lateinit var addToCartUseCase: AddToCartUseCase

    @BeforeEach
    fun setUp() {
        addToCartUseCase = AddToCartUseCase(userService, productOptionService, cartService)
    }

    @Test
    @DisplayName("신규 장바구니 아이템 추가 시 각 Service가 올바른 순서로 호출되어야 한다")
    fun `신규 장바구니 아이템 추가 시 각 Service가 올바른 순서로 호출되어야 한다`() {
        // Given
        val request = AddToCartRequest(
            userId = 1L,
            productOptionId = 1L,
            quantity = 2
        )

        val mockProductOption = createMockProductOption(1L, "ETH-HD-200")
        val mockCartItem = createMockCartItem(100L, 1L, 2)

        every { userService.validateUserExists(1L) } just Runs
        every { productOptionService.getActiveOptionWithProduct(1L) } returns mockProductOption
        every { cartService.upsertCartItem(1L, mockProductOption, 2) } returns UpsertCartItemResult(
            cartItem = mockCartItem,
            isNewItem = true
        )

        // When
        val result = addToCartUseCase.addToCart(request)

        // Then
        // [오케스트레이션 검증]: 각 Service가 올바른 순서로 호출되었는가?
        verifyOrder {
            userService.validateUserExists(1L)
            productOptionService.getActiveOptionWithProduct(1L)
            cartService.upsertCartItem(1L, mockProductOption, 2)
        }

        // [비즈니스 로직 검증]: 신규 아이템이 올바르게 생성되었는가?
        assertNotNull(result)
        assertTrue(result.isNewItem, "신규 아이템이므로 isNewItem은 true여야 함")
        assertEquals(100L, result.cartItem.cartItemId)
        assertEquals(2, result.cartItem.quantity)

        verify(exactly = 1) { userService.validateUserExists(1L) }
        verify(exactly = 1) { productOptionService.getActiveOptionWithProduct(1L) }
        verify(exactly = 1) { cartService.upsertCartItem(1L, mockProductOption, 2) }
    }

    @Test
    @DisplayName("기존 장바구니 아이템 수량 증가 시 각 Service가 올바르게 호출되어야 한다")
    fun `기존 장바구니 아이템 수량 증가 시 각 Service가 올바르게 호출되어야 한다`() {
        // Given
        val request = AddToCartRequest(
            userId = 1L,
            productOptionId = 1L,
            quantity = 2
        )

        val mockProductOption = createMockProductOption(1L, "ETH-HD-200")
        val mockCartItem = createMockCartItem(100L, 1L, 5) // 기존 3 + 추가 2 = 5

        every { userService.validateUserExists(1L) } just Runs
        every { productOptionService.getActiveOptionWithProduct(1L) } returns mockProductOption
        every { cartService.upsertCartItem(1L, mockProductOption, 2) } returns UpsertCartItemResult(
            cartItem = mockCartItem,
            isNewItem = false
        )

        // When
        val result = addToCartUseCase.addToCart(request)

        // Then
        // [오케스트레이션 검증]: 각 Service가 올바른 순서로 호출되었는가?
        verifyOrder {
            userService.validateUserExists(1L)
            productOptionService.getActiveOptionWithProduct(1L)
            cartService.upsertCartItem(1L, mockProductOption, 2)
        }

        // [비즈니스 로직 검증]: 기존 아이템의 수량이 증가했는가?
        assertFalse(result.isNewItem, "기존 아이템 수량 증가이므로 isNewItem은 false여야 함")
        assertEquals(5, result.cartItem.quantity)

        verify(exactly = 1) { userService.validateUserExists(1L) }
        verify(exactly = 1) { productOptionService.getActiveOptionWithProduct(1L) }
        verify(exactly = 1) { cartService.upsertCartItem(1L, mockProductOption, 2) }
    }

    @Test
    @DisplayName("존재하지 않는 사용자 ID 요청 시 UserService에서 예외가 발생해야 한다")
    fun `존재하지 않는 사용자 ID 요청 시 UserService에서 예외가 발생해야 한다`() {
        // Given
        val request = AddToCartRequest(
            userId = 999L,
            productOptionId = 1L,
            quantity = 2
        )

        every { userService.validateUserExists(999L) } throws ResourceNotFoundException("사용자 ID: 999를 찾을 수 없습니다.")

        // When & Then
        // [예외 전파 검증]: UserService의 예외가 UseCase를 통해 올바르게 전파되는가?
        val exception = assertThrows(ResourceNotFoundException::class.java) {
            addToCartUseCase.addToCart(request)
        }

        assertEquals("사용자 ID: 999를 찾을 수 없습니다.", exception.message)

        // [오케스트레이션 검증]: 사용자 검증 실패 시, 이후 Service는 호출되지 않아야 함
        verify(exactly = 1) { userService.validateUserExists(999L) }
        verify(exactly = 0) { productOptionService.getActiveOptionWithProduct(any()) }
        verify(exactly = 0) { cartService.upsertCartItem(any(), any(), any()) }
    }

    @Test
    @DisplayName("존재하지 않는 상품 옵션 ID 요청 시 ProductOptionService에서 예외가 발생해야 한다")
    fun `존재하지 않는 상품 옵션 ID 요청 시 ProductOptionService에서 예외가 발생해야 한다`() {
        // Given
        val request = AddToCartRequest(
            userId = 1L,
            productOptionId = 999L,
            quantity = 2
        )

        every { userService.validateUserExists(1L) } just Runs
        every { productOptionService.getActiveOptionWithProduct(999L) } throws ResourceNotFoundException("상품 옵션 ID: 999를 찾을 수 없습니다.")

        // When & Then
        // [예외 전파 검증]: ProductOptionService의 예외가 UseCase를 통해 올바르게 전파되는가?
        val exception = assertThrows(ResourceNotFoundException::class.java) {
            addToCartUseCase.addToCart(request)
        }

        assertEquals("상품 옵션 ID: 999를 찾을 수 없습니다.", exception.message)

        // [오케스트레이션 검증]: 상품 옵션 검증 실패 시, CartService는 호출되지 않아야 함
        verify(exactly = 1) { userService.validateUserExists(1L) }
        verify(exactly = 1) { productOptionService.getActiveOptionWithProduct(999L) }
        verify(exactly = 0) { cartService.upsertCartItem(any(), any(), any()) }
    }

    @Test
    @DisplayName("최대 수량 초과 시 CartService에서 예외가 발생해야 한다")
    fun `최대 수량 초과 시 CartService에서 예외가 발생해야 한다`() {
        // Given
        val request = AddToCartRequest(
            userId = 1L,
            productOptionId = 1L,
            quantity = 100
        )

        val mockProductOption = createMockProductOption(1L, "ETH-HD-200")

        every { userService.validateUserExists(1L) } just Runs
        every { productOptionService.getActiveOptionWithProduct(1L) } returns mockProductOption
        every { cartService.upsertCartItem(1L, mockProductOption, 100) } throws InvalidParameterException("장바구니 내 동일 옵션의 최대 수량은 999개입니다.")

        // When & Then
        // [예외 전파 검증]: CartService의 예외가 UseCase를 통해 올바르게 전파되는가?
        val exception = assertThrows(InvalidParameterException::class.java) {
            addToCartUseCase.addToCart(request)
        }

        assertEquals("장바구니 내 동일 옵션의 최대 수량은 999개입니다.", exception.message)

        // [오케스트레이션 검증]: 모든 Service가 호출되었는가?
        verify(exactly = 1) { userService.validateUserExists(1L) }
        verify(exactly = 1) { productOptionService.getActiveOptionWithProduct(1L) }
        verify(exactly = 1) { cartService.upsertCartItem(1L, mockProductOption, 100) }
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
    ): CartItemDetail {
        return CartItemDetail(
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
