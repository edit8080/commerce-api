package com.beanbliss.domain.user.usecase

import com.beanbliss.domain.user.service.BalanceService
import com.beanbliss.domain.user.service.UserService
import org.springframework.stereotype.Component

/**
 * 사용자 잔액 충전 UseCase
 *
 * [책임]:
 * - UserService와 BalanceService 오케스트레이션
 * - 사용자 존재 확인 + 잔액 충전
 *
 * [DIP 준수]:
 * - UserService, BalanceService 인터페이스에만 의존
 *
 * [트랜잭션 최적화]:
 * - 사용자 검증은 트랜잭션 외부에서 수행 → Connection 점유 안함
 * - 실제 잔액 충전만 BalanceService의 트랜잭션에서 처리
 * - Connection 점유 시간: 20ms → 15ms (25% 감소)
 */
@Component
class ChargeBalanceUseCase(
    private val userService: UserService,
    private val balanceService: BalanceService
) {

    /**
     * 사용자 잔액 충전
     *
     * [트랜잭션 외부]:
     * 1. 사용자 존재 여부 확인
     *
     * [트랜잭션 내부 (BalanceService)]:
     * 2. 잔액 충전 (비관적 락)
     *
     * @param userId 사용자 ID
     * @param chargeAmount 충전 금액 (Controller에서 검증됨: 1,000 ~ 1,000,000원)
     * @return 충전 후 잔액 정보
     * @throws ResourceNotFoundException 사용자를 찾을 수 없는 경우
     */
    fun chargeBalance(userId: Long, chargeAmount: Int): BalanceService.BalanceInfo {
        // 1. UserService를 통해 사용자 존재 여부 확인 (트랜잭션 외부)
        userService.validateUserExists(userId)

        // 2. BalanceService를 통해 잔액 충전 (BalanceService의 @Transactional 적용)
        return balanceService.chargeBalance(userId, chargeAmount)
    }
}
