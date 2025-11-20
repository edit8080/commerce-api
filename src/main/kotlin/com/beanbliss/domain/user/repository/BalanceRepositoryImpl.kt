package com.beanbliss.domain.user.repository

import com.beanbliss.domain.user.entity.BalanceEntity
import jakarta.persistence.LockModeType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository

/**
 * [책임]: Spring Data JPA를 활용한 Balance 영속성 처리
 * Infrastructure Layer에 속하며, JPA 기술에 종속적
 */
interface BalanceJpaRepository : JpaRepository<BalanceEntity, Long> {
    /**
     * 사용자 ID로 잔액 조회
     */
    fun findByUserId(userId: Long): BalanceEntity?

    /**
     * 사용자 ID로 잔액 조회 (비관적 락)
     * - FOR UPDATE 쿼리로 동시성 제어
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT b FROM BalanceEntity b WHERE b.userId = :userId")
    fun findByUserIdWithLock(@Param("userId") userId: Long): BalanceEntity?
}

/**
 * [책임]: BalanceRepository 인터페이스 구현체
 * - BalanceJpaRepository를 활용하여 실제 DB 접근
 */
@Repository
class BalanceRepositoryImpl(
    private val balanceJpaRepository: BalanceJpaRepository
) : BalanceRepository {

    override fun findByUserId(userId: Long): BalanceEntity? {
        return balanceJpaRepository.findByUserId(userId)
    }

    override fun findByUserIdWithLock(userId: Long): BalanceEntity? {
        return balanceJpaRepository.findByUserIdWithLock(userId)
    }

    override fun save(balance: BalanceEntity): BalanceEntity {
        return balanceJpaRepository.save(balance)
    }
}
