package com.beanbliss.domain.user.service

import com.beanbliss.domain.user.entity.BalanceEntity
import com.beanbliss.domain.user.exception.BalanceNotFoundException
import com.beanbliss.domain.user.repository.BalanceRepository
import io.mockk.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.LocalDateTime

/**
 * [책임]: BalanceService의 비즈니스 로직 검증
 * - Repository 호출 검증
 * - DTO 변환 검증
 * - 예외 처리 검증
 */
@DisplayName("사용자 잔액 조회 Service 테스트")
class GetBalanceServiceTest {

    private lateinit var balanceRepository: BalanceRepository
    private lateinit var balanceService: BalanceService

    @BeforeEach
    fun setUp() {
        balanceRepository = mockk()
        balanceService = BalanceServiceImpl(balanceRepository)
    }

    @Test
    fun `잔액 조회 성공 시_Repository의 findByUserId가 호출되어야 한다`() {
        // Given
        val userId = 123L
        val now = LocalDateTime.now()

        val mockBalance = BalanceEntity(
            id = 1L,
            userId = userId,
            amount = 50000,
            createdAt = now.minusDays(10),
            updatedAt = now
        )

        every { balanceRepository.findByUserId(userId) } returns mockBalance

        // When
        val response = balanceService.getBalance(userId)

        // Then
        // [TDD 검증 목표 1]: Service는 Repository의 계약(Interface)을 올바르게 사용했는가?
        verify(exactly = 1) { balanceRepository.findByUserId(userId) }

        // [TDD 검증 목표 2]: DTO 변환이 올바르게 수행되었는가?
        assertEquals(userId, response.userId)
        assertEquals(50000, response.amount)
        assertEquals(now, response.lastUpdatedAt)
    }

    @Test
    fun `잔액 정보가 없을 경우_BalanceNotFoundException이 발생해야 한다`() {
        // Given
        val userId = 999L

        every { balanceRepository.findByUserId(userId) } returns null

        // When & Then
        // [TDD 검증 목표 3]: 예외 처리가 올바르게 수행되었는가?
        assertThrows<BalanceNotFoundException> {
            balanceService.getBalance(userId)
        }

        // Repository 호출은 1번만 수행되어야 함
        verify(exactly = 1) { balanceRepository.findByUserId(userId) }
    }

    @Test
    fun `잔액이 0원인 경우도 정상적으로 반환해야 한다`() {
        // Given
        val userId = 123L
        val now = LocalDateTime.now()

        val mockBalance = BalanceEntity(
            id = 1L,
            userId = userId,
            amount = 0,
            createdAt = now.minusDays(10),
            updatedAt = now
        )

        every { balanceRepository.findByUserId(userId) } returns mockBalance

        // When
        val response = balanceService.getBalance(userId)

        // Then
        assertEquals(userId, response.userId)
        assertEquals(0, response.amount)
        assertEquals(now, response.lastUpdatedAt)
    }

    @Test
    fun `DTO 변환_모든 필드가 올바르게 매핑되어야 한다`() {
        // Given
        val userId = 456L
        val createdAt = LocalDateTime.of(2025, 1, 1, 10, 0)
        val updatedAt = LocalDateTime.of(2025, 11, 3, 14, 30)

        val mockBalance = BalanceEntity(
            id = 10L,
            userId = userId,
            amount = 100000,
            createdAt = createdAt,
            updatedAt = updatedAt
        )

        every { balanceRepository.findByUserId(userId) } returns mockBalance

        // When
        val response = balanceService.getBalance(userId)

        // Then
        assertEquals(userId, response.userId)
        assertEquals(100000, response.amount)
        assertEquals(updatedAt, response.lastUpdatedAt)
    }
}
