package com.beanbliss.domain.coupon.repository

import java.time.LocalDateTime

/**
 * [책임]: 남은 수량 정보를 포함한 쿠폰 데이터
 *
 * Repository에서 조회한 쿠폰 정보 + 계산된 remainingQuantity를 담는 데이터 클래스
 * Service 계층에서 이를 받아 CouponResponse로 변환합니다.
 */
data class CouponWithQuantity(
    val id: Long,
    val name: String,
    val discountType: String,
    val discountValue: Int,
    val minOrderAmount: Int,
    val maxDiscountAmount: Int,
    val totalQuantity: Int,
    val validFrom: LocalDateTime,
    val validUntil: LocalDateTime,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
    val remainingQuantity: Int // COUPON_TICKET에서 계산된 값
)
