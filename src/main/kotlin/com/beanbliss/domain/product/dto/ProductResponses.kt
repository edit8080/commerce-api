package com.beanbliss.domain.product.dto

import com.beanbliss.common.dto.PageableResponse
import java.time.LocalDateTime

/**
 * 상품 목록 조회 응답
 */
data class ProductListResponse(
    val content: List<ProductResponse>,
    val pageable: PageableResponse
)

/**
 * 상품 정보 응답
 */
data class ProductResponse(
    val productId: Long,
    val name: String,
    val description: String,
    val brand: String,
    val createdAt: LocalDateTime,
    val options: List<ProductOptionResponse>
)

/**
 * 상품 옵션 정보 응답
 */
data class ProductOptionResponse(
    val optionId: Long,
    val optionCode: String,
    val origin: String,
    val grindType: String,
    val weightGrams: Int,
    val price: Int,
    val availableStock: Int
)
