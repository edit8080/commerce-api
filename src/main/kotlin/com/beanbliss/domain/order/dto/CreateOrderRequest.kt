package com.beanbliss.domain.order.dto

import jakarta.validation.constraints.Positive
import jakarta.validation.constraints.Size

/**
 * [책임]: 주문 생성 요청 DTO
 * - 사용자 ID, 쿠폰 ID, 배송지 주소 전달
 * - 입력 유효성 검증
 */
data class CreateOrderRequest(
    @field:Positive(message = "사용자 ID는 양수여야 합니다.")
    val userId: Long,

    @field:Positive(message = "쿠폰 ID는 양수여야 합니다.")
    val userCouponId: Long?,  // nullable - 쿠폰 미사용 가능

    @field:Size(min = 10, max = 200, message = "배송지 주소는 10~200자여야 합니다.")
    val shippingAddress: String
)
