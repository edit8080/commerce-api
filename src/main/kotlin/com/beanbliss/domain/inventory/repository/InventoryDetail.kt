package com.beanbliss.domain.inventory.repository

import java.time.LocalDateTime

/**
 * [책임]: INVENTORY + PRODUCT_OPTION + PRODUCT JOIN 쿼리 결과 DTO
 * Repository 계층에서만 사용하며, N+1 문제 방지용
 *
 * [JOIN 구조]:
 * - INVENTORY: 재고 기본 정보
 * - PRODUCT_OPTION: 상품 옵션 정보
 * - PRODUCT: 상품 정보
 */
data class InventoryDetail(
    val inventoryId: Long,          // 재고 ID
    val productId: Long,            // 상품 ID
    val productName: String,        // 상품명 (PRODUCT.name)
    val productOptionId: Long,      // 상품 옵션 ID
    val optionCode: String,         // 옵션 코드 (PRODUCT_OPTION.option_code)
    val optionName: String,         // 옵션명 (grindType + weightGrams 조합)
    val price: Int,                 // 가격 (PRODUCT_OPTION.price)
    val stockQuantity: Int,         // 재고 수량 (INVENTORY.stock_quantity)
    val createdAt: LocalDateTime    // 재고 등록 시각 (INVENTORY.created_at)
)
