package com.beanbliss.domain.coupon.dto

import com.beanbliss.domain.coupon.domain.DiscountType
import java.time.LocalDateTime

/**
 * 쿠폰 생성 응답 DTO
 */
data class CreateCouponResponse(
    val couponId: Long,
    val name: String,
    val discountType: DiscountType,
    val discountValue: Int,
    val minOrderAmount: Int,
    val maxDiscountAmount: Int,
    val totalQuantity: Int,
    val validFrom: LocalDateTime,
    val validUntil: LocalDateTime,
    val createdAt: LocalDateTime
)
