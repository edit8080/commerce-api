package com.beanbliss.domain.order.entity

import jakarta.persistence.*
import java.math.BigDecimal
import java.time.LocalDateTime

/**
 * [책임]: 주문 정보를 DB에 저장하기 위한 JPA Entity
 * Infrastructure Layer에 속하며, 기술 종속적인 코드 포함
 *
 * [테이블 구조]:
 * - id: bigint (PK)
 * - user_id: bigint (FK to USER)
 * - user_coupon_id: bigint (FK to USER_COUPON, nullable)
 * - status: varchar (PAYMENT_COMPLETED, PREPARING, SHIPPING, DELIVERED)
 * - original_amount: decimal (원가)
 * - discount_amount: decimal (할인 금액)
 * - final_amount: decimal (최종 결제 금액)
 * - shipping_address: varchar (배송지 주소)
 * - tracking_number: varchar (운송장 번호, nullable)
 * - created_at: datetime
 * - updated_at: datetime
 *
 * [관계]:
 * - USER와 N:1 관계
 * - USER_COUPON과 N:1 관계 (선택적)
 * - ORDER_ITEM과 1:N 관계
 *
 * [고려 사항]:
 * - 금액 필드들은 계산 가능하지만 성능 최적화와 이력 관리를 위해 별도 저장
 */
@Entity
@Table(name = "orders")
class OrderEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(name = "user_id", nullable = false)
    val userId: Long,

    @Column(name = "user_coupon_id", nullable = true)
    val userCouponId: Long? = null,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var status: OrderStatus = OrderStatus.PAYMENT_COMPLETED,

    @Column(name = "original_amount", nullable = false, precision = 10, scale = 2)
    val originalAmount: BigDecimal,

    @Column(name = "discount_amount", nullable = false, precision = 10, scale = 2)
    val discountAmount: BigDecimal,

    @Column(name = "final_amount", nullable = false, precision = 10, scale = 2)
    val finalAmount: BigDecimal,

    @Column(name = "shipping_address", nullable = false)
    val shippingAddress: String,

    @Column(name = "tracking_number", nullable = true)
    var trackingNumber: String? = null,

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

        other as OrderEntity

        return id == other.id
    }

    override fun hashCode(): Int {
        return id.hashCode()
    }

    override fun toString(): String {
        return "OrderEntity(id=$id, userId=$userId, status=$status, finalAmount=$finalAmount)"
    }
}
