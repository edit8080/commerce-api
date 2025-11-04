package com.beanbliss.domain.user.repository

import com.beanbliss.domain.user.entity.BalanceEntity
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

/**
 * [책임]: BalanceRepository의 데이터 조회 검증
 * - userId로 잔액 조회
 * - 1:1 관계 검증
 */
@DisplayName("사용자 잔액 Repository 테스트")
class BalanceRepositoryTest {

    private lateinit var repository: FakeBalanceRepository

    @BeforeEach
    fun setUp() {
        repository = FakeBalanceRepository()
    }

    @Test
    fun `userId로 잔액 조회 시_해당 사용자의 잔액이 반환되어야 한다`() {
        // Given
        val userId = 123L
        val now = LocalDateTime.now()

        val balance = BalanceEntity(
            id = 1L,
            userId = userId,
            amount = 50000,
            createdAt = now.minusDays(10),
            updatedAt = now
        )

        repository.addBalance(balance)

        // When
        val result = repository.findByUserId(userId)

        // Then
        assertNotNull(result)
        assertEquals(userId, result!!.userId)
        assertEquals(50000, result.amount)
        assertEquals(now, result.updatedAt)
    }

    @Test
    fun `존재하지 않는 userId로 조회 시_null을 반환해야 한다`() {
        // Given
        val userId = 999L

        // When
        val result = repository.findByUserId(userId)

        // Then
        assertNull(result)
    }

    @Test
    fun `잔액이 0원인 경우도 정상적으로 조회되어야 한다`() {
        // Given
        val userId = 123L
        val now = LocalDateTime.now()

        val balance = BalanceEntity(
            id = 1L,
            userId = userId,
            amount = 0,
            createdAt = now,
            updatedAt = now
        )

        repository.addBalance(balance)

        // When
        val result = repository.findByUserId(userId)

        // Then
        assertNotNull(result)
        assertEquals(0, result!!.amount)
    }

    @Test
    fun `1대1 관계_같은 userId로 여러 개의 잔액이 존재하지 않아야 한다`() {
        // Given
        val userId = 123L
        val now = LocalDateTime.now()

        val balance1 = BalanceEntity(
            id = 1L,
            userId = userId,
            amount = 10000,
            createdAt = now.minusDays(10),
            updatedAt = now.minusDays(5)
        )

        val balance2 = BalanceEntity(
            id = 2L,
            userId = userId,
            amount = 50000,
            createdAt = now.minusDays(10),
            updatedAt = now
        )

        // When
        repository.addBalance(balance1)
        repository.addBalance(balance2)  // 같은 userId로 덮어쓰기

        val result = repository.findByUserId(userId)

        // Then
        assertNotNull(result)
        // 나중에 추가한 balance2가 반환되어야 함 (1:1 관계)
        assertEquals(2L, result!!.id)
        assertEquals(50000, result.amount)
    }

    @Test
    fun `여러 사용자의 잔액이 각각 조회되어야 한다`() {
        // Given
        val now = LocalDateTime.now()

        val balance1 = BalanceEntity(
            id = 1L,
            userId = 100L,
            amount = 10000,
            createdAt = now,
            updatedAt = now
        )

        val balance2 = BalanceEntity(
            id = 2L,
            userId = 200L,
            amount = 20000,
            createdAt = now,
            updatedAt = now
        )

        repository.addBalance(balance1)
        repository.addBalance(balance2)

        // When
        val result1 = repository.findByUserId(100L)
        val result2 = repository.findByUserId(200L)

        // Then
        assertNotNull(result1)
        assertNotNull(result2)
        assertEquals(10000, result1!!.amount)
        assertEquals(20000, result2!!.amount)
    }
}
