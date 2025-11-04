package com.beanbliss.domain.order.dto

import com.beanbliss.domain.inventory.entity.InventoryReservationStatus
import java.time.LocalDateTime

/**
 * [책임]: 재고 예약 아이템 응답 DTO
 * - 단일 예약 정보
 */
data class InventoryReservationItemResponse(
    val reservationId: Long,
    val productOptionId: Long,
    val productName: String,
    val optionCode: String,
    val quantity: Int,
    val status: InventoryReservationStatus,
    val availableStock: Int,
    val reservedAt: LocalDateTime,
    val expiresAt: LocalDateTime
)
