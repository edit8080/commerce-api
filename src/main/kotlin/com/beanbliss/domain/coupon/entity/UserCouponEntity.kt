package com.beanbliss.domain.coupon.entity

import com.beanbliss.domain.coupon.enums.UserCouponStatus
import com.beanbliss.domain.order.entity.OrderEntity
import com.beanbliss.domain.user.entity.UserEntity
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
 * [연관관계]:
 * - USER_COUPON N:1 USER
 * - USER_COUPON N:1 COUPON
 * - USER_COUPON 1:1 COUPON_TICKET
 * - USER_COUPON 1:1 ORDER (nullable, mappedBy)
 */
@Entity
@Table(name = "user_coupon")
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
    // 연관관계 (fetch = LAZY로 N+1 문제 방지, FK 제약조건 없음)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", insertable = false, updatable = false, foreignKey = ForeignKey(ConstraintMode.NO_CONSTRAINT))
    var user: UserEntity? = null

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "coupon_id", insertable = false, updatable = false, foreignKey = ForeignKey(ConstraintMode.NO_CONSTRAINT))
    var coupon: CouponEntity? = null

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id", insertable = false, updatable = false, foreignKey = ForeignKey(ConstraintMode.NO_CONSTRAINT))
    var couponTicket: CouponTicketEntity? = null

    @OneToOne(mappedBy = "userCoupon", fetch = FetchType.LAZY)
    var order: OrderEntity? = null

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
