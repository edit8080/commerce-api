package com.beanbliss.domain.inventory.entity

import jakarta.persistence.*
import java.time.LocalDateTime

/**
 * [책임]: 재고 예약 정보를 DB에 저장하기 위한 JPA Entity
 * Infrastructure Layer에 속하며, 기술 종속적인 코드 포함
 *
 * [테이블 구조]:
 * - id: bigint (PK)
 * - product_option_id: bigint (FK to PRODUCT_OPTION)
 * - user_id: bigint (FK to USER)
 * - quantity: int (예약 수량)
 * - status: varchar (RESERVED, CONFIRMED, EXPIRED, CANCELLED)
 * - reserved_at: datetime (예약 시각)
 * - expires_at: datetime (만료 시각, reserved_at + 10분)
 * - updated_at: datetime
 *
 * [설계 배경]:
 * - 하이브리드 재고 관리: 2단계 재고 차감 전략
 *   - Phase 1 (예약): 주문창 진입 시 가상 예약 (실제 재고 차감 X)
 *   - Phase 2 (차감): 결제 시 비관적 락으로 실제 재고 차감
 * - 사용자 경험 향상: 주문창 진입 시 "내 재고"를 10분간 보장
 * - 악의적 선점 방지: 타임아웃(10분) + 1인 1회 제한
 */
@Entity
@Table(name = "inventory_reservations")
class InventoryReservationEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(name = "product_option_id", nullable = false)
    val productOptionId: Long,

    @Column(name = "user_id", nullable = false)
    val userId: Long,

    @Column(nullable = false)
    var quantity: Int,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var status: InventoryReservationStatus = InventoryReservationStatus.RESERVED,

    @Column(name = "reserved_at", nullable = false)
    val reservedAt: LocalDateTime = LocalDateTime.now(),

    @Column(name = "expires_at", nullable = false)
    val expiresAt: LocalDateTime,

    @Column(name = "updated_at", nullable = false)
    var updatedAt: LocalDateTime = LocalDateTime.now()
) {
    @PreUpdate
    fun onPreUpdate() {
        updatedAt = LocalDateTime.now()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as InventoryReservationEntity

        return id == other.id
    }

    override fun hashCode(): Int {
        return id.hashCode()
    }

    override fun toString(): String {
        return "InventoryReservationEntity(id=$id, productOptionId=$productOptionId, userId=$userId, quantity=$quantity, status=$status)"
    }
}
