package com.beanbliss.domain.coupon.dto

import com.beanbliss.common.dto.PageableResponse
import java.time.LocalDateTime

/**
 * [책임]: 쿠폰 목록 조회 응답 DTO (개별 쿠폰 정보)
 */
data class CouponResponse(
    val couponId: Long,
    val name: String,
    val discountType: String,
    val discountValue: Int,
    val minOrderAmount: Int,
    val maxDiscountAmount: Int,
    val remainingQuantity: Int,
    val totalQuantity: Int,
    val validFrom: LocalDateTime,
    val validUntil: LocalDateTime,
    val isIssuable: Boolean
)

/**
 * [책임]: 쿠폰 목록 조회 응답 DTO (페이징 정보 포함)
 */
data class CouponListResponse(
    val data: CouponListData
)

data class CouponListData(
    val content: List<CouponResponse>,
    val pageable: PageableResponse
)
