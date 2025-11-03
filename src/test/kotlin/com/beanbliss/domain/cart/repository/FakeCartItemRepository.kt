package com.beanbliss.domain.cart.repository

import com.beanbliss.domain.cart.dto.CartItemResponse
import java.time.LocalDateTime

/**
 * 테스트용 In-Memory CartItemRepository 구현체
 *
 * [특징]:
 * - 실제 DB 없이 메모리 기반으로 동작
 * - Repository 인터페이스의 계약을 준수
 * - 테스트 격리를 위해 각 테스트마다 새 인스턴스 생성
 */
class FakeCartItemRepository : CartItemRepository {

    private val cartItems = mutableMapOf<Long, CartItemData>()
    private var nextId = 1L

    override fun findByUserIdAndProductOptionId(userId: Long, productOptionId: Long): CartItemResponse? {
        return cartItems.values
            .firstOrNull { it.userId == userId && it.productOptionId == productOptionId }
            ?.toResponse()
    }

    override fun save(cartItem: CartItemResponse, userId: Long): CartItemResponse {
        val id = if (cartItem.cartItemId == 0L) nextId++ else cartItem.cartItemId
        val now = LocalDateTime.now()

        val data = CartItemData(
            id = id,
            userId = userId,
            productOptionId = cartItem.productOptionId,
            productName = cartItem.productName,
            optionCode = cartItem.optionCode,
            origin = cartItem.origin,
            grindType = cartItem.grindType,
            weightGrams = cartItem.weightGrams,
            price = cartItem.price,
            quantity = cartItem.quantity,
            createdAt = if (cartItem.cartItemId == 0L) now else cartItem.createdAt,
            updatedAt = now
        )

        cartItems[id] = data
        return data.toResponse()
    }

    override fun updateQuantity(cartItemId: Long, newQuantity: Int): CartItemResponse {
        val existingItem = cartItems[cartItemId]
            ?: throw IllegalArgumentException("Cart item not found: $cartItemId")

        val updatedItem = existingItem.copy(
            quantity = newQuantity,
            updatedAt = LocalDateTime.now()
        )

        cartItems[cartItemId] = updatedItem
        return updatedItem.toResponse()
    }

    // === Test Helper Methods ===

    /**
     * 테스트용: 모든 데이터 삭제
     */
    fun clear() {
        cartItems.clear()
        nextId = 1L
    }

    // === Internal Data Class ===

    data class CartItemData(
        val id: Long,
        val userId: Long,
        val productOptionId: Long,
        val productName: String,
        val optionCode: String,
        val origin: String,
        val grindType: String,
        val weightGrams: Int,
        val price: Int,
        val quantity: Int,
        val createdAt: LocalDateTime,
        val updatedAt: LocalDateTime
    ) {
        fun toResponse() = CartItemResponse(
            cartItemId = id,
            productOptionId = productOptionId,
            productName = productName,
            optionCode = optionCode,
            origin = origin,
            grindType = grindType,
            weightGrams = weightGrams,
            price = price,
            quantity = quantity,
            totalPrice = price * quantity,
            createdAt = createdAt,
            updatedAt = updatedAt
        )
    }
}
