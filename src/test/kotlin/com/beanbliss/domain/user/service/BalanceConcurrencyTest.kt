package com.beanbliss.domain.user.service

import com.beanbliss.common.test.ConcurrencyTestBase
import com.beanbliss.domain.user.entity.BalanceEntity
import com.beanbliss.domain.user.entity.UserEntity
import jakarta.persistence.EntityManager
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.transaction.support.TransactionTemplate
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger

/**
 * 잔액 동시성 테스트
 *
 * [테스트 목표]:
 * 1. Lost Update 문제 검증 - 비관적 락 없이 동시에 충전/차감할 때 발생
 * 2. 잔액 부족 예외 - 동시 차감 시 정확한 검증
 *
 * [테스트 시나리오]:
 * - 시나리오 1: Lost Update - 동시 충전
 * - 시나리오 2: Lost Update - 동시 차감
 * - 시나리오 3: 혼합 - 동시 충전과 차감
 */
@DisplayName("잔액 동시성 테스트")
class BalanceConcurrencyTest : ConcurrencyTestBase() {

    @Autowired
    private lateinit var balanceService: BalanceService

    @Autowired
    private lateinit var entityManager: EntityManager

    @Autowired
    private lateinit var jdbcTemplate: JdbcTemplate

    @Autowired
    private lateinit var transactionTemplate: TransactionTemplate

    private val testUserId = 1L

    @BeforeEach
    fun setUp() {
        cleanDatabase()
        transactionTemplate.execute {
            val user = UserEntity(
                email = "test@gmail.com",
                password = "password",
                name = "test"
            )
            entityManager.persist(user)
        }
    }

    private fun cleanDatabase() {
        jdbcTemplate.execute("SET FOREIGN_KEY_CHECKS = 0")
        jdbcTemplate.execute("TRUNCATE TABLE balance")
        jdbcTemplate.execute("TRUNCATE TABLE user")
        jdbcTemplate.execute("SET FOREIGN_KEY_CHECKS = 1")
    }

    @Test
    @DisplayName("시나리오 1: Lost Update - 10명이 동시에 1000원씩 충전")
    fun `Lost Update_동시 충전`() {
        // Given - 초기 잔액 5000원
        transactionTemplate.execute {
            val initialBalance = BalanceEntity(
                id = 0L,
                userId = testUserId,
                amount = BigDecimal(5000),
                createdAt = LocalDateTime.now(),
                updatedAt = LocalDateTime.now()
            )
            entityManager.persist(initialBalance)
            entityManager.flush()
        }

        val concurrentUsers = 10
        val chargeAmount = 1000
        val executor = Executors.newFixedThreadPool(concurrentUsers)
        val latch = CountDownLatch(concurrentUsers)
        val successCount = AtomicInteger(0)

        // When - 10명이 동시에 1000원씩 충전
        repeat(concurrentUsers) {
            executor.submit {
                try {
                    balanceService.chargeBalance(testUserId, chargeAmount)
                    successCount.incrementAndGet()
                } finally {
                    latch.countDown()
                }
            }
        }
        latch.await()
        executor.shutdown()

        // Then
        val finalAmount = jdbcTemplate.queryForObject(
            "SELECT amount FROM balance WHERE user_id = ?",
            BigDecimal::class.java,
            testUserId
        )

        val expectedAmount = BigDecimal(5000 + (successCount.get() * chargeAmount))
        assertEquals(0, expectedAmount.compareTo(finalAmount), "잔액 충전이 정확히 반영되어야 합니다")
    }

    @Test
    @DisplayName("시나리오 2: Lost Update - 10명이 동시에 500원씩 차감")
    fun `Lost Update_동시 차감`() {
        // Given - 초기 잔액 10000원
        transactionTemplate.execute {
            val initialBalance = BalanceEntity(
                id = 0L,
                userId = testUserId,
                amount = BigDecimal(10000),
                createdAt = LocalDateTime.now(),
                updatedAt = LocalDateTime.now()
            )
            entityManager.persist(initialBalance)
            entityManager.flush()
        }

        val concurrentUsers = 10
        val deductAmount = 500
        val executor = Executors.newFixedThreadPool(concurrentUsers)
        val latch = CountDownLatch(concurrentUsers)
        val successCount = AtomicInteger(0)

        // When - 10명이 동시에 500원씩 차감
        repeat(concurrentUsers) {
            executor.submit {
                try {
                    balanceService.deductBalance(testUserId, deductAmount)
                    successCount.incrementAndGet()
                } finally {
                    latch.countDown()
                }
            }
        }
        latch.await()
        executor.shutdown()

        // Then
        val finalAmount = jdbcTemplate.queryForObject(
            "SELECT amount FROM balance WHERE user_id = ?",
            BigDecimal::class.java,
            testUserId
        )

        val expectedAmount = BigDecimal(10000 - (successCount.get() * deductAmount))
        assertEquals(0, expectedAmount.compareTo(finalAmount), "잔액 차감이 정확히 반영되어야 합니다")
    }

