package com.beanbliss.domain.product.dto

import java.math.BigDecimal
import java.time.LocalDateTime

// 1. 인기 상품 Top K 조회 응답
data class TopSellingProductsResponse(
    val products: List<TopSellingProductDto>
)

data class TopSellingProductDto(
    val id: Long,
    val name: String,
    val description: String,
    val origin: String,
    val salesCount: Int,
    val basePrice: BigDecimal,
    val thumbnailUrl: String
)

// 2. 상품 목록 조회 응답
data class ProductListResponse(
    val products: List<ProductDto>,
    val pagination: PaginationDto
)

data class ProductDto(
    val id: Long,
    val name: String,
    val description: String,
    val origin: String,
    val basePrice: BigDecimal,
    val thumbnailUrl: String
)

data class PaginationDto(
    val currentPage: Int,
    val pageSize: Int,
    val totalElements: Long,
    val totalPages: Int
)

// 3. 상품 상세 정보 조회 응답
data class ProductDetailResponse(
    val id: Long,
    val name: String,
    val description: String,
    val origin: String,
    val thumbnailUrl: String,
    val options: List<ProductOptionSummaryDto>,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime
)

data class ProductOptionSummaryDto(
    val id: Long,
    val optionCode: String,
    val grindType: String,
    val grindTypeName: String,
    val weightGrams: Int,
    val price: BigDecimal,
    val stockQuantity: Int,
    val isAvailable: Boolean
)

// 4. 상품 옵션 상세 조회 응답
data class ProductOptionDetailResponse(
    val id: Long,
    val productId: Long,
    val productName: String,
    val optionCode: String,
    val grindType: String,
    val grindTypeName: String,
    val weightGrams: Int,
    val price: BigDecimal,
    val inventory: InventoryDto,
    val isActive: Boolean,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime
)

data class InventoryDto(
    val stockQuantity: Int,
    val isAvailable: Boolean,
    val updatedAt: LocalDateTime
)
