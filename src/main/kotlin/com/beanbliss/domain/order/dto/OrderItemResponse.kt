package com.beanbliss.domain.order.dto

/**
 * [책임]: 주문 항목 응답 DTO
 * - 개별 상품 옵션의 주문 정보 전달
 */
data class OrderItemResponse(
    val productOptionId: Long,
    val productName: String,
    val optionCode: String,
    val quantity: Int,
    val unitPrice: Int,        // 개당 가격
    val totalPrice: Int         // 수량 * 단가
)
