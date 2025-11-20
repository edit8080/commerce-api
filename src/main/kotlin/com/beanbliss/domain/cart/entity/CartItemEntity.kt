package com.beanbliss.domain.cart.entity

import com.beanbliss.domain.product.entity.ProductOptionEntity
import com.beanbliss.domain.user.entity.UserEntity
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
 * [연관관계]:
 * - CART_ITEM N:1 USER
 * - CART_ITEM N:1 PRODUCT_OPTION
 */
@Entity
@Table(
    name = "cart_item",
    indexes = [
        Index(name = "idx_user_id", columnList = "user_id"),
        Index(name = "idx_user_product", columnList = "user_id, product_option_id")
    ]
)
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
    // 연관관계 (fetch = LAZY로 N+1 문제 방지, FK 제약조건 없음)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", insertable = false, updatable = false, foreignKey = ForeignKey(ConstraintMode.NO_CONSTRAINT))
    var user: UserEntity? = null

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_option_id", insertable = false, updatable = false, foreignKey = ForeignKey(ConstraintMode.NO_CONSTRAINT))
    var productOption: ProductOptionEntity? = null

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
