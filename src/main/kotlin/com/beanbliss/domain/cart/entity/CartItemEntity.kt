package com.beanbliss.domain.cart.entity

import jakarta.persistence.*
import java.time.LocalDateTime

/**
 * [책임]: 장바구니에 담긴 상품 정보를 DB에 저장하기 위한 JPA Entity
 * Infrastructure Layer에 속하며, 기술 종속적인 코드 포함
 *
 * [테이블 구조]:
 * - id: bigint (PK)
 * - user_id: bigint (FK to USER)
 * - product_option_id: bigint (FK to PRODUCT_OPTION)
 * - quantity: int (수량)
 * - created_at: datetime
 * - updated_at: datetime
 *
 * [관계]:
 * - USER와 N:1 관계
 * - PRODUCT_OPTION과 N:1 관계
 */
@Entity
@Table(name = "cart_items")
class CartItemEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(name = "user_id", nullable = false)
    val userId: Long,

    @Column(name = "product_option_id", nullable = false)
    val productOptionId: Long,

    @Column(nullable = false)
    var quantity: Int,

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

        other as CartItemEntity

        return id == other.id
    }

    override fun hashCode(): Int {
        return id.hashCode()
    }

    override fun toString(): String {
        return "CartItemEntity(id=$id, userId=$userId, productOptionId=$productOptionId, quantity=$quantity)"
    }
}
