package com.beanbliss.domain.cart.dto

import java.time.LocalDateTime

/**
 * 장바구니 아이템 응답 DTO
 *
 * [포함 정보]:
 * - 장바구니 아이템 ID
 * - 상품 옵션 정보 (상품명, 옵션 코드, 원산지, 분쇄 타입, 용량)
 * - 가격 정보 (단가, 수량, 총 가격)
 * - 타임스탬프 (생성 시각, 수정 시각)
 */
data class CartItemResponse(
    val cartItemId: Long,
    val productOptionId: Long,
    val productName: String,
    val optionCode: String,
    val origin: String,
    val grindType: String,
    val weightGrams: Int,
    val price: Int,
    val quantity: Int,
    val totalPrice: Int,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime
)
