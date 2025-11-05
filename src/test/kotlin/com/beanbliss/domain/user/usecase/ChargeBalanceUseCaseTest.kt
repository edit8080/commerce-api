package com.beanbliss.domain.user.usecase

import com.beanbliss.common.exception.ResourceNotFoundException
import com.beanbliss.domain.user.dto.ChargeBalanceResponse
import com.beanbliss.domain.user.service.BalanceService
import com.beanbliss.domain.user.service.UserService
import io.mockk.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.LocalDateTime

/**
 * ChargeBalanceUseCase의 멀티 도메인 오케스트레이션 로직을 검증하는 테스트
 *
 * [테스트 목표]:
 * 1. UserService와 BalanceService를 올바르게 조율하는가?
 * 2. 사용자 존재 확인 + 잔액 충전이 순차적으로 수행되는가?
 * 3. 예외 상황에서 적절히 전파하는가?
 *
 * [UseCase의 책임]:
 * - UserService: 사용자 존재 여부 확인
 * - BalanceService: 잔액 충전 수행
 * - UseCase: 두 Service의 결과를 조율
 *
 * [관련 API]:
 * - POST /api/users/{userId}/balance/charge
 */
@DisplayName("사용자 잔액 충전 UseCase 테스트")
class ChargeBalanceUseCaseTest {

    // Mock 객체 (Service Interface에 의존)
    private val userService: UserService = mockk()
    private val balanceService: BalanceService = mockk()

    // 테스트 대상
    private lateinit var chargeBalanceUseCase: ChargeBalanceUseCase

    @BeforeEach
    fun setUp() {
        chargeBalanceUseCase = ChargeBalanceUseCase(userService, balanceService)
    }

    @Test
    @DisplayName("잔액 충전 성공 시 UserService와 BalanceService를 올바르게 조율해야 한다")
    fun `잔액 충전 성공 시_UserService와 BalanceService를 올바르게 조율해야 한다`() {
        // Given
        val userId = 123L
        val chargeAmount = 50000
        val now = LocalDateTime.now()

        val mockResponse = ChargeBalanceResponse(
            userId = userId,
            currentBalance = 80000,
            chargedAt = now
        )

        // UserService Mock 설정
        every { userService.validateUserExists(userId) } just Runs

        // BalanceService Mock 설정
        every { balanceService.chargeBalance(userId, chargeAmount) } returns mockResponse

        // When
        val result = chargeBalanceUseCase.chargeBalance(userId, chargeAmount)

        // Then
        // [검증 1]: UserService가 올바르게 호출되었는가?
        verify(exactly = 1) {
            userService.validateUserExists(userId)
        }

        // [검증 2]: BalanceService가 올바르게 호출되었는가?
        verify(exactly = 1) {
            balanceService.chargeBalance(userId, chargeAmount)
        }

        // [검증 3]: 결과가 올바르게 반환되었는가?
        assertEquals(userId, result.userId)
        assertEquals(80000, result.currentBalance)
        assertEquals(now, result.chargedAt)
    }

    @Test
    @DisplayName("존재하지 않는 사용자의 잔액 충전 시 ResourceNotFoundException이 전파되어야 한다")
    fun `존재하지 않는 사용자의 잔액 충전 시_ResourceNotFoundException이 전파되어야 한다`() {
        // Given
        val userId = 999L
        val chargeAmount = 50000

        every { userService.validateUserExists(userId) } throws ResourceNotFoundException("사용자 ID: ${userId}를 찾을 수 없습니다.")

        // When & Then
        // [검증]: UserService에서 발생한 예외가 그대로 전파되어야 함
        val exception = assertThrows<ResourceNotFoundException> {
            chargeBalanceUseCase.chargeBalance(userId, chargeAmount)
        }

        assertTrue(exception.message?.contains("$userId") ?: false)

        // [검증]: UserService 호출 후 예외 발생으로 BalanceService는 호출되지 않아야 함
        verify(exactly = 1) {
            userService.validateUserExists(userId)
        }
        verify(exactly = 0) {
            balanceService.chargeBalance(any(), any())
        }
    }

    @Test
    @DisplayName("유효하지 않은 충전 금액일 경우 IllegalArgumentException이 전파되어야 한다")
    fun `유효하지 않은 충전 금액일 경우_IllegalArgumentException이 전파되어야 한다`() {
        // Given
        val userId = 123L
        val invalidChargeAmount = -1000 // 음수 금액

        every { userService.validateUserExists(userId) } just Runs
        every { balanceService.chargeBalance(userId, invalidChargeAmount) } throws IllegalArgumentException("충전 금액은 1,000원 이상 1,000,000원 이하여야 합니다.")

        // When & Then
        // [검증]: BalanceService에서 발생한 예외가 그대로 전파되어야 함
        val exception = assertThrows<IllegalArgumentException> {
            chargeBalanceUseCase.chargeBalance(userId, invalidChargeAmount)
        }

        assertTrue(exception.message?.contains("1,000원 이상 1,000,000원 이하") ?: false)

        // [검증]: UserService와 BalanceService 모두 호출되어야 함
        verify(exactly = 1) {
            userService.validateUserExists(userId)
        }
        verify(exactly = 1) {
            balanceService.chargeBalance(userId, invalidChargeAmount)
        }
    }

    @Test
    @DisplayName("최소 금액(1,000원) 충전 시 정상적으로 처리되어야 한다")
    fun `최소 금액 충전 시_정상적으로 처리되어야 한다`() {
        // Given
        val userId = 123L
        val minChargeAmount = 1000
        val now = LocalDateTime.now()

        val mockResponse = ChargeBalanceResponse(
            userId = userId,
            currentBalance = minChargeAmount,
            chargedAt = now
        )

        every { userService.validateUserExists(userId) } just Runs
        every { balanceService.chargeBalance(userId, minChargeAmount) } returns mockResponse

        // When
        val result = chargeBalanceUseCase.chargeBalance(userId, minChargeAmount)

        // Then
        // [검증]: 최소 금액도 정상적으로 충전되어야 함
        assertEquals(minChargeAmount, result.currentBalance)
    }

    @Test
    @DisplayName("최대 금액(1,000,000원) 충전 시 정상적으로 처리되어야 한다")
    fun `최대 금액 충전 시_정상적으로 처리되어야 한다`() {
        // Given
        val userId = 123L
        val maxChargeAmount = 1000000
        val now = LocalDateTime.now()

        val mockResponse = ChargeBalanceResponse(
            userId = userId,
            currentBalance = 1500000,
            chargedAt = now
        )

        every { userService.validateUserExists(userId) } just Runs
        every { balanceService.chargeBalance(userId, maxChargeAmount) } returns mockResponse

        // When
        val result = chargeBalanceUseCase.chargeBalance(userId, maxChargeAmount)

        // Then
        // [검증]: 최대 금액도 정상적으로 충전되어야 함
        assertEquals(1500000, result.currentBalance)
    }
}
