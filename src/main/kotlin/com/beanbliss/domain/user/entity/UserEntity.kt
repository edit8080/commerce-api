package com.beanbliss.domain.user.entity

import com.beanbliss.domain.cart.entity.CartItemEntity
import com.beanbliss.domain.coupon.entity.UserCouponEntity
import com.beanbliss.domain.inventory.entity.InventoryReservationEntity
import com.beanbliss.domain.order.entity.OrderEntity
import jakarta.persistence.*
import java.time.LocalDateTime

/**
 * [책임]: 사용자 정보를 DB에 저장하기 위한 JPA Entity
 * Infrastructure Layer에 속하며, 기술 종속적인 코드 포함
 *
 * [테이블 구조]:
 * - id: bigint (PK)
 * - email: varchar (Unique)
 * - password: varchar
 * - name: varchar
 * - created_at: datetime
 * - updated_at: datetime
 *
 * [연관관계]:
 * - USER 1:1 BALANCE
 * - USER 1:N USER_COUPON
 * - USER 1:N ORDER
 * - USER 1:N CART_ITEM
 * - USER 1:N INVENTORY_RESERVATION
 */
@Entity
@Table(name = "user")
class UserEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(nullable = false, unique = true)
    val email: String,

    @Column(nullable = false)
    val password: String,

    @Column(nullable = false)
    val name: String,

    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @Column(name = "updated_at", nullable = false)
    var updatedAt: LocalDateTime = LocalDateTime.now()
) {
    // 연관관계 (fetch = LAZY로 N+1 문제 방지, FK 제약조건 없음)
    @OneToOne(mappedBy = "user", fetch = FetchType.LAZY)
    var balance: BalanceEntity? = null

    @OneToMany(mappedBy = "user", fetch = FetchType.LAZY)
    var userCoupons: List<UserCouponEntity> = listOf()

    @OneToMany(mappedBy = "user", fetch = FetchType.LAZY)
    var orders: List<OrderEntity> = listOf()

    @OneToMany(mappedBy = "user", fetch = FetchType.LAZY)
    var cartItems: List<CartItemEntity> = listOf()

    @OneToMany(mappedBy = "user", fetch = FetchType.LAZY)
    var inventoryReservations: List<InventoryReservationEntity> = listOf()

    @PreUpdate
    fun onPreUpdate() {
        updatedAt = LocalDateTime.now()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as UserEntity

        return id == other.id
    }

    override fun hashCode(): Int {
        return id.hashCode()
    }

    override fun toString(): String {
        return "UserEntity(id=$id, email='$email', name='$name')"
    }
}
