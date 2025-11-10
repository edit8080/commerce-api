package com.beanbliss.domain.coupon.enums

/**
 * 사용자 쿠폰 상태
 *
 * [책임]: 사용자에게 발급된 쿠폰의 상태를 타입 안전하게 표현
 *
 * [상태 설명]:
 * - ISSUED: 발급됨 (사용 가능)
 * - USED: 사용됨 (주문에 사용)
 * - EXPIRED: 만료됨 (유효기간 지남)
 */
enum class UserCouponStatus {
    /**
     * 발급됨 - 사용 가능한 상태
     */
    ISSUED,

    /**
     * 사용됨 - 주문에 사용된 상태
     */
    USED,

    /**
     * 만료됨 - 유효기간이 지난 상태
     */
    EXPIRED;

    companion object {
        /**
         * String 값을 UserCouponStatus Enum으로 변환
         *
         * @param value String 값 (예: "ISSUED", "USED", "EXPIRED")
         * @return 해당하는 UserCouponStatus
         * @throws IllegalArgumentException 유효하지 않은 값인 경우
         */
        fun fromString(value: String): UserCouponStatus {
            return entries.find { it.name.equals(value, ignoreCase = true) }
                ?: throw IllegalArgumentException("유효하지 않은 UserCouponStatus: $value")
        }
    }
}
