package com.beanbliss.domain.user.service

import com.beanbliss.domain.user.entity.BalanceEntity
import com.beanbliss.domain.user.repository.BalanceRepository
import io.mockk.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.LocalDateTime

/**
 * BalanceService의 잔액 충전 비즈니스 로직 검증
 *
 * [검증 목표]:
 * - 충전 금액 유효성 검증 (1,000 ~ 1,000,000원)
 * - 잔액 계산 로직 검증
 * - UPSERT 로직 검증 (INSERT vs UPDATE)
 *
 * [비즈니스 로직]:
 * 1. 충전 금액 범위 검증 (최소 1,000원, 최대 1,000,000원)
 * 2. UPSERT: 레코드 없으면 INSERT, 있으면 UPDATE
 * 3. UPDATE 시 잔액 계산: currentBalance + chargeAmount
 *
 * [관련 API]:
 * - POST /api/users/{userId}/balance/charge
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
    @DisplayName("잔액 충전 성공 시 UPDATE가 수행되고 정확한 금액 계산이 이루어져야 한다")
    fun `잔액 충전 성공 시_UPDATE가 수행되고 정확한 금액 계산이 이루어져야 한다`() {
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
        // [비즈니스 로직 검증 1]: Repository 호출 확인
        verify(exactly = 1) { balanceRepository.findByUserIdWithLock(userId) }
        verify(exactly = 1) { balanceRepository.save(any()) }

        // [비즈니스 로직 검증 2]: 잔액 계산이 올바르게 수행되었는가? (30000 + 50000 = 80000)
        assertEquals(userId, response.userId)
        assertEquals(80000, response.currentBalance)
        assertNotNull(response.chargedAt)
    }

    @Test
    @DisplayName("충전 금액이 최소 금액(1,000원) 미만일 경우 IllegalArgumentException이 발생해야 한다")
    fun `충전 금액이 최소 금액(1,000원) 미만일 경우_IllegalArgumentException이 발생해야 한다`() {
        // Given
        val userId = 123L
        val invalidChargeAmount = 500 // 1,000원 미만

        // When & Then
        // [비즈니스 로직 검증]: 유효성 검증이 올바르게 수행되었는가?
        val exception = assertThrows<IllegalArgumentException> {
            balanceService.chargeBalance(userId, invalidChargeAmount)
        }

        assertTrue(exception.message!!.contains("1,000원 이상"))

        // Repository 호출은 발생하지 않아야 함
        verify(exactly = 0) { balanceRepository.findByUserIdWithLock(any()) }
        verify(exactly = 0) { balanceRepository.save(any()) }
    }

    @Test
    @DisplayName("충전 금액이 최대 금액(1,000,000원)을 초과할 경우 IllegalArgumentException이 발생해야 한다")
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
    @DisplayName("잔액 레코드가 없을 경우 INSERT가 수행되어야 한다 (UPSERT)")
    fun `잔액 레코드가 없을 경우_INSERT가 수행되어야 한다`() {
        // Given
        val userId = 999L
        val chargeAmount = 50000
        val now = LocalDateTime.now()

        val newBalance = BalanceEntity(
            id = 1L,
            userId = userId,
            amount = chargeAmount,
            createdAt = now,
            updatedAt = now
        )

        every { balanceRepository.findByUserIdWithLock(userId) } returns null
        every { balanceRepository.save(any()) } returns newBalance

        // When
        val response = balanceService.chargeBalance(userId, chargeAmount)

        // Then
        // [비즈니스 로직 검증]: UPSERT - INSERT 케이스 (0 + 50000 = 50000)
        verify(exactly = 1) { balanceRepository.findByUserIdWithLock(userId) }
        verify(exactly = 1) { balanceRepository.save(any()) }

        assertEquals(userId, response.userId)
        assertEquals(chargeAmount, response.currentBalance)
        assertNotNull(response.chargedAt)
    }

    @Test
    @DisplayName("최소 금액(1,000원) 충전 시 정상적으로 처리되어야 한다")
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
        // [비즈니스 로직 검증]: 경계값 테스트 (최소값) - 0 + 1000 = 1000
        assertEquals(minChargeAmount, response.currentBalance)
    }

    @Test
    @DisplayName("최대 금액(1,000,000원) 충전 시 정상적으로 처리되어야 한다")
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
        // [비즈니스 로직 검증]: 경계값 테스트 (최대값) - 500000 + 1000000 = 1500000
        assertEquals(1500000, response.currentBalance)
    }
}
