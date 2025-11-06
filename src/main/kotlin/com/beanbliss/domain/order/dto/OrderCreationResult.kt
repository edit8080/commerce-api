package com.beanbliss.domain.order.dto

import com.beanbliss.domain.order.entity.OrderStatus
import java.time.LocalDateTime

/**
 * 주문 생성 결과
 *
 * @property orderId 생성된 주문 ID
 * @property orderStatus 주문 상태
 * @property orderItems 주문 아이템 목록
 * @property orderedAt 주문 생성 시각
 */
data class OrderCreationResult(
    val orderId: Long,
    val orderStatus: OrderStatus,
    val orderItems: List<OrderItemResponse>,
    val orderedAt: LocalDateTime
)
