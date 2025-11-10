package com.beanbliss.domain.order.dto

import jakarta.validation.constraints.Positive

/**
 * [책임]: 주문 예약 요청 DTO
 * - 사용자 ID 유효성 검증
 */
data class ReserveOrderRequest(
    @field:Positive(message = "사용자 ID는 양수여야 합니다.")
    val userId: Long
)
