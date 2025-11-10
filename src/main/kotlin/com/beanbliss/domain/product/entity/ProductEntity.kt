package com.beanbliss.domain.product.entity

import jakarta.persistence.*
import java.time.LocalDateTime

/**
 * [책임]: 커피 원두 상품 기본 정보를 DB에 저장하기 위한 JPA Entity
 * Infrastructure Layer에 속하며, 기술 종속적인 코드 포함
 *
 * [테이블 구조]:
 * - id: bigint (PK)
 * - name: varchar (상품명)
 * - description: varchar (상품 설명)
 * - brand: varchar (커피 브랜드)
 * - created_at: datetime
 * - updated_at: datetime
 *
 * [관계]:
 * - PRODUCT_OPTION과 1:N 관계
 */
@Entity
@Table(name = "products")
class ProductEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(nullable = false)
    val name: String,

    @Column(nullable = false, columnDefinition = "TEXT")
    val description: String,

    @Column(nullable = false)
    val brand: String,

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

        other as ProductEntity

        return id == other.id
    }

    override fun hashCode(): Int {
        return id.hashCode()
    }

    override fun toString(): String {
        return "ProductEntity(id=$id, name='$name', brand='$brand')"
    }
}
