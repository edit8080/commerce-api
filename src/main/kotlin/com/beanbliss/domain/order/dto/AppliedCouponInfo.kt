package com.beanbliss.domain.order.dto

/**
 * [책임]: 적용된 쿠폰 정보 응답 DTO
 * - 주문에 적용된 쿠폰 정보 전달
 * - nullable: 쿠폰 미사용 시 null
 */
data class AppliedCouponInfo(
    val userCouponId: Long,
    val couponName: String,
    val discountType: String,
    val discountValue: Int
)
