package com.beanbliss.domain.order.repository

/**
 * [책임]: 상품별 주문 수량 집계 정보 (ORDER_ITEM + PRODUCT_OPTION JOIN 결과)
 * Repository 계층에서만 사용하며, 집계 쿼리 결과를 담는 DTO
 *
 * [JOIN 구조]:
 * - ORDER_ITEM: 주문 아이템 정보
 * - PRODUCT_OPTION: 상품 옵션 정보 (product_id 조회용)
 *
 * [사용처]:
 * - OrderItemRepository.findTopOrderedProducts()
 * - 지정된 기간 동안 가장 많이 주문된 상품 조회 시 사용
 */
data class ProductOrderCount(
    val productId: Long,        // 상품 ID
    val totalOrderCount: Int    // 총 주문 수량 (지정된 기간 동안)
)
