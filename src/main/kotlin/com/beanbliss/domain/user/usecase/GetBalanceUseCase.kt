package com.beanbliss.domain.user.usecase

import com.beanbliss.domain.user.service.BalanceService
import com.beanbliss.domain.user.service.UserService
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

/**
 * 사용자 잔액 조회 UseCase
 *
 * [책임]:
 * - UserService와 BalanceService 오케스트레이션
 * - 사용자 존재 확인 + 잔액 조회
 *
 * [DIP 준수]:
 * - UserService, BalanceService 인터페이스에만 의존
 */
@Component
class GetBalanceUseCase(
    private val userService: UserService,
    private val balanceService: BalanceService
) {

    /**
     * 사용자 잔액 조회
     *
     * @param userId 사용자 ID
     * @return 사용자 잔액 정보 (레코드 없으면 null)
     * @throws ResourceNotFoundException 사용자를 찾을 수 없는 경우
     */
    @Transactional(readOnly = true)
    fun getBalance(userId: Long): BalanceService.BalanceInfo? {
        // 1. UserService를 통해 사용자 존재 여부 확인
        userService.validateUserExists(userId)

        // 2. BalanceService를 통해 잔액 조회 (Service DTO 반환)
        return balanceService.getBalance(userId)
    }
}
