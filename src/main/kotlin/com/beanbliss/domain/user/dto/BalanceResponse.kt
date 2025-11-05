package com.beanbliss.domain.user.dto

import java.time.LocalDateTime

/**
 * 사용자 잔액 조회 응답 DTO
 *
 * [책임]:
 * - 사용자의 현재 잔액 정보 제공
 * - 레코드 없을 시 amount=0, lastUpdatedAt=null 반환
 */
data class BalanceResponse(
    val userId: Long,
    val amount: Int,
    val lastUpdatedAt: LocalDateTime?
)
