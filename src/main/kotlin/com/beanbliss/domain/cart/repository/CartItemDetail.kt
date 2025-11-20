package com.beanbliss.domain.cart.repository

import java.time.LocalDateTime

/**
 * [책임]: CART_ITEM + PRODUCT_OPTION + PRODUCT JOIN 쿼리 결과 DTO
 * Repository 계층에서만 사용하며, N+1 문제 방지용
 *
 * [JOIN 구조]:
 * - CART_ITEM: 장바구니 아이템 기본 정보
 * - PRODUCT_OPTION: 상품 옵션 상세 정보
 * - PRODUCT: 상품 정보
 */
data class CartItemDetail(
    val cartItemId: Long,
    val productOptionId: Long,
    val productName: String,       // PRODUCT.name
    val optionCode: String,        // PRODUCT_OPTION.option_code
    val origin: String,            // PRODUCT_OPTION.origin
    val grindType: String,         // PRODUCT_OPTION.grind_type
    val weightGrams: Int,          // PRODUCT_OPTION.weight_grams
    val price: Int,                // PRODUCT_OPTION.price
    val quantity: Int,             // CART_ITEM.quantity
    val totalPrice: Int,           // price * quantity (계산 필드)
    val createdAt: LocalDateTime,  // CART_ITEM.created_at
    val updatedAt: LocalDateTime   // CART_ITEM.updated_at
)
