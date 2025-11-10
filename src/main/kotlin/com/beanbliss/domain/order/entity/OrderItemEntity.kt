package com.beanbliss.domain.order.entity

import jakarta.persistence.*
import java.math.BigDecimal
import java.time.LocalDateTime

/**
 * [책임]: 주문별 상품 정보를 DB에 저장하기 위한 JPA Entity
 * Infrastructure Layer에 속하며, 기술 종속적인 코드 포함
 *
 * [테이블 구조]:
 * - id: bigint (PK)
 * - order_id: bigint (FK to ORDER)
 * - product_option_id: bigint (FK to PRODUCT_OPTION)
 * - quantity: int (주문 수량)
 * - unit_price: decimal (단가, 주문 당시 가격)
 * - total_price: decimal (총액, quantity * unit_price)
 * - created_at: datetime
 *
 * [관계]:
 * - ORDER와 N:1 관계
 * - PRODUCT_OPTION과 N:1 관계
 *
 * [고려 사항]:
 * - 주문 당시 가격을 unit_price에 저장하여 가격 변동에 대비 (snapshot)
 */
@Entity
@Table(name = "order_items")
class OrderItemEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(name = "order_id", nullable = false)
    val orderId: Long,

    @Column(name = "product_option_id", nullable = false)
    val productOptionId: Long,

    @Column(nullable = false)
    val quantity: Int,

    @Column(name = "unit_price", nullable = false, precision = 10, scale = 2)
    val unitPrice: BigDecimal,

    @Column(name = "total_price", nullable = false, precision = 10, scale = 2)
    val totalPrice: BigDecimal,

    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: LocalDateTime = LocalDateTime.now()
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as OrderItemEntity

        return id == other.id
    }

    override fun hashCode(): Int {
        return id.hashCode()
    }

    override fun toString(): String {
        return "OrderItemEntity(id=$id, orderId=$orderId, productOptionId=$productOptionId, quantity=$quantity, totalPrice=$totalPrice)"
    }
}
