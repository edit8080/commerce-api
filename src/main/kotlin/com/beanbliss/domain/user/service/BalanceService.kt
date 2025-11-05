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
     * @param chargeAmount 충전 금액 (1,000 ~ 1,000,000원)
     * @return 충전 결과 (충전 후 현재 잔액, 충전 시각)
     * @throws IllegalArgumentException 충전 금액이 유효하지 않은 경우
     * @note 레코드 없으면 INSERT, 있으면 UPDATE
     */
    fun chargeBalance(userId: Long, chargeAmount: Int): ChargeBalanceResponse
}
