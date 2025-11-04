package com.beanbliss.domain.user.repository

import com.beanbliss.domain.user.entity.BalanceEntity
import org.springframework.stereotype.Repository
import java.time.LocalDateTime
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

/**
 * [책임]: 사용자 잔액 In-memory 저장소 구현
 * - ConcurrentHashMap을 사용하여 Thread-safe 보장
 * - USER와 1:1 관계 유지 (userId를 키로 사용)
 *
 * [주의사항]:
 * - 애플리케이션 재시작 시 데이터 소실 (In-memory 특성)
 * - 실제 DB 사용 시 JPA 기반 구현체로 교체 필요
 */
@Repository
class BalanceRepositoryImpl : BalanceRepository {

    // Thread-safe한 In-memory 저장소 (userId를 키로 사용하여 1:1 관계 보장)
    private val balances = ConcurrentHashMap<Long, BalanceEntity>()

    // ID 생성기
    private val idGenerator = AtomicLong(0)

    init {
        // 초기 테스트 데이터 세팅
        initializeSampleData()
    }

    override fun findByUserId(userId: Long): BalanceEntity? {
        return balances[userId]
    }

    /**
     * 초기 테스트 데이터 세팅
     * 테스트 용도로 3개의 샘플 데이터를 생성합니다.
     */
    private fun initializeSampleData() {
        val now = LocalDateTime.now()

        // 사용자 1: 잔액 50,000원
        val balance1 = BalanceEntity(
            id = idGenerator.incrementAndGet(),
            userId = 1L,
            amount = 50000,
            createdAt = now.minusDays(30),
            updatedAt = now
        )
        balances[1L] = balance1

        // 사용자 2: 잔액 100,000원
        val balance2 = BalanceEntity(
            id = idGenerator.incrementAndGet(),
            userId = 2L,
            amount = 100000,
            createdAt = now.minusDays(20),
            updatedAt = now.minusDays(5)
        )
        balances[2L] = balance2

        // 사용자 3: 잔액 0원
        val balance3 = BalanceEntity(
            id = idGenerator.incrementAndGet(),
            userId = 3L,
            amount = 0,
            createdAt = now.minusDays(10),
            updatedAt = now.minusDays(10)
        )
        balances[3L] = balance3
    }
}
