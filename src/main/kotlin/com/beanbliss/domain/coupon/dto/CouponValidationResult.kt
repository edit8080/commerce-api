package com.beanbliss.domain.coupon.dto

import com.beanbliss.domain.coupon.entity.CouponEntity

/**
 * 쿠폰 검증 결과
 *
 * @property coupon 검증된 쿠폰 엔티티
 */
data class CouponValidationResult(
    val coupon: CouponEntity
)
