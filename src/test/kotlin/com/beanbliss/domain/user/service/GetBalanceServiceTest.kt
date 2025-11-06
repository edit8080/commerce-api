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
 * 1. 잔액 레코드가 있을 경우 실제 잔액 반환 (Service DTO)
 * 2. 잔액 레코드가 없을 경우 null 반환
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
        balanceService = BalanceService(balanceRepository)
    }

    @Test
    @DisplayName("잔액 레코드가 있을 경우 Service DTO를 반환해야 한다")
    fun `잔액 레코드가 있을 경우_Service DTO를 반환해야 한다`() {
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
        val balanceInfo = balanceService.getBalance(userId)

        // Then: Service DTO 검증
        assertNotNull(balanceInfo)
        assertEquals(userId, balanceInfo!!.userId)
        assertEquals(50000, balanceInfo.amount)
        assertEquals(now, balanceInfo.updatedAt)

        verify(exactly = 1) { balanceRepository.findByUserId(userId) }
    }

    @Test
    @DisplayName("잔액 레코드가 없을 경우 null을 반환해야 한다")
    fun `잔액 레코드가 없을 경우_null을 반환해야 한다`() {
        // Given
        val userId = 999L

        every { balanceRepository.findByUserId(userId) } returns null

        // When
        val balanceInfo = balanceService.getBalance(userId)

        // Then: null 반환 검증
        assertNull(balanceInfo)

        verify(exactly = 1) { balanceRepository.findByUserId(userId) }
    }
}
