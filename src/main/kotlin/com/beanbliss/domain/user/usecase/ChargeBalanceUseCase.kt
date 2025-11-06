package com.beanbliss.domain.user.usecase

import com.beanbliss.domain.user.dto.ChargeBalanceResponse
import com.beanbliss.domain.user.service.BalanceService
import com.beanbliss.domain.user.service.UserService
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

/**
 * 사용자 잔액 충전 UseCase
 *
 * [책임]:
 * - UserService와 BalanceService 오케스트레이션
 * - 사용자 존재 확인 + 잔액 충전
 *
 * [DIP 준수]:
 * - UserService, BalanceService 인터페이스에만 의존
 */
@Component
class ChargeBalanceUseCase(
    private val userService: UserService,
    private val balanceService: BalanceService
) {

    /**
     * 사용자 잔액 충전
     *
     * @param userId 사용자 ID
     * @param chargeAmount 충전 금액 (Controller에서 검증됨: 1,000 ~ 1,000,000원)
     * @return 충전 결과 (충전 후 현재 잔액, 충전 시각)
     * @throws ResourceNotFoundException 사용자를 찾을 수 없는 경우
     */
    @Transactional
    fun chargeBalance(userId: Long, chargeAmount: Int): ChargeBalanceResponse {
        // 1. UserService를 통해 사용자 존재 여부 확인
        userService.validateUserExists(userId)

        // 2. BalanceService를 통해 잔액 충전
        return balanceService.chargeBalance(userId, chargeAmount)
    }
}
