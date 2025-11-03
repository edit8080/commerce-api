package com.beanbliss.domain.coupon.dto

import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Positive

/**
 * [책임]: 쿠폰 발급 요청 DTO
 */
data class IssueCouponRequest(
    @field:NotNull(message = "사용자 ID는 필수입니다.")
    @field:Positive(message = "사용자 ID는 양수여야 합니다.")
    val userId: Long
)
