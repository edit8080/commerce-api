package com.beanbliss.domain.cart.entity

import com.beanbliss.domain.cart.dto.CartItemResponse
import java.time.LocalDateTime

/**
 * [책임]: 장바구니 아이템 Entity (ERD CART_ITEM 테이블에 대응)
 *
 * [테이블 구조]:
 * - id: bigint (PK)
 * - user_id: bigint (FK to USER)
 * - product_option_id: bigint (FK to PRODUCT_OPTION)
 * - quantity: int
 * - created_at: datetime
 * - updated_at: datetime
 *
 * [설계 원칙]:
 * - Entity는 DB 테이블 구조와 1:1 매핑
 * - DTO 변환 메서드 제공 (toResponse)
 */
data class CartItemEntity(
    val id: Long,
    val userId: Long,
    val productOptionId: Long,
    val productName: String,
    val optionCode: String,
    val origin: String,
    val grindType: String,
    val weightGrams: Int,
    val price: Int,
    var quantity: Int,
    val createdAt: LocalDateTime,
    var updatedAt: LocalDateTime
) {
    /**
     * Entity를 DTO(Response)로 변환
     */
    fun toResponse(): CartItemResponse {
        return CartItemResponse(
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
