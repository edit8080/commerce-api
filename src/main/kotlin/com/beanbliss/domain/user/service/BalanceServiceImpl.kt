package com.beanbliss.domain.user.service

import com.beanbliss.domain.user.dto.BalanceResponse
import com.beanbliss.domain.user.dto.ChargeBalanceResponse
import com.beanbliss.domain.user.exception.BalanceNotFoundException
import com.beanbliss.domain.user.repository.BalanceRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * [책임]: 사용자 잔액 비즈니스 로직 처리
 * - 잔액 조회
 * - 잔액 충전
 */
@Service
@Transactional(readOnly = true)
class BalanceServiceImpl(
    private val balanceRepository: BalanceRepository
) : BalanceService {

    override fun getBalance(userId: Long): BalanceResponse {
        // 1. Repository에서 잔액 조회
        val balance = balanceRepository.findByUserId(userId)
            ?: throw BalanceNotFoundException("사용자 ID: $userId 의 잔액 정보를 찾을 수 없습니다.")

        // 2. Entity를 DTO(Response)로 변환
        return BalanceResponse(
            userId = balance.userId,
            amount = balance.amount,
            lastUpdatedAt = balance.updatedAt
        )
    }

    @Transactional
    override fun chargeBalance(userId: Long, chargeAmount: Int): ChargeBalanceResponse {
        // 1. 충전 금액 유효성 검증 (트랜잭션 밖에서 수행하여 빠른 실패)
        validateChargeAmount(chargeAmount)

        // 2. 비관적 락으로 잔액 조회 (FOR UPDATE)
        val balance = balanceRepository.findByUserIdWithLock(userId)
            ?: throw BalanceNotFoundException("사용자 ID: $userId 의 잔액 정보를 찾을 수 없습니다.")

        // 3. 충전 전 잔액 저장
        val previousBalance = balance.amount

        // 4. 새로운 잔액 계산
        val newBalance = previousBalance + chargeAmount

        // 5. 잔액 업데이트
        val now = java.time.LocalDateTime.now()
        val updatedBalance = balance.copy(
            amount = newBalance,
            updatedAt = now
        )
        balanceRepository.save(updatedBalance)

        // 6. ChargeBalanceResponse DTO 생성 및 반환
        return ChargeBalanceResponse(
            userId = userId,
            previousBalance = previousBalance,
            chargeAmount = chargeAmount,
            currentBalance = newBalance,
            chargedAt = now
        )
    }

    /**
     * 충전 금액 유효성 검증
     *
     * @param chargeAmount 충전 금액
     * @throws IllegalArgumentException 충전 금액이 유효하지 않은 경우
     */
    private fun validateChargeAmount(chargeAmount: Int) {
        if (chargeAmount < 1000) {
            throw IllegalArgumentException("충전 금액은 1,000원 이상이어야 합니다.")
        }
        if (chargeAmount > 1000000) {
            throw IllegalArgumentException("1회 최대 충전 금액은 1,000,000원입니다.")
        }
    }
}
