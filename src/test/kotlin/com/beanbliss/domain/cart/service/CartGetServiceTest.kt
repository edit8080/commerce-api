package com.beanbliss.domain.cart.service

import com.beanbliss.domain.cart.domain.CartItem
import com.beanbliss.domain.cart.repository.CartItemRepository
import io.mockk.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

/**
 * CartService의 장바구니 조회 비즈니스 로직을 검증하는 테스트
 *
 * [검증 목표]:
 * 1. 장바구니 조회 시 Repository가 올바르게 호출되는가?
 * 2. 정상 조회 시 장바구니 아이템 목록(CartItem)이 반환되는가?
 *
 * [변경사항]:
 * - CartService는 이제 순수하게 CART 도메인 데이터만 반환
 * - Product 정보 조합은 UseCase에서 처리
 * - 빈 장바구니 검증도 UseCase에서 처리
 *
 * [관련 UseCase]:
 * - GetCartItemsUseCase, ReserveOrderUseCase, CreateOrderUseCase
 */
@DisplayName("장바구니 조회 Service 테스트")
class CartGetServiceTest {

    // Mock 객체 (Repository Interface에 의존)
    private val cartItemRepository: CartItemRepository = mockk()

    // 테스트 대상
    private lateinit var cartService: CartService

    @BeforeEach
    fun setUp() {
        cartService = CartService(cartItemRepository)
    }

    @Test
    @DisplayName("정상 조회 시 Repository의 findByUserId가 호출되고 장바구니 아이템 목록이 반환되어야 한다")
    fun `정상 조회 시_Repository의 findByUserId가 호출되고_장바구니 아이템 목록이 반환되어야 한다`() {
        // Given
        val userId = 123L
        val now = LocalDateTime.now()

        val cartItems = listOf(
            CartItem(
                id = 1L,
                userId = userId,
                productOptionId = 10L,
                quantity = 2,
                createdAt = now,
                updatedAt = now
            ),
            CartItem(
                id = 2L,
                userId = userId,
                productOptionId = 20L,
                quantity = 1,
                createdAt = now,
                updatedAt = now
            )
        )

        every { cartItemRepository.findByUserId(userId) } returns cartItems

        // When
        val result = cartService.getCartItems(userId)

        // Then
        // [Repository 호출 검증]: findByUserId가 정확히 한 번 호출되어야 함
        verify(exactly = 1) { cartItemRepository.findByUserId(userId) }

        // [비즈니스 로직 검증]: 조회된 아이템 목록이 올바르게 반환되는가?
        assertEquals(2, result.size)
        assertEquals(1L, result[0].id)
        assertEquals(10L, result[0].productOptionId)
        assertEquals(2, result[0].quantity)
        assertEquals(2L, result[1].id)
        assertEquals(20L, result[1].productOptionId)
    }

    @Test
    @DisplayName("장바구니가 비어 있을 경우 빈 리스트를 반환해야 한다")
    fun `장바구니가 비어 있을 경우_빈 리스트를 반환해야 한다`() {
        // Given
        val userId = 123L

        every { cartItemRepository.findByUserId(userId) } returns emptyList()

        // When
        val result = cartService.getCartItems(userId)

        // Then
        // [비즈니스 로직 검증]: 빈 장바구니는 빈 리스트 반환 (예외 발생하지 않음)
        assertEquals(0, result.size)

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
            CartItem(
                id = 1L,
                userId = userId,
                productOptionId = 10L,
                quantity = 5,
                createdAt = now,
                updatedAt = now
            )
        )

        every { cartItemRepository.findByUserId(userId) } returns singleCartItem

        // When
        val result = cartService.getCartItems(userId)

        // Then
        // [경계값 검증]: 단일 아이템도 정상적으로 처리되어야 함
        assertEquals(1, result.size)
        assertEquals(1L, result[0].id)
        assertEquals(5, result[0].quantity)

        verify(exactly = 1) { cartItemRepository.findByUserId(userId) }
    }
}
