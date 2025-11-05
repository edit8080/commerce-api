package com.beanbliss.domain.user.dto

import java.time.LocalDateTime

/**
 * [책임]: 잔액 충전 응답 DTO
 * - 충전 후 현재 잔액
 * - 충전 시각
 */
data class ChargeBalanceResponse(
    val userId: Long,
    val currentBalance: Int,
    val chargedAt: LocalDateTime
)
