package com.beanbliss.domain.user.service

import com.beanbliss.domain.user.dto.BalanceResponse
import com.beanbliss.domain.user.dto.ChargeBalanceResponse
import com.beanbliss.domain.user.entity.BalanceEntity
import com.beanbliss.domain.user.exception.BalanceNotFoundException
import com.beanbliss.domain.user.exception.InsufficientBalanceException
import com.beanbliss.domain.user.repository.BalanceRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

/**
 * [책임]: 사용자 잔액 비즈니스 로직 처리
 * - 잔액 조회 (레코드 없으면 0원 반환)
 * - 잔액 충전 (UPSERT: 없으면 INSERT, 있으면 UPDATE)
 */
@Service
@Transactional(readOnly = true)
class BalanceServiceImpl(
    private val balanceRepository: BalanceRepository
) : BalanceService {

    override fun getBalance(userId: Long): BalanceResponse {
        // 1. Repository에서 잔액 조회
        val balance = balanceRepository.findByUserId(userId)

        // 2. Entity를 DTO(Response)로 변환
        // 레코드가 없으면 0원 반환 (예외 발생 X)
        return if (balance != null) {
            BalanceResponse(
                userId = balance.userId,
                amount = balance.amount,
                lastUpdatedAt = balance.updatedAt
            )
        } else {
            BalanceResponse(
                userId = userId,
                amount = 0,
                lastUpdatedAt = null
            )
        }
    }

    @Transactional
    override fun chargeBalance(userId: Long, chargeAmount: Int): ChargeBalanceResponse {
        // 1. 충전 금액 유효성 검증
        validateChargeAmount(chargeAmount)

        // 2. 비관적 락으로 잔액 조회 (FOR UPDATE)
        val existingBalance = balanceRepository.findByUserIdWithLock(userId)

        val now = LocalDateTime.now()

        // 3. UPSERT: 레코드 존재 여부에 따라 UPDATE 또는 INSERT
        val savedBalance = if (existingBalance != null) {
            // UPDATE: 기존 잔액에 충전 금액 추가
            val newAmount = existingBalance.amount + chargeAmount
            val updatedBalance = existingBalance.copy(
                amount = newAmount,
                updatedAt = now
            )
            balanceRepository.save(updatedBalance)
        } else {
            // INSERT: 새로운 잔액 레코드 생성 (0 + chargeAmount)
            val newBalance = BalanceEntity(
                id = 0, // Auto-generated
                userId = userId,
                amount = chargeAmount,
                createdAt = now,
                updatedAt = now
            )
            balanceRepository.save(newBalance)
        }

        // 4. ChargeBalanceResponse DTO 생성 및 반환
        return ChargeBalanceResponse(
            userId = userId,
            currentBalance = savedBalance.amount,
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

    @Transactional
    override fun deductBalance(userId: Long, amount: Int) {
        // 1. 비관적 락으로 잔액 조회 (FOR UPDATE)
        val balance = balanceRepository.findByUserIdWithLock(userId)
            ?: throw BalanceNotFoundException("사용자 ID: $userId 의 잔액 정보를 찾을 수 없습니다.")

        // 2. 잔액 충분성 검증
        if (balance.amount < amount) {
            throw InsufficientBalanceException(
                "잔액이 부족합니다. 현재 잔액: ${balance.amount}원, 결제 금액: ${amount}원"
            )
        }

        // 3. 잔액 차감
        val now = LocalDateTime.now()
        val updatedBalance = balance.copy(
            amount = balance.amount - amount,
            updatedAt = now
        )

        // 4. 변경 사항 저장
        balanceRepository.save(updatedBalance)
    }
}
