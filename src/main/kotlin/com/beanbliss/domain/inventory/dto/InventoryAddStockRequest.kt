package com.beanbliss.domain.inventory.dto

import jakarta.validation.constraints.Min

/**
 * 재고 추가 요청 DTO
 *
 * [책임]: 클라이언트로부터 재고 추가 요청 데이터를 수신하고 유효성 검증
 */
data class InventoryAddStockRequest(
    @field:Min(value = 1, message = "수량은 1개 이상이어야 합니다.")
    val quantity: Int
)
