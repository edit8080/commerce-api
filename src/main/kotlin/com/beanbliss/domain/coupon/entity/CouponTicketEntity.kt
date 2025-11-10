package com.beanbliss.domain.coupon.entity

import jakarta.persistence.*
import java.time.LocalDateTime

/**
 * [책임]: 선착순 쿠폰 발급을 위한 티켓 정보를 DB에 저장하기 위한 JPA Entity
 * Infrastructure Layer에 속하며, 기술 종속적인 코드 포함
 *
 * [테이블 구조]:
 * - id: bigint (PK)
 * - coupon_id: bigint (FK to COUPON)
 * - user_id: bigint (FK to USER, nullable)
 * - status: varchar (AVAILABLE, ISSUED, EXPIRED)
 * - user_coupon_id: bigint (FK to USER_COUPON, nullable)
 * - issued_at: datetime (nullable)
 * - created_at: datetime
 *
 * [설계 배경]:
 * - 선착순 쿠폰 발급 시 동시성 제어
 * - total_quantity만큼 티켓을 사전 생성
 * - FOR UPDATE SKIP LOCKED로 동시 발급 처리
 */
@Entity
@Table(name = "coupon_tickets")
class CouponTicketEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(name = "coupon_id", nullable = false)
    val couponId: Long,

    @Column(name = "user_id", nullable = true)
    var userId: Long? = null,

    @Column(nullable = false)
    var status: String = "AVAILABLE", // AVAILABLE, ISSUED, EXPIRED

    @Column(name = "user_coupon_id", nullable = true)
    var userCouponId: Long? = null,

    @Column(name = "issued_at", nullable = true)
    var issuedAt: LocalDateTime? = null,

    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: LocalDateTime = LocalDateTime.now()
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as CouponTicketEntity

        return id == other.id
    }

    override fun hashCode(): Int {
        return id.hashCode()
    }

    override fun toString(): String {
        return "CouponTicketEntity(id=$id, couponId=$couponId, status='$status', userId=$userId)"
    }
}
