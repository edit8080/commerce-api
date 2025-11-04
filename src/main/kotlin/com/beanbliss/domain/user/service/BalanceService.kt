package com.beanbliss.domain.user.service

import com.beanbliss.domain.user.dto.BalanceResponse

/**
 * [책임]: 사용자 잔액 관리 기능의 계약 정의
 * Controller는 이 인터페이스에만 의존합니다 (DIP 준수)
 */
interface BalanceService {
    /**
     * 사용자 잔액 조회
     *
     * @param userId 사용자 ID
     * @return 사용자 잔액 정보
     * @throws BalanceNotFoundException 잔액 정보를 찾을 수 없는 경우
     */
    fun getBalance(userId: Long): BalanceResponse
}
