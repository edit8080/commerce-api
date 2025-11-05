package com.beanbliss.domain.user.service

import com.beanbliss.domain.user.entity.BalanceEntity
import com.beanbliss.domain.user.repository.BalanceRepository
import io.mockk.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

/**
 * BalanceService의 비즈니스 로직 검증
 *
 * [검증 목표]:
 * - BalanceService의 잔액 조회 비즈니스 규칙 검증
 *
 * [비즈니스 로직]:
 * 1. 잔액 레코드가 있을 경우 실제 잔액 반환
 * 2. 잔액 레코드가 없을 경우 0원 반환 (예외 발생 X)
 *
 * [관련 API]:
 * - GET /api/users/{userId}/balance
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
    @DisplayName("잔액 레코드가 있을 경우 실제 잔액을 반환해야 한다")
    fun `잔액 레코드가 있을 경우_실제 잔액을 반환해야 한다`() {
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
        assertEquals(userId, response.userId)
        assertEquals(50000, response.amount)
        assertEquals(now, response.lastUpdatedAt)

        verify(exactly = 1) { balanceRepository.findByUserId(userId) }
    }

    @Test
    @DisplayName("잔액 레코드가 없을 경우 0원과 null lastUpdatedAt을 반환해야 한다")
    fun `잔액 레코드가 없을 경우_0원과 null lastUpdatedAt을 반환해야 한다`() {
        // Given
        val userId = 999L

        every { balanceRepository.findByUserId(userId) } returns null

        // When
        val response = balanceService.getBalance(userId)

        // Then
        // [비즈니스 로직 검증]: 레코드 없을 시 0원 반환 (예외 발생 X)
        assertEquals(userId, response.userId)
        assertEquals(0, response.amount)
        assertNull(response.lastUpdatedAt)

        verify(exactly = 1) { balanceRepository.findByUserId(userId) }
    }
}
