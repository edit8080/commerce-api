package com.beanbliss.domain.order.entity

/**
 * [책임]: 주문 상태 Enum
 * - 주문 생명주기 관리
 *
 * [상태 목록]:
 * - PAYMENT_COMPLETED: 결제 완료
 * - PREPARING: 배송 준비중
 * - SHIPPING: 배송중
 * - DELIVERED: 배송 완료
 */
enum class OrderStatus {
    PAYMENT_COMPLETED,  // 결제 완료
    PREPARING,          // 배송 준비중
    SHIPPING,           // 배송중
    DELIVERED           // 배송 완료
}
