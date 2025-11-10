package com.beanbliss.domain.order.dto

import com.beanbliss.domain.order.entity.OrderStatus
import java.time.LocalDateTime

/**
 * [책임]: 주문 생성 응답 DTO
 * - 생성된 주문 정보 전달
 * - 주문 항목, 쿠폰, 가격 정보 포함
 */
data class CreateOrderResponse(
    val orderId: Long,
    val orderStatus: OrderStatus,
    val orderItems: List<OrderItemResponse>,
    val appliedCoupon: AppliedCouponInfo?,  // nullable - 쿠폰 미사용 시 null
    val priceInfo: PriceInfo,
    val shippingAddress: String,
    val orderedAt: LocalDateTime
)
