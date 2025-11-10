package com.beanbliss.domain.product.repository

import java.time.LocalDateTime

/**
 * [책임]: PRODUCT + PRODUCT_OPTION + INVENTORY JOIN 쿼리 결과 DTO
 * Repository 계층에서만 사용하며, N+1 문제 방지용
 *
 * [JOIN 구조]:
 * - PRODUCT: 기본 상품 정보
 * - PRODUCT_OPTION: 상품 옵션 정보 (활성 옵션만)
 * - INVENTORY: 재고 정보
 */
data class ProductWithOptions(
    val productId: Long,
    val name: String,
    val description: String,
    val brand: String,
    val createdAt: LocalDateTime,
    val options: List<ProductOptionInfo>
)

/**
 * [책임]: PRODUCT_OPTION + INVENTORY JOIN 결과
 * ProductWithOptions의 내부 데이터 구조
 */
data class ProductOptionInfo(
    val optionId: Long,
    val optionCode: String,
    val origin: String,
    val grindType: String,
    val weightGrams: Int,
    val price: Int,
    val availableStock: Int
)

/**
 * [책임]: 상품 기본 정보 (JOIN 없음)
 * Repository에서 단일 PRODUCT 테이블 조회 시 사용
 */
data class ProductBasicInfo(
    val productId: Long,
    val productName: String,
    val brand: String,
    val description: String
)
