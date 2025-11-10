package com.beanbliss.domain.coupon.entity

import com.beanbliss.domain.user.entity.UserEntity
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
 * [연관관계]:
 * - COUPON_TICKET N:1 COUPON
 * - COUPON_TICKET N:1 USER (nullable)
 * - COUPON_TICKET 1:1 USER_COUPON
 *
 * [설계 배경]:
 * - 선착순 쿠폰 발급 시 동시성 제어
 * - total_quantity만큼 티켓을 사전 생성
 * - FOR UPDATE SKIP LOCKED로 동시 발급 처리
 */
@Entity
@Table(name = "coupon_ticket")
class CouponTicketEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(name = "coupon_id", nullable = false)
    val couponId: Long,

    @Column(name = "user_id", nullable = true)
    var userId: Long? = null,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var status: CouponTicketStatus = CouponTicketStatus.AVAILABLE,

    @Column(name = "user_coupon_id", nullable = true)
    var userCouponId: Long? = null,

    @Column(name = "issued_at", nullable = true)
    var issuedAt: LocalDateTime? = null,

    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: LocalDateTime = LocalDateTime.now()
) {
    // 연관관계 (fetch = LAZY로 N+1 문제 방지, FK 제약조건 없음)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "coupon_id", insertable = false, updatable = false, foreignKey = ForeignKey(ConstraintMode.NO_CONSTRAINT))
    var coupon: CouponEntity? = null

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", insertable = false, updatable = false, foreignKey = ForeignKey(ConstraintMode.NO_CONSTRAINT))
    var user: UserEntity? = null

    @OneToOne(mappedBy = "couponTicket", fetch = FetchType.LAZY)
    var userCoupon: UserCouponEntity? = null

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
        return "CouponTicketEntity(id=$id, couponId=$couponId, status=$status, userId=$userId)"
    }
}
