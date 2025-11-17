package com.beanbliss.domain.coupon.repository

import com.beanbliss.domain.coupon.entity.CouponTicketEntity

/**
 * [책임]: 쿠폰 티켓 영속성 계층의 계약 정의
 * UseCase는 이 인터페이스에만 의존합니다 (DIP 준수)
 */
interface CouponTicketRepository {
    /**
     * 발급 가능한 티켓 조회 및 락 설정 (FOR UPDATE SKIP LOCKED)
     * - status = 'AVAILABLE'인 티켓 중 하나를 선점
     * - 이미 락이 걸린 티켓은 건너뛰고 다음 티켓 자동 선택
     * - 조회된 티켓은 즉시 락 상태가 되어 다른 요청의 조회 불가
     *
     * @param couponId 쿠폰 ID
     * @return 선점된 티켓 (없으면 null)
     */
    fun findAvailableTicketWithLock(couponId: Long): CouponTicketEntity?

    /**
     * 티켓을 사용자에게 발급
     * - status: 'AVAILABLE' → 'ISSUED'
     * - userId, userCouponId, issuedAt 설정
     *
     * @param ticketId 티켓 ID
     * @param userId 사용자 ID
     * @param userCouponId 사용자 쿠폰 ID
     */
    fun issueTicketToUser(ticketId: Long, userId: Long, userCouponId: Long)

    /**
     * 특정 쿠폰의 AVAILABLE 상태 티켓 개수 조회
     *
     * @param couponId 쿠폰 ID
     * @return AVAILABLE 티켓 개수
     */
    fun countAvailableTickets(couponId: Long): Int

    /**
     * 쿠폰 티켓 일괄 저장 (Batch Insert)
     * - 쿠폰 생성 시 totalQuantity만큼 티켓을 미리 생성
     *
     * @param tickets 저장할 티켓 목록
     * @return 저장된 티켓 목록
     */
    fun saveAll(tickets: List<CouponTicketEntity>): List<CouponTicketEntity>
}
