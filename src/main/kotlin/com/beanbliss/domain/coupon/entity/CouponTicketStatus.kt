package com.beanbliss.domain.coupon.entity

/**
 * 쿠폰 티켓 상태
 *
 * [책임]: 쿠폰 티켓의 상태를 타입 안전하게 표현
 *
 * [상태 설명]:
 * - AVAILABLE: 발급 가능 (미발급)
 * - ISSUED: 발급됨
 * - EXPIRED: 만료됨
 */
enum class CouponTicketStatus {
    /**
     * 발급 가능 - 아직 발급되지 않은 상태
     */
    AVAILABLE,

    /**
     * 발급됨 - 사용자에게 발급된 상태
     */
    ISSUED,

    /**
     * 만료됨 - 유효기간이 지나 발급할 수 없는 상태
     */
    EXPIRED;

    companion object {
        /**
         * String 값을 CouponTicketStatus Enum으로 변환
         *
         * @param value String 값 (예: "AVAILABLE", "ISSUED", "EXPIRED")
         * @return 해당하는 CouponTicketStatus
         * @throws IllegalArgumentException 유효하지 않은 값인 경우
         */
        fun fromString(value: String): CouponTicketStatus {
            return entries.find { it.name.equals(value, ignoreCase = true) }
                ?: throw IllegalArgumentException("유효하지 않은 CouponTicketStatus: $value")
        }
    }
}
