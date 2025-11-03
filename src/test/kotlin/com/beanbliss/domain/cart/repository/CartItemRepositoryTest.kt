package com.beanbliss.domain.cart.repository

import com.beanbliss.domain.cart.dto.CartItemResponse
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

/**
 * CartItemRepository의 데이터 접근 로직을 검증하는 테스트
 *
 * [검증 목표]:
 * 1. 사용자 ID와 상품 옵션 ID로 장바구니 아이템을 조회할 수 있는가?
 * 2. 신규 장바구니 아이템을 저장할 수 있는가?
 * 3. 기존 장바구니 아이템의 수량을 업데이트할 수 있는가?
 * 4. 존재하지 않는 조건 조회 시 null을 반환하는가?
 */
@DisplayName("장바구니 아이템 Repository 테스트")
class CartItemRepositoryTest {

    private lateinit var cartItemRepository: FakeCartItemRepository

    @BeforeEach
    fun setUp() {
        cartItemRepository = FakeCartItemRepository()
    }

    @Test
    @DisplayName("사용자 ID와 상품 옵션 ID로 장바구니 아이템을 조회할 수 있어야 한다")
    fun `사용자 ID와 상품 옵션 ID로 장바구니 아이템을 조회할 수 있어야 한다`() {
        // Given
        val cartItem = createMockCartItem(productOptionId = 1L, quantity = 2)
        cartItemRepository.saveWithUserId(cartItem, userId = 1L)

        // When
        val result = cartItemRepository.findByUserIdAndProductOptionId(userId = 1L, productOptionId = 1L)

        // Then
        assertNotNull(result, "장바구니 아이템이 조회되어야 함")
        assertEquals(1L, result!!.productOptionId)
        assertEquals(2, result.quantity)
    }

    @Test
    @DisplayName("존재하지 않는 사용자 ID와 상품 옵션 ID로 조회 시 null을 반환해야 한다")
    fun `존재하지 않는 사용자 ID와 상품 옵션 ID로 조회 시 null을 반환해야 한다`() {
        // Given
        val cartItem = createMockCartItem(productOptionId = 1L, quantity = 2)
        cartItemRepository.saveWithUserId(cartItem, userId = 1L)

        // When
        val result = cartItemRepository.findByUserIdAndProductOptionId(
            userId = 999L,
            productOptionId = 999L
        )

        // Then
        assertNull(result, "존재하지 않는 장바구니 아이템은 null을 반환해야 함")
    }

    @Test
    @DisplayName("신규 장바구니 아이템을 저장할 수 있어야 한다")
    fun `신규 장바구니 아이템을 저장할 수 있어야 한다`() {
        // Given
        val newCartItem = createMockCartItem(productOptionId = 2L, quantity = 3)

        // When
        val savedItem = cartItemRepository.save(newCartItem)

        // Then
        assertTrue(savedItem.cartItemId > 0, "저장 후 ID가 자동 생성되어야 함")
        assertEquals(2L, savedItem.productOptionId)
        assertEquals(3, savedItem.quantity)
    }

    @Test
    @DisplayName("기존 장바구니 아이템의 수량을 업데이트할 수 있어야 한다")
    fun `기존 장바구니 아이템의 수량을 업데이트할 수 있어야 한다`() {
        // Given
        val cartItem = createMockCartItem(productOptionId = 1L, quantity = 2)
        val saved = cartItemRepository.saveWithUserId(cartItem, userId = 1L)
        val newQuantity = 5

        // When
        val updatedItem = cartItemRepository.updateQuantity(saved.cartItemId, newQuantity)

        // Then
        assertEquals(newQuantity, updatedItem.quantity, "수량이 5로 업데이트되어야 함")
    }

    @Test
    @DisplayName("동일 사용자가 다른 상품 옵션을 각각 저장할 수 있어야 한다")
    fun `동일 사용자가 다른 상품 옵션을 각각 저장할 수 있어야 한다`() {
        // Given
        val userId = 1L
        val cartItem1 = createMockCartItem(productOptionId = 3L, quantity = 1)
        val cartItem2 = createMockCartItem(productOptionId = 4L, quantity = 2)

        // When
        val saved1 = cartItemRepository.saveWithUserId(cartItem1, userId)
        val saved2 = cartItemRepository.saveWithUserId(cartItem2, userId)

        // Then
        assertNotEquals(saved1.cartItemId, saved2.cartItemId, "각 아이템은 별도의 ID를 가져야 함")

        val found1 = cartItemRepository.findByUserIdAndProductOptionId(userId, 3L)
        val found2 = cartItemRepository.findByUserIdAndProductOptionId(userId, 4L)

        assertNotNull(found1)
        assertNotNull(found2)
        assertEquals(1, found1!!.quantity)
        assertEquals(2, found2!!.quantity)
    }

    @Test
    @DisplayName("다른 사용자가 같은 상품 옵션을 각각 저장할 수 있어야 한다")
    fun `다른 사용자가 같은 상품 옵션을 각각 저장할 수 있어야 한다`() {
        // Given
        val productOptionId = 5L
        val user1Item = createMockCartItem(productOptionId = productOptionId, quantity = 2)
        val user2Item = createMockCartItem(productOptionId = productOptionId, quantity = 3)

        // When
        val saved1 = cartItemRepository.saveWithUserId(user1Item, userId = 1L)
        val saved2 = cartItemRepository.saveWithUserId(user2Item, userId = 2L)

        // Then
        assertNotEquals(saved1.cartItemId, saved2.cartItemId, "사용자별로 별도 아이템이어야 함")

        val found1 = cartItemRepository.findByUserIdAndProductOptionId(1L, productOptionId)
        val found2 = cartItemRepository.findByUserIdAndProductOptionId(2L, productOptionId)

        assertNotNull(found1)
        assertNotNull(found2)
        assertEquals(2, found1!!.quantity)
        assertEquals(3, found2!!.quantity)
    }

    @Test
    @DisplayName("updateQuantity 호출 시 updated_at 필드가 갱신되어야 한다")
    fun `updateQuantity 호출 시 updated_at 필드가 갱신되어야 한다`() {
        // Given
        val cartItem = createMockCartItem(productOptionId = 1L, quantity = 2)
        val saved = cartItemRepository.saveWithUserId(cartItem, userId = 1L)
        val originalUpdatedAt = saved.updatedAt

        // 시간 차이를 만들기 위해 잠시 대기
        Thread.sleep(10)

        // When
        val updatedItem = cartItemRepository.updateQuantity(saved.cartItemId, 10)

        // Then
        assertTrue(updatedItem.updatedAt.isAfter(originalUpdatedAt),
            "수량 업데이트 시 updated_at이 갱신되어야 함")
    }

    // === Helper Methods ===

    private fun createMockCartItem(
        productOptionId: Long,
        quantity: Int
    ): CartItemResponse {
        return CartItemResponse(
            cartItemId = 0L,
            productOptionId = productOptionId,
            productName = "테스트 상품",
            optionCode = "TEST-CODE-$productOptionId",
            origin = "Test Origin",
            grindType = "WHOLE_BEANS",
            weightGrams = 200,
            price = 20000,
            quantity = quantity,
            totalPrice = 20000 * quantity,
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now()
        )
    }
}
