package com.beanbliss.domain.order.dto

/**
 * [책임]: 주문 예약 응답 DTO
 * - 예약된 재고 목록
 */
data class ReserveOrderResponse(
    val reservations: List<InventoryReservationItemResponse>
)
