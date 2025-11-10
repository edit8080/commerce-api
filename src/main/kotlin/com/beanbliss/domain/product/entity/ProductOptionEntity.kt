package com.beanbliss.domain.product.entity

import com.beanbliss.domain.cart.entity.CartItemEntity
import com.beanbliss.domain.inventory.entity.InventoryEntity
import com.beanbliss.domain.inventory.entity.InventoryReservationEntity
import com.beanbliss.domain.order.entity.OrderItemEntity
import jakarta.persistence.*
import java.math.BigDecimal
import java.time.LocalDateTime

/**
 * [책임]: 상품 옵션 정보를 DB에 저장하기 위한 JPA Entity
 * Infrastructure Layer에 속하며, 기술 종속적인 코드 포함
 *
 * [테이블 구조]:
 * - id: bigint (PK)
 * - option_code: varchar (Unique, SKU 코드)
 * - product_id: bigint (FK to PRODUCT)
 * - origin: varchar (원산지)
 * - grind_type: varchar (분쇄 방식)
 * - weight_grams: int (용량)
 * - price: decimal (가격)
 * - is_active: boolean (활성 여부)
 * - created_at: datetime
 * - updated_at: datetime
 *
 * [연관관계]:
 * - PRODUCT_OPTION N:1 PRODUCT
 * - PRODUCT_OPTION 1:1 INVENTORY
 * - PRODUCT_OPTION 1:N CART_ITEM
 * - PRODUCT_OPTION 1:N ORDER_ITEM
 * - PRODUCT_OPTION 1:N INVENTORY_RESERVATION
 */
@Entity
@Table(name = "product_option")
class ProductOptionEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(name = "option_code", nullable = false, unique = true)
    val optionCode: String,

    @Column(name = "product_id", nullable = false)
    val productId: Long,

    @Column(nullable = false)
    val origin: String,

    @Column(name = "grind_type", nullable = false)
    val grindType: String,

    @Column(name = "weight_grams", nullable = false)
    val weightGrams: Int,

    @Column(nullable = false, precision = 10, scale = 2)
    val price: BigDecimal,

    @Column(name = "is_active", nullable = false)
    var isActive: Boolean = true,

    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @Column(name = "updated_at", nullable = false)
    var updatedAt: LocalDateTime = LocalDateTime.now()
) {
    // 연관관계 (fetch = LAZY로 N+1 문제 방지, FK 제약조건 없음)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", insertable = false, updatable = false, foreignKey = ForeignKey(ConstraintMode.NO_CONSTRAINT))
    var product: ProductEntity? = null

    @OneToOne(mappedBy = "productOption", fetch = FetchType.LAZY)
    var inventory: InventoryEntity? = null

    @OneToMany(mappedBy = "productOption", fetch = FetchType.LAZY)
    var cartItems: List<CartItemEntity> = listOf()

    @OneToMany(mappedBy = "productOption", fetch = FetchType.LAZY)
    var orderItems: List<OrderItemEntity> = listOf()

    @OneToMany(mappedBy = "productOption", fetch = FetchType.LAZY)
    var inventoryReservations: List<InventoryReservationEntity> = listOf()

    @PreUpdate
    fun onPreUpdate() {
        updatedAt = LocalDateTime.now()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ProductOptionEntity

        return id == other.id
    }

    override fun hashCode(): Int {
        return id.hashCode()
    }

    override fun toString(): String {
        return "ProductOptionEntity(id=$id, optionCode='$optionCode', origin='$origin', grindType='$grindType', weightGrams=$weightGrams)"
    }
}
