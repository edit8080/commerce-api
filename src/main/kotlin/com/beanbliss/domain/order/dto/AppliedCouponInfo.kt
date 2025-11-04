package com.beanbliss.domain.order.dto

import java.time.LocalDateTime

/**
 * [책임]: 적용된 쿠폰 정보 응답 DTO
 * - 주문에 적용된 쿠폰 정보 전달
 * - nullable: 쿠폰 미사용 시 null
 */
data class AppliedCouponInfo(
    val couponId: Long,
    val couponCode: String,
    val couponName: String,
    val discountAmount: Int,
    val expiresAt: LocalDateTime
)
