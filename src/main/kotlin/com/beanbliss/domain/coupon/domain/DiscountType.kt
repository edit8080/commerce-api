package com.beanbliss.domain.coupon.domain

import com.beanbliss.domain.coupon.exception.InvalidCouponException

/**
 * 쿠폰 할인 타입
 */
enum class DiscountType {
    /**
     * 정률 할인 (%)
     * - 할인값: 1~100
     * - 최대 할인 금액 설정 가능
     */
    PERCENTAGE,

    /**
     * 정액 할인 (원)
     * - 할인값: 1 이상
     * - 최대 할인 금액 설정 불가
     */
    FIXED_AMOUNT;

    companion object {
        /**
         * 문자열을 DiscountType enum으로 변환
         * @throws InvalidCouponException 유효하지 않은 타입인 경우
         */
        fun from(value: String): DiscountType {
            return try {
                valueOf(value.uppercase())
            } catch (e: IllegalArgumentException) {
                throw InvalidCouponException("잘못된 할인 타입입니다: $value. PERCENTAGE 또는 FIXED_AMOUNT만 허용됩니다.")
            }
        }
    }
}
