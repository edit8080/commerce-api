package com.beanbliss.domain.order.entity

/**
 * [책임]: ORDER_ITEM 테이블 엔티티
 * - 주문 항목 정보 관리
 * - 주문별 상품 옵션 수량 및 가격 정보 포함
 *
 * [테이블 구조]:
 * - id: bigint (PK)
 * - order_id: bigint (FK: ORDER)
 * - product_option_id: bigint (FK: PRODUCT_OPTION)
 * - quantity: int (주문 수량)
 * - unit_price: int (개당 가격)
 * - total_price: int (수량 * 단가)
 */
data class OrderItemEntity(
    val id: Long,
    val orderId: Long,
    val productOptionId: Long,
    val quantity: Int,
    val unitPrice: Int,
    val totalPrice: Int
)
