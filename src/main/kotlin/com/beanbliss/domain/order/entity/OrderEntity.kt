package com.beanbliss.domain.order.entity

import java.time.LocalDateTime

/**
 * [책임]: ORDER 테이블 엔티티
 * - 주문 정보 관리
 * - 결제 금액 및 배송지 정보 포함
 *
 * [테이블 구조]:
 * - id: bigint (PK)
 * - user_id: bigint
 * - total_amount: int (총 상품 금액)
 * - discount_amount: int (쿠폰 할인 금액)
 * - final_amount: int (최종 결제 금액 = total_amount - discount_amount)
 * - shipping_address: varchar(200)
 * - order_status: varchar (PENDING_PAYMENT, PAID, CANCELLED)
 * - ordered_at: datetime (주문 생성 시각)
 * - updated_at: datetime (주문 상태 변경 시각)
 */
data class OrderEntity(
    val id: Long,
    val userId: Long,
    val totalAmount: Int,
    val discountAmount: Int,
    val finalAmount: Int,
    val shippingAddress: String,
    val orderStatus: OrderStatus,
    val orderedAt: LocalDateTime,
    val updatedAt: LocalDateTime
)
