package com.beanbliss.domain.user.service

import com.beanbliss.domain.user.dto.BalanceResponse
import com.beanbliss.domain.user.exception.BalanceNotFoundException
import com.beanbliss.domain.user.repository.BalanceRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * [책임]: 사용자 잔액 비즈니스 로직 처리
 * - 잔액 조회
 */
@Service
@Transactional(readOnly = true)
class BalanceServiceImpl(
    private val balanceRepository: BalanceRepository
) : BalanceService {

    override fun getBalance(userId: Long): BalanceResponse {
        // 1. Repository에서 잔액 조회
        val balance = balanceRepository.findByUserId(userId)
            ?: throw BalanceNotFoundException("사용자 ID: $userId 의 잔액 정보를 찾을 수 없습니다.")

        // 2. Entity를 DTO(Response)로 변환
        return BalanceResponse(
            userId = balance.userId,
            amount = balance.amount,
            lastUpdatedAt = balance.updatedAt
        )
    }
}
