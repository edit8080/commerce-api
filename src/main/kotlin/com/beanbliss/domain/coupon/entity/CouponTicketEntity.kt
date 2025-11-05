package com.beanbliss.domain.coupon.entity

import java.time.LocalDateTime

/**
 * [책임]: COUPON_TICKET 테이블 엔티티
 * - 선착순 쿠폰 발급을 위해 사전 생성된 티켓
 * - FOR UPDATE SKIP LOCKED 대상
 */
data class CouponTicketEntity(
    val id: Long?,
    val couponId: Long,
    val status: String, // AVAILABLE, ISSUED
    val userId: Long?,
    val userCouponId: Long?,
    val issuedAt: LocalDateTime?,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime
)
