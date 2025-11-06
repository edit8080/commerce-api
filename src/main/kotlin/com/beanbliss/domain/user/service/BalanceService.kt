package com.beanbliss.domain.user.service

import com.beanbliss.domain.user.dto.BalanceResponse
import com.beanbliss.domain.user.dto.ChargeBalanceResponse

/**
 * [책임]: 사용자 잔액 관리 기능의 계약 정의
 * Controller는 이 인터페이스에만 의존합니다 (DIP 준수)
 */
interface BalanceService {
    /**
     * 사용자 잔액 조회
     *
     * @param userId 사용자 ID
     * @return 사용자 잔액 정보 (레코드 없으면 0원 반환)
     */
    fun getBalance(userId: Long): BalanceResponse

    /**
     * 사용자 잔액 충전 (UPSERT)
     *
     * @param userId 사용자 ID
     * @param chargeAmount 충전 금액 (Controller에서 검증됨: 1,000 ~ 1,000,000원)
     * @return 충전 결과 (충전 후 현재 잔액, 충전 시각)
     * @note 레코드 없으면 INSERT, 있으면 UPDATE
     */
    fun chargeBalance(userId: Long, chargeAmount: Int): ChargeBalanceResponse

    /**
     * 사용자 잔액 차감 (주문 결제)
     *
     * [비즈니스 로직]:
     * 1. 비관적 락으로 잔액 조회 (FOR UPDATE)
     * 2. 잔액 존재 여부 확인
     * 3. 잔액 충분성 검증
     * 4. 잔액 차감 및 저장
     *
     * [트랜잭션]:
     * - @Transactional로 원자성 보장
     * - 비관적 락으로 동시성 제어
     *
     * @param userId 사용자 ID
     * @param amount 차감할 금액
     * @throws BalanceNotFoundException 잔액 정보를 찾을 수 없는 경우
     * @throws InsufficientBalanceException 잔액이 부족한 경우
     */
    fun deductBalance(userId: Long, amount: Int)
}
