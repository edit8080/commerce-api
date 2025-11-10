package com.beanbliss.domain.inventory.entity

import com.beanbliss.domain.product.entity.ProductOptionEntity
import jakarta.persistence.*
import java.time.LocalDateTime

/**
 * [책임]: 상품 옵션별 재고 수량을 DB에 저장하기 위한 JPA Entity
 * Infrastructure Layer에 속하며, 기술 종속적인 코드 포함
 *
 * [테이블 구조]:
 * - id: bigint (PK)
 * - product_option_id: bigint (FK to PRODUCT_OPTION, Unique - 1:1 관계)
 * - stock_quantity: int (현재 재고 수량)
 * - created_at: datetime
 * - updated_at: datetime
 *
 * [연관관계]:
 * - INVENTORY 1:1 PRODUCT_OPTION
 *
 * [동시성 제어]:
 * - PRODUCT_OPTION과 분리하여 Lock 적용 용이
 */
@Entity
@Table(name = "inventory")
class InventoryEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(name = "product_option_id", nullable = false, unique = true)
    val productOptionId: Long,

    @Column(name = "stock_quantity", nullable = false)
    var stockQuantity: Int,

    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @Column(name = "updated_at", nullable = false)
    var updatedAt: LocalDateTime = LocalDateTime.now()
) {
    // 연관관계 (fetch = LAZY로 N+1 문제 방지, FK 제약조건 없음)
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_option_id", insertable = false, updatable = false, foreignKey = ForeignKey(ConstraintMode.NO_CONSTRAINT))
    var productOption: ProductOptionEntity? = null

    @PreUpdate
    fun onPreUpdate() {
        updatedAt = LocalDateTime.now()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as InventoryEntity

        return id == other.id
    }

    override fun hashCode(): Int {
        return id.hashCode()
    }

    override fun toString(): String {
        return "InventoryEntity(id=$id, productOptionId=$productOptionId, stockQuantity=$stockQuantity)"
    }
}
