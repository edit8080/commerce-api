package com.beanbliss.domain.coupon.repository

import com.beanbliss.domain.coupon.enums.UserCouponStatus
import java.time.LocalDateTime

/**
 * UserCoupon과 Coupon을 JOIN한 결과 데이터 모델
 *
 * [책임]:
 * - Repository에서 JOIN 결과를 담아 Service로 전달
 * - Service에서 UserCouponResponse로 변환할 때 사용
 * - isAvailable: Repository 쿼리 레벨에서 계산된 사용 가능 여부
 */
data class UserCouponWithCoupon(
    // UserCoupon 정보
    val userCouponId: Long,
    val userId: Long,
    val couponId: Long,
    val status: UserCouponStatus,
    val usedOrderId: Long?,
    val usedAt: LocalDateTime?,
    val issuedAt: LocalDateTime,

    // Coupon 정보
    val couponName: String,
    val discountType: String,
    val discountValue: Int,
    val minOrderAmount: Int,
    val maxDiscountAmount: Int,
    val validFrom: LocalDateTime,
    val validUntil: LocalDateTime,

    // 계산 필드 (Repository 쿼리 레벨에서 계산)
    val isAvailable: Boolean         // (status == ISSUED) AND (validFrom <= NOW <= validUntil)
)
