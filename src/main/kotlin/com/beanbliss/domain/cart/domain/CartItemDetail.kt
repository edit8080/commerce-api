package com.beanbliss.domain.cart.domain

import java.time.LocalDateTime

/**
 * [책임]: CART + PRODUCT 도메인 조합 결과 DTO
 * 여러 계층(UseCase, Service)에서 사용하며, 도메인 간 데이터 조합 결과를 담음
 *
 * [설계 변경]:
 * - Repository JOIN 제거로 인해 UseCase에서 데이터 조합
 * - Repository는 도메인별로 분리된 데이터만 반환
 * - UseCase에서 여러 도메인 데이터를 조합하여 이 DTO 생성
 *
 * [데이터 구조]:
 * - CART 도메인: cartItemId, productOptionId, quantity, createdAt, updatedAt
 * - PRODUCT 도메인: productName, optionCode, origin, grindType, weightGrams, price
 * - 계산 필드: totalPrice
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
