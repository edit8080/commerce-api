package com.beanbliss.domain.order.dto

/**
 * 상품별 주문 수량 집계 정보
 *
 * [책임]: OrderService에서 집계된 상품별 주문 수량을 전달
 * [사용처]: GetPopularProductsUseCase에서 인기 상품 조회 시 사용
 */
data class ProductOrderCount(
    val productId: Long,        // 상품 ID
    val totalOrderCount: Int    // 총 주문 수량 (지정된 기간 동안)
)
