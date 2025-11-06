package com.beanbliss.domain.cart.service

import com.beanbliss.domain.cart.dto.CartItemResponse
import com.beanbliss.domain.cart.repository.CartItemRepository
import com.beanbliss.domain.order.exception.CartEmptyException
import io.mockk.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.LocalDateTime

/**
 * CartService의 장바구니 조회 비즈니스 로직을 검증하는 테스트
 *
 * [검증 목표]:
 * 1. 장바구니 조회 시 Repository가 올바르게 호출되는가?
 * 2. 장바구니가 비어 있을 경우 CartEmptyException이 발생하는가? (핵심 비즈니스 규칙)
 * 3. 정상 조회 시 장바구니 아이템 목록이 반환되는가?
 *
 * [관련 UseCase]:
 * - ReserveOrderUseCase
 */
@DisplayName("장바구니 조회 Service 테스트")
class CartGetServiceTest {

    // Mock 객체 (Repository Interface에 의존)
    private val cartItemRepository: CartItemRepository = mockk()
    private val productOptionRepository = mockk<com.beanbliss.domain.product.repository.ProductOptionRepository>()

    // 테스트 대상 (Service 인터페이스로 선언)
    private lateinit var cartService: CartService

    @BeforeEach
    fun setUp() {
        cartService = CartService(cartItemRepository, productOptionRepository)
    }

    @Test
    @DisplayName("정상 조회 시 Repository의 findByUserId가 호출되고 장바구니 아이템 목록이 반환되어야 한다")
    fun `정상 조회 시_Repository의 findByUserId가 호출되고_장바구니 아이템 목록이 반환되어야 한다`() {
        // Given
        val userId = 123L
        val now = LocalDateTime.now()

        val cartItems = listOf(
            CartItemResponse(
                cartItemId = 1L,
                productOptionId = 10L,
                productName = "에티오피아 예가체프 G1",
                optionCode = "ETH-HD-200",
                origin = "에티오피아",
                grindType = "홀빈",
                weightGrams = 200,
                price = 15000,
                quantity = 2,
                totalPrice = 30000,
                createdAt = now,
                updatedAt = now
            ),
            CartItemResponse(
                cartItemId = 2L,
                productOptionId = 20L,
                productName = "콜롬비아 수프리모",
                optionCode = "COL-WB-500",
                origin = "콜롬비아",
                grindType = "원두",
                weightGrams = 500,
                price = 25000,
                quantity = 1,
                totalPrice = 25000,
                createdAt = now,
                updatedAt = now
            )
        )

        every { cartItemRepository.findByUserId(userId) } returns cartItems

        // When
        val result = cartService.getCartItemsWithProducts(userId)

        // Then
        // [Repository 호출 검증]: findByUserId가 정확히 한 번 호출되어야 함
        verify(exactly = 1) { cartItemRepository.findByUserId(userId) }

        // [비즈니스 로직 검증]: 조회된 아이템 목록이 올바르게 반환되는가?
        assertEquals(2, result.size)
        assertEquals(1L, result[0].cartItemId)
        assertEquals(10L, result[0].productOptionId)
        assertEquals("에티오피아 예가체프 G1", result[0].productName)
        assertEquals(2, result[0].quantity)
        assertEquals(2L, result[1].cartItemId)
        assertEquals(20L, result[1].productOptionId)
    }

    @Test
    @DisplayName("장바구니가 비어 있을 경우 CartEmptyException이 발생해야 한다")
    fun `장바구니가 비어 있을 경우_CartEmptyException이 발생해야 한다`() {
        // Given
        val userId = 123L

        every { cartItemRepository.findByUserId(userId) } returns emptyList()

        // When & Then
        // [핵심 비즈니스 규칙 검증]: 빈 장바구니 시 적절한 예외가 발생하는가?
        val exception = assertThrows<CartEmptyException> {
            cartService.getCartItemsWithProducts(userId)
        }

        assertEquals("장바구니가 비어 있습니다.", exception.message)

        // [Repository 호출 검증]: findByUserId는 호출되었어야 함
        verify(exactly = 1) { cartItemRepository.findByUserId(userId) }
    }

    @Test
    @DisplayName("단일 아이템만 있는 장바구니 조회 시 정상 처리되어야 한다")
    fun `단일 아이템만 있는 장바구니 조회 시_정상 처리되어야 한다`() {
        // Given
        val userId = 123L
        val now = LocalDateTime.now()

        val singleCartItem = listOf(
            CartItemResponse(
                cartItemId = 1L,
                productOptionId = 10L,
                productName = "에티오피아 예가체프 G1",
                optionCode = "ETH-HD-200",
                origin = "에티오피아",
                grindType = "홀빈",
                weightGrams = 200,
                price = 15000,
                quantity = 5,
                totalPrice = 75000,
                createdAt = now,
                updatedAt = now
            )
        )

        every { cartItemRepository.findByUserId(userId) } returns singleCartItem

        // When
        val result = cartService.getCartItemsWithProducts(userId)

        // Then
        // [경계값 검증]: 단일 아이템도 정상적으로 처리되어야 함
        assertEquals(1, result.size)
        assertEquals(1L, result[0].cartItemId)
        assertEquals(5, result[0].quantity)
        assertEquals(75000, result[0].totalPrice)

        verify(exactly = 1) { cartItemRepository.findByUserId(userId) }
    }
}
