package com.beanbliss.domain.user.entity

import java.time.LocalDateTime

/**
 * [책임]: BALANCE 테이블 엔티티
 * - 사용자별 현재 잔액 관리
 * - USER와 1:1 관계
 */
data class BalanceEntity(
    val id: Long,
    val userId: Long,          // Unique - 1:1 관계
    val amount: Int,           // 현재 잔액 (원 단위)
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime
)
