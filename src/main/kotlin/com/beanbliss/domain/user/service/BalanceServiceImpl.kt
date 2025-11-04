package com.beanbliss.domain.user.service

import com.beanbliss.domain.user.dto.BalanceResponse
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
        // TODO: Red Test - 아직 구현되지 않음
        throw NotImplementedError("아직 구현되지 않았습니다")
    }
}
