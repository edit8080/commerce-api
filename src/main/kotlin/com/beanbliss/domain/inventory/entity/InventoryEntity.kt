package com.beanbliss.domain.inventory.entity

import java.time.LocalDateTime

/**
 * [책임]: 재고 Entity (ERD INVENTORY 테이블에 대응)
 *
 * [테이블 구조]:
 * - id: bigint (PK)
 * - product_option_id: bigint (FK to PRODUCT_OPTION)
 * - stock_quantity: int
 * - created_at: datetime
 * - updated_at: datetime
 *
 * [설계 원칙]:
 * - Entity는 DB 테이블 구조와 1:1 매핑
 * - 실제 가용 재고는 INVENTORY_RESERVATION을 고려하여 계산
 */
data class InventoryEntity(
    val id: Long,
    val productOptionId: Long,
    val stockQuantity: Int,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime
)
