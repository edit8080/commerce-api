package com.beanbliss.domain.coupon.entity

import com.beanbliss.domain.coupon.dto.CouponResponse
import java.time.LocalDateTime

/**
 * [책임]: 쿠폰 Entity (ERD COUPON 테이블에 대응)
 *
 * [테이블 구조]:
 * - id: bigint (PK)
 * - name: varchar
 * - discount_type: varchar (PERCENTAGE, FIXED_AMOUNT)
 * - discount_value: decimal
 * - min_order_amount: decimal
 * - max_discount_amount: decimal
 * - total_quantity: int
 * - valid_from: datetime
 * - valid_until: datetime
 * - created_at: datetime
 * - updated_at: datetime
 *
 * [설계 원칙]:
 * - Entity는 DB 테이블 구조와 1:1 매핑
 * - DTO 변환 메서드 제공 (toResponse)
 */
data class CouponEntity(
    val id: Long,
    val name: String,
    val discountType: String, // PERCENTAGE, FIXED_AMOUNT
    val discountValue: Int,
    val minOrderAmount: Int,
    val maxDiscountAmount: Int,
    val totalQuantity: Int,
    val validFrom: LocalDateTime,
    val validUntil: LocalDateTime,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime
) {
    /**
     * Entity를 DTO(Response)로 변환
     *
     * @param remainingQuantity 남은 수량 (COUPON_TICKET에서 계산)
     */
    fun toResponse(remainingQuantity: Int): CouponResponse {
        val now = LocalDateTime.now()
        val isInValidPeriod = now.isAfter(validFrom) && now.isBefore(validUntil)
        val hasRemainingQuantity = remainingQuantity > 0
        val isIssuable = isInValidPeriod && hasRemainingQuantity

        return CouponResponse(
            couponId = id,
            name = name,
            discountType = discountType,
            discountValue = discountValue,
            minOrderAmount = minOrderAmount,
            maxDiscountAmount = maxDiscountAmount,
            remainingQuantity = remainingQuantity,
            totalQuantity = totalQuantity,
            validFrom = validFrom,
            validUntil = validUntil,
            isIssuable = isIssuable
        )
    }
}
