package com.beanbliss.domain.inventory.dto

import java.time.LocalDateTime

/**
 * 재고 정보 응답 DTO
 *
 * [책임]: 재고 + 상품 + 옵션 정보를 클라이언트에 전달
 */
data class InventoryResponse(
    val inventoryId: Long,          // 재고 ID
    val productId: Long,            // 상품 ID
    val productName: String,        // 상품명
    val productOptionId: Long,      // 상품 옵션 ID
    val optionCode: String,         // 옵션 코드
    val optionName: String,         // 옵션명
    val price: Int,                 // 가격
    val stockQuantity: Int,         // 재고 수량
    val createdAt: LocalDateTime    // 재고 등록 시각
)
