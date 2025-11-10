package com.beanbliss.domain.inventory.dto

/**
 * 재고 추가 응답 DTO
 *
 * [책임]: 재고 추가 후 결과를 클라이언트에 전달
 */
data class InventoryAddStockResponse(
    val productOptionId: Long,  // 상품 옵션 ID
    val currentStock: Int       // 추가 후 현재 재고 수량
)
