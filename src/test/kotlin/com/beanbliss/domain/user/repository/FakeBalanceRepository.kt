package com.beanbliss.domain.user.repository

import com.beanbliss.domain.user.entity.BalanceEntity
import java.util.concurrent.ConcurrentHashMap

/**
 * [책임]: BalanceRepository의 In-memory Fake 구현
 * - 단위 테스트를 위한 빠른 실행 환경 제공
 * - USER와 1:1 관계 유지
 */
class FakeBalanceRepository : BalanceRepository {

    private val balances = ConcurrentHashMap<Long, BalanceEntity>()

    // 테스트 헬퍼: 잔액 추가
    fun addBalance(balance: BalanceEntity) {
        // userId를 키로 사용 (1:1 관계)
        balances[balance.userId] = balance
    }

    // 테스트 헬퍼: 모든 데이터 삭제
    fun clear() {
        balances.clear()
    }

    override fun findByUserId(userId: Long): BalanceEntity? {
        return balances[userId]
    }
}
