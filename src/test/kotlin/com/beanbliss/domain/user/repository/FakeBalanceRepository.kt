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

    override fun findByUserIdWithLock(userId: Long): BalanceEntity? {
        // In-memory 구현에서는 락이 불필요하지만
        // 인터페이스 계약을 준수하기 위해 findByUserId와 동일하게 동작
        return balances[userId]
    }

    override fun save(balance: BalanceEntity): BalanceEntity {
        // userId를 키로 사용하여 저장 또는 업데이트 (1:1 관계)
        balances[balance.userId] = balance
        return balance
    }
}
