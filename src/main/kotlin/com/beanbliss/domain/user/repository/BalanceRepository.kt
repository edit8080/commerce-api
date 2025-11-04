package com.beanbliss.domain.user.repository

import com.beanbliss.domain.user.entity.BalanceEntity

/**
 * [책임]: 사용자 잔액 영속성 계층의 계약 정의
 * Service는 이 인터페이스에만 의존합니다 (DIP 준수)
 */
interface BalanceRepository {
    /**
     * 사용자 ID로 잔액 조회
     * - USER와 1:1 관계이므로 단일 결과 반환
     *
     * @param userId 사용자 ID
     * @return BalanceEntity (없으면 null)
     */
    fun findByUserId(userId: Long): BalanceEntity?

    /**
     * 사용자 ID로 잔액 조회 (비관적 락)
     * - FOR UPDATE 쿼리 사용
     * - 동시성 제어를 위한 락 획득
     *
     * @param userId 사용자 ID
     * @return BalanceEntity (없으면 null)
     */
    fun findByUserIdWithLock(userId: Long): BalanceEntity?

    /**
     * 잔액 정보 저장/업데이트
     * - userId가 같으면 업데이트, 없으면 생성
     *
     * @param balance 저장할 잔액 정보
     * @return 저장된 BalanceEntity
     */
    fun save(balance: BalanceEntity): BalanceEntity
}
