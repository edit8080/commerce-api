package com.beanbliss.domain.coupon.dto

import com.beanbliss.domain.coupon.service.CouponService

/**
 * 쿠폰 검증 결과
 *
 * @property coupon 검증된 쿠폰 정보 (Service DTO)
 */
data class CouponValidationResult(
    val coupon: CouponService.CouponInfo
)
