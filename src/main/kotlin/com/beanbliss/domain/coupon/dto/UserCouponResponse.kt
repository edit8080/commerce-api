package com.beanbliss.domain.coupon.dto

import com.beanbliss.domain.coupon.enums.UserCouponStatus
import java.time.LocalDateTime

/**
 * 사용자 쿠폰 응답 DTO
 *
 * [책임]:
 * - 사용자가 발급받은 쿠폰의 상세 정보 제공
 * - isAvailable: 현재 사용 가능 여부 제공
 */
data class UserCouponResponse(
    val userCouponId: Long,
    val couponId: Long,
    val couponName: String,
    val discountType: String,
    val discountValue: Int,
    val minOrderAmount: Int,
    val maxDiscountAmount: Int,
    val status: UserCouponStatus,
    val validFrom: LocalDateTime,
    val validUntil: LocalDateTime,
    val issuedAt: LocalDateTime,
    val usedAt: LocalDateTime?,
    val usedOrderId: Long?,
    val isAvailable: Boolean         // 사용 가능 여부 (status == ISSUED && 유효기간 내)
)
