package com.beanbliss.domain.user.dto

import java.time.LocalDateTime

/**
 * [책임]: 잔액 충전 응답 DTO
 * - 충전 전/후 잔액 정보
 * - 충전 금액 및 시각
 */
data class ChargeBalanceResponse(
    val userId: Long,
    val previousBalance: Int,
    val chargeAmount: Int,
    val currentBalance: Int,
    val chargedAt: LocalDateTime
)
