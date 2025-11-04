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
 * [책임]: BalanceService의 잔액 충전 비즈니스 로직 검증
 * - Repository 호출 검증
 * - 충전 금액 유효성 검증
 * - 잔액 계산 로직 검증
 * - DTO 변환 검증
 * - 예외 처리 검증
 */
@DisplayName("사용자 잔액 충전 Service 테스트")
class ChargeBalanceServiceTest {

    private lateinit var balanceRepository: BalanceRepository
    private lateinit var balanceService: BalanceService

    @BeforeEach
    fun setUp() {
        balanceRepository = mockk()
        balanceService = BalanceServiceImpl(balanceRepository)
    }

    @Test
    fun `잔액 충전 성공 시_Repository의 findByUserIdWithLock과 save가 호출되어야 한다`() {
        // Given
        val userId = 123L
        val chargeAmount = 50000
        val now = LocalDateTime.now()

        val mockBalance = BalanceEntity(
            id = 1L,
            userId = userId,
            amount = 30000,
            createdAt = now.minusDays(10),
            updatedAt = now.minusDays(1)
        )

        val updatedBalance = mockBalance.copy(
            amount = 80000,
            updatedAt = now
        )

        every { balanceRepository.findByUserIdWithLock(userId) } returns mockBalance
        every { balanceRepository.save(any()) } returns updatedBalance

        // When
        val response = balanceService.chargeBalance(userId, chargeAmount)

        // Then
        // [TDD 검증 목표 1]: Service는 Repository의 계약(Interface)을 올바르게 사용했는가?
        verify(exactly = 1) { balanceRepository.findByUserIdWithLock(userId) }
        verify(exactly = 1) { balanceRepository.save(any()) }

        // [TDD 검증 목표 2]: 잔액 계산이 올바르게 수행되었는가?
        assertEquals(userId, response.userId)
        assertEquals(30000, response.previousBalance)
        assertEquals(50000, response.chargeAmount)
        assertEquals(80000, response.currentBalance)
        assertNotNull(response.chargedAt)
    }

    @Test
    fun `충전 금액이 최소 금액(1,000원) 미만일 경우_IllegalArgumentException이 발생해야 한다`() {
        // Given
        val userId = 123L
        val invalidChargeAmount = 500 // 1,000원 미만

        // When & Then
        // [TDD 검증 목표 3]: 유효성 검증이 올바르게 수행되었는가?
        val exception = assertThrows<IllegalArgumentException> {
            balanceService.chargeBalance(userId, invalidChargeAmount)
        }

        assertTrue(exception.message!!.contains("1,000원 이상"))

        // Repository 호출은 발생하지 않아야 함
        verify(exactly = 0) { balanceRepository.findByUserIdWithLock(any()) }
        verify(exactly = 0) { balanceRepository.save(any()) }
    }

    @Test
    fun `충전 금액이 최대 금액(1,000,000원)을 초과할 경우_IllegalArgumentException이 발생해야 한다`() {
        // Given
        val userId = 123L
        val invalidChargeAmount = 1500000 // 1,000,000원 초과

        // When & Then
        val exception = assertThrows<IllegalArgumentException> {
            balanceService.chargeBalance(userId, invalidChargeAmount)
        }

        assertTrue(exception.message!!.contains("1,000,000원"))

        // Repository 호출은 발생하지 않아야 함
        verify(exactly = 0) { balanceRepository.findByUserIdWithLock(any()) }
        verify(exactly = 0) { balanceRepository.save(any()) }
    }

    @Test
    fun `잔액 정보가 없을 경우_BalanceNotFoundException이 발생해야 한다`() {
        // Given
        val userId = 999L
        val chargeAmount = 50000

        every { balanceRepository.findByUserIdWithLock(userId) } returns null

        // When & Then
        // [TDD 검증 목표 4]: 예외 처리가 올바르게 수행되었는가?
        assertThrows<BalanceNotFoundException> {
            balanceService.chargeBalance(userId, chargeAmount)
        }

        // findByUserIdWithLock은 호출되었지만 save는 호출되지 않아야 함
        verify(exactly = 1) { balanceRepository.findByUserIdWithLock(userId) }
        verify(exactly = 0) { balanceRepository.save(any()) }
    }

    @Test
    fun `최소 금액(1,000원) 충전 시_정상적으로 처리되어야 한다`() {
        // Given
        val userId = 123L
        val minChargeAmount = 1000
        val now = LocalDateTime.now()

        val mockBalance = BalanceEntity(
            id = 1L,
            userId = userId,
            amount = 0,
            createdAt = now.minusDays(10),
            updatedAt = now.minusDays(10)
        )

        val updatedBalance = mockBalance.copy(
            amount = minChargeAmount,
            updatedAt = now
        )

        every { balanceRepository.findByUserIdWithLock(userId) } returns mockBalance
        every { balanceRepository.save(any()) } returns updatedBalance

        // When
        val response = balanceService.chargeBalance(userId, minChargeAmount)

        // Then
        assertEquals(0, response.previousBalance)
        assertEquals(minChargeAmount, response.chargeAmount)
        assertEquals(minChargeAmount, response.currentBalance)
    }

    @Test
    fun `최대 금액(1,000,000원) 충전 시_정상적으로 처리되어야 한다`() {
        // Given
        val userId = 123L
        val maxChargeAmount = 1000000
        val now = LocalDateTime.now()

        val mockBalance = BalanceEntity(
            id = 1L,
            userId = userId,
            amount = 500000,
            createdAt = now.minusDays(10),
            updatedAt = now.minusDays(1)
        )

        val updatedBalance = mockBalance.copy(
            amount = 1500000,
            updatedAt = now
        )

        every { balanceRepository.findByUserIdWithLock(userId) } returns mockBalance
        every { balanceRepository.save(any()) } returns updatedBalance

        // When
        val response = balanceService.chargeBalance(userId, maxChargeAmount)

        // Then
        assertEquals(500000, response.previousBalance)
        assertEquals(maxChargeAmount, response.chargeAmount)
        assertEquals(1500000, response.currentBalance)
    }

    @Test
    fun `DTO 변환_모든 필드가 올바르게 매핑되어야 한다`() {
        // Given
        val userId = 456L
        val chargeAmount = 100000
        val now = LocalDateTime.now()

        val mockBalance = BalanceEntity(
            id = 10L,
            userId = userId,
            amount = 200000,
            createdAt = now.minusDays(20),
            updatedAt = now.minusDays(5)
        )

        val updatedBalance = mockBalance.copy(
            amount = 300000,
            updatedAt = now
        )

        every { balanceRepository.findByUserIdWithLock(userId) } returns mockBalance
        every { balanceRepository.save(any()) } returns updatedBalance

        // When
        val response = balanceService.chargeBalance(userId, chargeAmount)

        // Then
        assertEquals(userId, response.userId)
        assertEquals(200000, response.previousBalance)
        assertEquals(100000, response.chargeAmount)
        assertEquals(300000, response.currentBalance)
        assertNotNull(response.chargedAt)
    }
}
