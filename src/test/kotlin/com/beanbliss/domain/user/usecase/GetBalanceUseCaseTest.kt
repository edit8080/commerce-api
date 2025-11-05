package com.beanbliss.domain.user.usecase

import com.beanbliss.common.exception.ResourceNotFoundException
import com.beanbliss.domain.user.dto.BalanceResponse
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
 * GetBalanceUseCase의 멀티 도메인 오케스트레이션 로직을 검증하는 테스트
 *
 * [테스트 목표]:
 * 1. UserService와 BalanceService를 올바르게 조율하는가?
 * 2. 사용자 존재 확인 + 잔액 조회가 순차적으로 수행되는가?
 * 3. 예외 상황에서 적절히 전파하는가?
 *
 * [UseCase의 책임]:
 * - UserService: 사용자 존재 여부 확인
 * - BalanceService: 잔액 조회
 * - UseCase: 두 Service의 결과를 조율
 *
 * [관련 API]:
 * - GET /api/users/{userId}/balance
 */
@DisplayName("사용자 잔액 조회 UseCase 테스트")
class GetBalanceUseCaseTest {

    // Mock 객체 (Service Interface에 의존)
    private val userService: UserService = mockk()
    private val balanceService: BalanceService = mockk()

    // 테스트 대상
    private lateinit var getBalanceUseCase: GetBalanceUseCase

    @BeforeEach
    fun setUp() {
        getBalanceUseCase = GetBalanceUseCase(userService, balanceService)
    }

    @Test
    @DisplayName("잔액 조회 성공 시 UserService와 BalanceService를 올바르게 조율해야 한다")
    fun `잔액 조회 성공 시_UserService와 BalanceService를 올바르게 조율해야 한다`() {
        // Given
        val userId = 123L
        val now = LocalDateTime.now()

        val mockResponse = BalanceResponse(
            userId = userId,
            amount = 50000,
            lastUpdatedAt = now
        )

        // UserService Mock 설정
        every { userService.validateUserExists(userId) } just Runs

        // BalanceService Mock 설정
        every { balanceService.getBalance(userId) } returns mockResponse

        // When
        val result = getBalanceUseCase.getBalance(userId)

        // Then
        // [검증 1]: UserService가 올바르게 호출되었는가?
        verify(exactly = 1) {
            userService.validateUserExists(userId)
        }

        // [검증 2]: BalanceService가 올바르게 호출되었는가?
        verify(exactly = 1) {
            balanceService.getBalance(userId)
        }

        // [검증 3]: 결과가 올바르게 반환되었는가?
        assertEquals(userId, result.userId)
        assertEquals(50000, result.amount)
        assertEquals(now, result.lastUpdatedAt)
    }

    @Test
    @DisplayName("존재하지 않는 사용자 조회 시 ResourceNotFoundException이 전파되어야 한다")
    fun `존재하지 않는 사용자 조회 시_ResourceNotFoundException이 전파되어야 한다`() {
        // Given
        val userId = 999L

        every { userService.validateUserExists(userId) } throws ResourceNotFoundException("사용자 ID: ${userId}를 찾을 수 없습니다.")

        // When & Then
        // [검증]: UserService에서 발생한 예외가 그대로 전파되어야 함
        val exception = assertThrows<ResourceNotFoundException> {
            getBalanceUseCase.getBalance(userId)
        }

        assertTrue(exception.message?.contains("$userId") ?: false)

        // [검증]: UserService 호출 후 예외 발생으로 BalanceService는 호출되지 않아야 함
        verify(exactly = 1) {
            userService.validateUserExists(userId)
        }
        verify(exactly = 0) {
            balanceService.getBalance(any())
        }
    }

    @Test
    @DisplayName("잔액 레코드가 없을 경우 0원이 반환되어야 한다")
    fun `잔액 레코드가 없을 경우_0원이 반환되어야 한다`() {
        // Given
        val userId = 123L

        // BalanceService가 레코드 없을 시 0원 반환
        val mockResponse = BalanceResponse(
            userId = userId,
            amount = 0,
            lastUpdatedAt = null
        )

        every { userService.validateUserExists(userId) } just Runs
        every { balanceService.getBalance(userId) } returns mockResponse

        // When
        val result = getBalanceUseCase.getBalance(userId)

        // Then
        // [검증]: 0원이 정상적으로 반환되어야 함
        assertEquals(userId, result.userId)
        assertEquals(0, result.amount)
        assertNull(result.lastUpdatedAt)

        // [검증]: UserService와 BalanceService 모두 호출되어야 함
        verify(exactly = 1) {
            userService.validateUserExists(userId)
        }
        verify(exactly = 1) {
            balanceService.getBalance(userId)
        }
    }

    @Test
    @DisplayName("잔액이 0원인 경우도 정상적으로 반환해야 한다")
    fun `잔액이 0원인 경우도 정상적으로 반환해야 한다`() {
        // Given
        val userId = 123L
        val now = LocalDateTime.now()

        val mockResponse = BalanceResponse(
            userId = userId,
            amount = 0,
            lastUpdatedAt = now
        )

        every { userService.validateUserExists(userId) } just Runs
        every { balanceService.getBalance(userId) } returns mockResponse

        // When
        val result = getBalanceUseCase.getBalance(userId)

        // Then
        // [검증]: 0원 잔액도 정상적으로 반환되어야 함
        assertEquals(0, result.amount)
    }
}
