package com.beanbliss.domain.coupon.entity

import com.beanbliss.domain.coupon.enums.UserCouponStatus
import jakarta.persistence.*
import java.time.LocalDateTime

/**
 * [책임]: 사용자별 쿠폰 발급 및 사용 이력을 DB에 저장하기 위한 JPA Entity
 * Infrastructure Layer에 속하며, 기술 종속적인 코드 포함
 *
 * [테이블 구조]:
 * - id: bigint (PK)
 * - user_id: bigint (FK to USER)
 * - coupon_id: bigint (FK to COUPON)
 * - status: varchar (ISSUED, USED, EXPIRED)
 * - used_order_id: bigint (FK to ORDER, nullable)
 * - used_at: datetime (nullable)
 * - created_at: datetime
 * - updated_at: datetime
 *
 * [관계]:
 * - USER와 N:1 관계
 * - COUPON과 N:1 관계
 * - ORDER와 N:1 관계 (사용된 경우)
 */
@Entity
@Table(name = "user_coupons")
class UserCouponEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(name = "user_id", nullable = false)
    val userId: Long,

    @Column(name = "coupon_id", nullable = false)
    val couponId: Long,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var status: UserCouponStatus = UserCouponStatus.ISSUED,

    @Column(name = "used_order_id", nullable = true)
    var usedOrderId: Long? = null,

    @Column(name = "used_at", nullable = true)
    var usedAt: LocalDateTime? = null,

    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),

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

        other as UserCouponEntity

        return id == other.id
    }

    override fun hashCode(): Int {
        return id.hashCode()
    }

    override fun toString(): String {
        return "UserCouponEntity(id=$id, userId=$userId, couponId=$couponId, status=$status)"
    }
}