    @Test
    @DisplayName("시나리오 3: 혼합 - 동시 충전 5명 + 차감 5명")
    fun `혼합_동시 충전과 차감`() {
        // Given - 초기 잔액 10000원
        transactionTemplate.execute {
            val initialBalance = BalanceEntity(
                id = 0L,
                userId = testUserId,
                amount = BigDecimal(10000),
                createdAt = LocalDateTime.now(),
                updatedAt = LocalDateTime.now()
            )
            entityManager.persist(initialBalance)
            entityManager.flush()
        }

        val concurrentOperations = 10
        val executor = Executors.newFixedThreadPool(concurrentOperations)
        val latch = CountDownLatch(concurrentOperations)
        val chargeCount = AtomicInteger(0)
        val deductCount = AtomicInteger(0)

        // When - 5명 충전 2000원 + 5명 차감 1000원
        repeat(5) {
            executor.submit {
                try {
                    balanceService.chargeBalance(testUserId, 2000)
                    chargeCount.incrementAndGet()
                } finally {
                    latch.countDown()
                }
            }
        }

        repeat(5) {
            executor.submit {
                try {
                    balanceService.deductBalance(testUserId, 1000)
                    deductCount.incrementAndGet()
                } finally {
                    latch.countDown()
                }
            }
        }

        latch.await()
        executor.shutdown()

        // Then
        val finalAmount = jdbcTemplate.queryForObject(
            "SELECT amount FROM balance WHERE user_id = ?",
            BigDecimal::class.java,
            testUserId
        )

        val expectedAmount = BigDecimal(10000 + (chargeCount.get() * 2000) - (deductCount.get() * 1000))
        assertEquals(0, expectedAmount.compareTo(finalAmount), "충전과 차감이 모두 정확히 반영되어야 합니다")
    }

    @Test
    @DisplayName("시나리오 4: 잔액 부족 - 동시 차감 시 정확한 검증")
    fun `잔액 부족_동시 차감`() {
        // Given - 초기 잔액 5000원
        transactionTemplate.execute {
            val initialBalance = BalanceEntity(
                id = 0L,
                userId = testUserId,
                amount = BigDecimal(5000),
                createdAt = LocalDateTime.now(),
                updatedAt = LocalDateTime.now()
            )
            entityManager.persist(initialBalance)
            entityManager.flush()
        }

        val concurrentUsers = 10
        val deductAmount = 1000
        val executor = Executors.newFixedThreadPool(concurrentUsers)
        val latch = CountDownLatch(concurrentUsers)
        val successCount = AtomicInteger(0)
        val failCount = AtomicInteger(0)

        // When - 10명이 동시에 1000원씩 차감 시도 (5000원 잔액)
        repeat(concurrentUsers) {
            executor.submit {
                try {
                    balanceService.deductBalance(testUserId, deductAmount)
                    successCount.incrementAndGet()
                } catch (e: Exception) {
                    failCount.incrementAndGet()
                } finally {
                    latch.countDown()
                }
            }
        }
        latch.await()
        executor.shutdown()

        // Then
        val finalAmount = jdbcTemplate.queryForObject(
            "SELECT amount FROM balance WHERE user_id = ?",
            BigDecimal::class.java,
            testUserId
        ) ?: BigDecimal.ZERO

        // 검증
        assertTrue(finalAmount >= BigDecimal.ZERO, "잔액이 음수가 되면 안 됩니다")
        assertEquals(5, successCount.get(), "정확히 5명만 성공해야 합니다")
        assertEquals(5, failCount.get(), "정확히 5명은 잔액 부족으로 실패해야 합니다")
        assertEquals(0, BigDecimal.ZERO.compareTo(finalAmount), "최종 잔액은 0원이어야 합니다")
    }
}
