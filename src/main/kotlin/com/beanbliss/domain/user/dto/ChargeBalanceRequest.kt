package com.beanbliss.domain.user.dto

import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min

/**
 * [책임]: 잔액 충전 요청 DTO
 * - 충전 금액 유효성 검증 (Bean Validation)
 */
data class ChargeBalanceRequest(
    @field:Min(value = 1000, message = "충전 금액은 1,000원 이상이어야 합니다.")
    @field:Max(value = 1000000, message = "1회 최대 충전 금액은 1,000,000원입니다.")
    val chargeAmount: Int
)
