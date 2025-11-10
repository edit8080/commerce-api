package com.beanbliss.domain.coupon.dto

import com.beanbliss.domain.coupon.enums.UserCouponStatus
import java.time.LocalDateTime

/**
 * [책임]: 쿠폰 발급 응답 DTO
 */
data class IssueCouponResponse(
    val userCouponId: Long,
    val couponId: Long,
    val userId: Long,
    val couponName: String,
    val discountType: String,
    val discountValue: Int,
    val minOrderAmount: Int,
    val maxDiscountAmount: Int,
    val status: UserCouponStatus,
    val validFrom: LocalDateTime,
    val validUntil: LocalDateTime,
    val issuedAt: LocalDateTime
)
