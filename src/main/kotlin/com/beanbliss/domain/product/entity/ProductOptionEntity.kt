package com.beanbliss.domain.product.entity

import com.beanbliss.domain.product.repository.ProductOptionDetail
import java.time.LocalDateTime

/**
 * [책임]: 상품 옵션 Entity (ERD PRODUCT_OPTION 테이블에 대응)
 *
 * [테이블 구조]:
 * - id: bigint (PK)
 * - option_code: varchar (Unique, SKU)
 * - product_id: bigint (FK to PRODUCT)
 * - origin: varchar
 * - grind_type: varchar
 * - weight_grams: int
 * - price: decimal
 * - is_active: boolean
 * - created_at: datetime
 * - updated_at: datetime
 */
data class ProductOptionEntity(
    val id: Long,
    val optionCode: String,
    val productId: Long,
    val productName: String, // JOIN된 PRODUCT 정보
    val origin: String,
    val grindType: String,
    val weightGrams: Int,
    val price: Int,
    val isActive: Boolean,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime
) {
    /**
     * Entity를 ProductOptionDetail로 변환
     */
    fun toDetail(): ProductOptionDetail {
        return ProductOptionDetail(
            optionId = id,
            productId = productId,
            productName = productName,
            optionCode = optionCode,
            origin = origin,
            grindType = grindType,
            weightGrams = weightGrams,
            price = price,
            isActive = isActive
        )
    }
}
