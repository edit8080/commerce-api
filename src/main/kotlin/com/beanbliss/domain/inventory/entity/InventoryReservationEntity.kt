package com.beanbliss.domain.inventory.entity

import java.time.LocalDateTime

/**
 * [책임]: INVENTORY_RESERVATION 테이블 엔티티
 * - 재고 예약 정보 관리
 * - 30분간 재고 보장
 *
 * [테이블 구조]:
 * - id: bigint (PK)
 * - product_option_id: bigint
 * - user_id: bigint
 * - quantity: int (예약 수량)
 * - status: varchar (RESERVED, CONFIRMED, EXPIRED, CANCELLED)
 * - reserved_at: datetime
 * - expires_at: datetime (reserved_at + 30분)
 * - updated_at: datetime
 */
data class InventoryReservationEntity(
    val id: Long,
    val productOptionId: Long,
    val userId: Long,
    val quantity: Int,
    val status: InventoryReservationStatus,
    val reservedAt: LocalDateTime,
    val expiresAt: LocalDateTime,
    val updatedAt: LocalDateTime
)
