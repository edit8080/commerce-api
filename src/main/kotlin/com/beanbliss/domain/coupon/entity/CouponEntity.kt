package com.beanbliss.domain.coupon.entity

import com.beanbliss.domain.coupon.domain.DiscountType
import jakarta.persistence.*
import java.math.BigDecimal
import java.time.LocalDateTime

/**
 * [책임]: 쿠폰 마스터 정보를 DB에 저장하기 위한 JPA Entity
 * Infrastructure Layer에 속하며, 기술 종속적인 코드 포함
 *
 * [테이블 구조]:
 * - id: bigint (PK)
 * - name: varchar (쿠폰명)
 * - discount_type: varchar (할인 타입: PERCENTAGE, FIXED_AMOUNT)
 * - discount_value: decimal (할인값)
 * - min_order_amount: decimal (최소 주문 금액)
 * - max_discount_amount: decimal (최대 할인 금액)
 * - total_quantity: int (총 발행 수량)
 * - valid_from: datetime (유효 시작)
 * - valid_until: datetime (유효 종료)
 * - created_at: datetime
 * - updated_at: datetime
 *
 * [연관관계]:
 * - COUPON 1:N COUPON_TICKET
 *
 * [설계 변경사항]:
 * - issued_quantity 제거 → COUPON_TICKET으로 관리
 * - 쿠폰 메타데이터만 관리, 발급 관리는 COUPON_TICKET에서 처리
 */
@Entity
@Table(name = "coupon")
class CouponEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(nullable = false)
    val name: String,

    @Enumerated(EnumType.STRING)
    @Column(name = "discount_type", nullable = false)
    val discountType: DiscountType,

    @Column(name = "discount_value", nullable = false, precision = 10, scale = 2)
    val discountValue: BigDecimal,

    @Column(name = "min_order_amount", nullable = false, precision = 10, scale = 2)
    val minOrderAmount: BigDecimal,

    @Column(name = "max_discount_amount", nullable = false, precision = 10, scale = 2)
    val maxDiscountAmount: BigDecimal,

    @Column(name = "total_quantity", nullable = false)
    val totalQuantity: Int,

    @Column(name = "valid_from", nullable = false)
    val validFrom: LocalDateTime,

    @Column(name = "valid_until", nullable = false)
    val validUntil: LocalDateTime,

    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @Column(name = "updated_at", nullable = false)
    var updatedAt: LocalDateTime = LocalDateTime.now()
) {
    // 연관관계 (fetch = LAZY로 N+1 문제 방지, FK 제약조건 없음)
    @OneToMany(mappedBy = "coupon", fetch = FetchType.LAZY)
    var couponTickets: List<CouponTicketEntity> = listOf()

    @PreUpdate
    fun onPreUpdate() {
        updatedAt = LocalDateTime.now()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as CouponEntity

        return id == other.id
    }

    override fun hashCode(): Int {
        return id.hashCode()
    }

    override fun toString(): String {
        return "CouponEntity(id=$id, name='$name', discountType=$discountType, totalQuantity=$totalQuantity)"
    }
}
