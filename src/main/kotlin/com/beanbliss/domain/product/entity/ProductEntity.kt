package com.beanbliss.domain.product.entity

import com.beanbliss.domain.product.dto.ProductOptionResponse
import com.beanbliss.domain.product.dto.ProductResponse
import java.time.LocalDateTime

/**
 * [책임]: 상품 Entity (ERD PRODUCT 테이블에 대응)
 *
 * [테이블 구조]:
 * - id: bigint (PK)
 * - name: varchar
 * - description: varchar
 * - brand: varchar
 * - created_at: datetime
 * - updated_at: datetime
 *
 * [설계 원칙]:
 * - Entity는 DB 테이블 구조와 1:1 매핑
 * - DTO 변환 메서드 제공 (toResponse)
 * - PRODUCT_OPTION과 1:N 관계
 */
data class ProductEntity(
    val id: Long,
    val name: String,
    val description: String,
    val brand: String,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime
) {
    /**
     * Entity를 DTO(Response)로 변환
     *
     * @param options 상품에 속한 옵션 목록 (활성 옵션만 포함)
     */
    fun toResponse(options: List<ProductOptionResponse>): ProductResponse {
        return ProductResponse(
            productId = id,
            name = name,
            description = description,
            brand = brand,
            createdAt = createdAt,
            options = options
        )
    }
}
