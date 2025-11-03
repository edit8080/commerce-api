package com.beanbliss.domain.cart.repository

import com.beanbliss.domain.cart.dto.CartItemResponse
import com.beanbliss.domain.cart.entity.CartItemEntity
import org.springframework.stereotype.Repository
import java.time.LocalDateTime
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

/**
 * [책임]: 장바구니 아이템 In-memory 저장소 구현
 * - ConcurrentHashMap을 사용하여 Thread-safe 보장
 * - AtomicLong을 사용하여 ID 자동 생성
 *
 * [주의사항]:
 * - 애플리케이션 재시작 시 데이터 소실 (In-memory 특성)
 * - 실제 DB 사용 시 JPA 기반 구현체로 교체 필요
 */
@Repository
class CartItemRepositoryImpl : CartItemRepository {

    // Thread-safe한 In-memory 저장소
    private val cartItems = ConcurrentHashMap<Long, CartItemEntity>()

    // 자동 증가 ID 생성기
    private val idGenerator = AtomicLong(1L)

    override fun findByUserIdAndProductOptionId(
        userId: Long,
        productOptionId: Long
    ): CartItemResponse? {
        return cartItems.values
            .firstOrNull { it.userId == userId && it.productOptionId == productOptionId }
            ?.toResponse()
    }

    override fun save(cartItem: CartItemResponse, userId: Long): CartItemResponse {
        val now = LocalDateTime.now()

        // 신규 저장: ID가 0이면 새 ID 생성
        val id = if (cartItem.cartItemId == 0L) {
            idGenerator.getAndIncrement()
        } else {
            cartItem.cartItemId
        }

        val entity = CartItemEntity(
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

        cartItems[id] = entity
        return entity.toResponse()
    }

    override fun updateQuantity(cartItemId: Long, newQuantity: Int): CartItemResponse {
        val entity = cartItems[cartItemId]
            ?: throw IllegalArgumentException("장바구니 아이템을 찾을 수 없습니다. ID: $cartItemId")

        // 수량 업데이트
        entity.quantity = newQuantity
        entity.updatedAt = LocalDateTime.now()

        return entity.toResponse()
    }

    /**
     * 테스트용 헬퍼 메서드: 모든 데이터 삭제
     */
    fun clear() {
        cartItems.clear()
        idGenerator.set(1L)
    }
}
