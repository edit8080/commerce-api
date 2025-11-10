package com.beanbliss.domain.coupon.entity

import com.beanbliss.domain.coupon.enums.UserCouponStatus
import java.time.LocalDateTime

/**
 * [책임]: USER_COUPON 테이블 엔티티
 * - 사용자에게 발급된 쿠폰 정보
 */
data class UserCouponEntity(
    val id: Long,
    val userId: Long,
    val couponId: Long,
    val status: UserCouponStatus,
    val usedOrderId: Long?,
    val usedAt: LocalDateTime?,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime
)
