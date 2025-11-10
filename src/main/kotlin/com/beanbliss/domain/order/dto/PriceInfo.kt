package com.beanbliss.domain.order.dto

/**
 * [책임]: 주문 가격 정보 응답 DTO
 * - 총 상품 금액, 할인 금액, 최종 결제 금액 전달
 */
data class PriceInfo(
    val totalProductAmount: Int,    // 총 상품 금액 (할인 전)
    val discountAmount: Int,         // 쿠폰 할인 금액
    val finalAmount: Int             // 최종 결제 금액 = totalProductAmount - discountAmount
)
